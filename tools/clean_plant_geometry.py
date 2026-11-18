"""Trim hidden and coplanar faces in the Blockbench source, preserving UVs and animation groups."""
import json
import re
from copy import deepcopy
from uuid import UUID, uuid5

import sys

from export_plant_model import ROOT, SETTINGS, collect_groups


FACES = {
    "north": (2, False, 0, -1, 1, -1), "south": (2, True, 0, 1, 1, -1),
    "west": (0, False, 2, 1, 1, -1), "east": (0, True, 2, -1, 1, -1),
    "up": (1, True, 0, 1, 2, 1), "down": (1, False, 0, 1, 2, -1),
}


def subtract(rect, mask):
    left, top, right, bottom = rect
    low_x, low_y = max(left, mask[0]), max(top, mask[1])
    high_x, high_y = min(right, mask[2]), min(bottom, mask[3])
    if low_x >= high_x or low_y >= high_y:
        return [rect]
    return [piece for piece in ((left, top, low_x, bottom), (high_x, top, right, bottom),
                                (low_x, top, high_x, low_y), (low_x, high_y, high_x, bottom))
            if piece[0] < piece[2] and piece[1] < piece[3]]


def crop_uv(start, end, low, high, first, last, sign):
    if sign < 0:
        first, last = low + high - last, low + high - first
    return [round(start + (end - start) * (point - low) / (high - low), 6) for point in (first, last)]


def join_patches(model):
    groups = {}
    removed = set()
    for elem in model['elements']:
        match = re.fullmatch(r'(.+)_(north|south|east|west|up|down)_\d+', elem['name'])
        if match and elem['from'][FACES[match[2]][0]] == elem['to'][FACES[match[2]][0]]:
            groups.setdefault(match.groups(), []).append(elem)
    for (_, side), patches in groups.items():
        _, _, horizontal, _, vertical, _ = FACES[side]
        changed = True
        while changed:
            changed = False
            for axis, other in ((horizontal, vertical), (vertical, horizontal)):
                ordered = sorted(patches, key=lambda part: (part['from'][other], part['to'][other], part['from'][axis]))
                kept = []
                for part in ordered:
                    previous = kept[-1] if kept else None
                    if previous and previous['from'][other] == part['from'][other] and previous['to'][other] == part['to'][other] and previous['to'][axis] == part['from'][axis]:
                        previous['to'][axis] = part['to'][axis]
                        start = 0 if axis == horizontal else 1
                        sign = FACES[side][3 if axis == horizontal else 5]
                        end = start + 2 if sign > 0 else start
                        previous['faces'][side]['uv'][end] = part['faces'][side]['uv'][end]
                        removed.add(part['uuid'])
                        changed = True
                    else:
                        kept.append(part)
                patches = kept
    model['elements'] = [elem for elem in model['elements'] if elem['uuid'] not in removed]

    def prune(nodes):
        for node in nodes:
            if isinstance(node, dict):
                node['children'] = prune(node['children'])
        return [node for node in nodes if not isinstance(node, str) or node not in removed]

    model['outliner'] = prune(model['outliner'])


def trim(model):
    elements = model['elements']
    for elem in elements:
        name = elem['name']
        if name == 'water_sight_core' or name.startswith('water_sight_core_'):
            for bound in ('from', 'to'):
                elem[bound][1] = {24: 24.25, 48: 47.75}.get(elem[bound][1], elem[bound][1])
        elif name in {'roller_f_a', 'roller_f_b', 'roller_b_a', 'roller_b_b'}:
            for end in ('west', 'east'):
                elem['faces'][end]['texture'] = None
        elif name == 'drum_core_b' or name.startswith('drum_ring_b_') or name == 'motor_housing_b':
            axis = 0 if name == 'motor_housing_b' else 2
            reference = next(other for other in elements if other['name'] == name.replace('_b', '_a'))
            elem['from'][axis] = reference['from'][axis] + 0.25
            elem['to'][axis] = reference['to'][axis] - 0.25
        elif name.startswith(('diag_brace_2_', 'diag_brace_4_')):
            elem['from'][0] = elem['origin'][0] - 0.95
            elem['to'][0] = elem['origin'][0] + 0.95
    groups, _ = collect_groups(model['outliner'], {group['uuid']: group for group in model['groups']})
    # Spinning groups and repeated sections move against the rest, so their faces are never trimmed against it.
    moving = (set(SETTINGS[model['name']]['spinners']) | {'tail', 'bay', 'stand'}) if model.get('name') in SETTINGS else set()
    frames = [tuple(group for group in groups.get(elem['uuid'], []) if group in moving) for elem in elements]
    glass = {int(tex['id']) for tex in model['textures'] if tex['name'].removesuffix('.png') == 'gt_glass_frame'}
    opaque = [not any(face.get('texture') in glass for face in elem['faces'].values()) for elem in elements]
    replacements = {}
    result = []
    for index, elem in enumerate(elements):
        fresh = deepcopy(elem)
        extras = []
        low, high = elem['from'], elem['to']
        if elem.get('rotation') or any(start == end for start, end in zip(low, high)):
            result.append(fresh)
            continue
        for name, face in elem['faces'].items():
            if face.get('texture') is None:
                continue
            axis, positive, horizontal, u_sign, vertical, v_sign = FACES[name]
            plane = (high if positive else low)[axis]
            rect = (low[horizontal], low[vertical], high[horizontal], high[vertical])
            pieces = [rect]
            for other_index, other in enumerate(elements):
                if index == other_index or frames[index] != frames[other_index] or other.get('rotation') or not opaque[other_index]:
                    continue
                lower, upper = other['from'], other['to']
                if any(first == last for first, last in zip(lower, upper)):
                    continue
                buried = lower[axis] <= plane < upper[axis] if positive else lower[axis] < plane <= upper[axis]
                tied = (upper if positive else lower)[axis] == plane
                if not buried and not (tied and other_index > index):
                    continue
                mask = (lower[horizontal], lower[vertical], upper[horizontal], upper[vertical])
                pieces = [part for piece in pieces for part in subtract(piece, mask)]
                if not pieces:
                    break
            if pieces == [rect]:
                continue
            fresh['faces'][name]['texture'] = None
            for number, (left, top, right, bottom) in enumerate(pieces):
                patch = deepcopy(elem)
                patch['name'] += f'_{name}_{number}'
                patch['uuid'] = str(uuid5(UUID(elem['uuid']), f'{name}/{number}'))
                patch['from'][axis] = patch['to'][axis] = plane
                patch['from'][horizontal], patch['to'][horizontal] = left, right
                patch['from'][vertical], patch['to'][vertical] = top, bottom
                for side in patch['faces'].values():
                    side['texture'] = None
                u0, v0, u1, v1 = face['uv']
                u0, u1 = crop_uv(u0, u1, rect[0], rect[2], left, right, u_sign)
                v0, v1 = crop_uv(v0, v1, rect[1], rect[3], top, bottom, v_sign)
                patch['faces'][name] = dict(face, uv=[u0, v0, u1, v1])
                extras.append(patch)
        retained = [fresh] if any(side.get('texture') is not None for side in fresh['faces'].values()) else []
        result.extend(retained + extras)
        replacements[elem['uuid']] = [part['uuid'] for part in retained + extras]

    def remap(nodes):
        output = []
        for node in nodes:
            if isinstance(node, str):
                output.extend(replacements.get(node, [node]))
            else:
                node['children'] = remap(node.get('children', []))
                output.append(node)
        return output

    model['outliner'] = remap(model['outliner'])
    model['elements'] = result


if __name__ == '__main__':
    BBMODEL = ROOT / f"tools/models/{sys.argv[1] if len(sys.argv) > 1 else 'ore_processing_plant'}.bbmodel"
    model = json.loads(BBMODEL.read_text(encoding='utf-8'))
    trim(model)
    join_patches(model)
    BBMODEL.write_text(json.dumps(model, ensure_ascii=False, separators=(',', ':')) + '\n', encoding='utf-8')

# Repository workflow

- Work on an existing local branch; if HEAD is detached or no local branch exists, create a local working branch before committing.
- Use repository-local Git identity `lhy512103 <lhy512103@gmail.com>`.
- After completing and verifying each feature, update `CHANGELOG.md` and make a focused local commit; do not push unless explicitly requested.
- Keep commit subjects short, using `新增：…`, `修复：…`, `移除：…`, or `优化：…`.
- Under each version heading in `CHANGELOG.md`, put the complete `### English` numbered list before the complete `### 中文` numbered list.
- Each changelog item is one short sentence about a player-visible change; omit developer-only changes such as dependency coordinates and build plumbing.
- Do not include unrelated pre-existing changes in commits.
- Do not use subagents unless the task is particularly difficult.

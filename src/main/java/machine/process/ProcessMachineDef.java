package machine.process;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import energy.EUVoltage;

import java.util.List;

/**
 * One data-driven process machine, read from {@code iuce/machines/<id>.json}. Every process
 * machine shares the same block entity, menu and screen; the definition only decides the slots,
 * tanks, power and look.
 * <p>
 * Item slot order: inputs, outputs, catalyst (optional), card (optional), upgrades.
 * Tank order: inputs, then outputs.
 * </p>
 *
 * @param id           registry name of the block
 * @param voltage      highest voltage the machine accepts
 * @param buffer       internal EU buffer
 * @param itemsIn      input slots
 * @param itemsOut     output slots
 * @param fluidsIn     capacity (mB) of each input tank
 * @param fluidsOut    capacity (mB) of each output tank
 * @param catalystSlot whether it has a catalyst / mould slot
 * @param cardSlot     whether it has a process card slot
 * @param upgrades     upgrade slots
 * @param baseTime     ticks shown before any recipe is chosen
 * @param model        block model shape, see {@code tools/gen_content.py}
 * @param progress     how the screen shows progress, see {@link ProgressStyle}
 */
public record ProcessMachineDef(String id, EUVoltage voltage, long buffer, int itemsIn, int itemsOut,
                                List<Integer> fluidsIn, List<Integer> fluidsOut,
                                boolean catalystSlot, boolean cardSlot, int upgrades, int baseTime, String model,
                                ProgressStyle progress) {

    private static final Codec<EUVoltage> VOLTAGE = Codec.STRING.xmap(EUVoltage::valueOf, EUVoltage::name);

    public static final Codec<ProcessMachineDef> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("id").forGetter(ProcessMachineDef::id),
        VOLTAGE.optionalFieldOf("voltage", EUVoltage.LV).forGetter(ProcessMachineDef::voltage),
        Codec.LONG.optionalFieldOf("buffer", 2000L).forGetter(ProcessMachineDef::buffer),
        Codec.intRange(0, 9).optionalFieldOf("items_in", 1).forGetter(ProcessMachineDef::itemsIn),
        Codec.intRange(0, 9).optionalFieldOf("items_out", 1).forGetter(ProcessMachineDef::itemsOut),
        Codec.intRange(1, 1_000_000).listOf().optionalFieldOf("fluids_in", List.of()).forGetter(ProcessMachineDef::fluidsIn),
        Codec.intRange(1, 1_000_000).listOf().optionalFieldOf("fluids_out", List.of()).forGetter(ProcessMachineDef::fluidsOut),
        Codec.BOOL.optionalFieldOf("catalyst_slot", false).forGetter(ProcessMachineDef::catalystSlot),
        Codec.BOOL.optionalFieldOf("card_slot", false).forGetter(ProcessMachineDef::cardSlot),
        Codec.intRange(0, 4).optionalFieldOf("upgrades", 4).forGetter(ProcessMachineDef::upgrades),
        Codec.intRange(1, 100_000).optionalFieldOf("base_time", 200).forGetter(ProcessMachineDef::baseTime),
        Codec.STRING.optionalFieldOf("model", "box").forGetter(ProcessMachineDef::model),
        ProgressStyle.CODEC.optionalFieldOf("progress", ProgressStyle.ARROW).forGetter(ProcessMachineDef::progress)
    ).apply(i, ProcessMachineDef::new));

    public int outputStart() { return itemsIn; }
    public int catalystIndex() { return catalystSlot ? itemsIn + itemsOut : -1; }
    public int cardIndex() { return cardSlot ? itemsIn + itemsOut + (catalystSlot ? 1 : 0) : -1; }
    public int upgradeStart() { return itemsIn + itemsOut + (catalystSlot ? 1 : 0) + (cardSlot ? 1 : 0); }
    public int slotCount() { return upgradeStart() + upgrades; }
    public int tankCount() { return fluidsIn.size() + fluidsOut.size(); }
}

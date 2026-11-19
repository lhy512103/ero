package machine.process;

import block.MachineBlock;

/** A process machine block; which machine it is comes from its {@link ProcessMachineDef}. */
public class ProcessMachineBlock extends MachineBlock {
    private final ProcessMachineDef def;

    public ProcessMachineBlock(Properties properties, ProcessMachineDef def) {
        super(properties, ProcessMachineBlockEntity::new, ProcessMachines.BLOCK_ENTITY);
        this.def = def;
    }

    public ProcessMachineDef def() { return def; }
}

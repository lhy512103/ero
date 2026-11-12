package item;

/** Ultimate overclocker: x0.45 processing time, x2.2 energy per tick for each one installed. */
public class OverclockerUpgrade2Item extends OverclockerUpgradeItem {

    public OverclockerUpgrade2Item(Properties properties) {
        super(properties);
    }

    @Override public double timeFactor() { return 0.45; }

    @Override public double energyFactor() { return 2.2; }
}

package item;

/** Advanced overclocker: x0.60 processing time, x1.8 energy per tick for each one installed. */
public class OverclockerUpgrade1Item extends OverclockerUpgradeItem {

    public OverclockerUpgrade1Item(Properties properties) {
        super(properties);
    }

    @Override public double timeFactor() { return 0.60; }

    @Override public double energyFactor() { return 1.8; }
}

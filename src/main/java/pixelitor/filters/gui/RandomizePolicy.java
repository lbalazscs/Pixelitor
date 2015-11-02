package pixelitor.filters.gui;

public enum RandomizePolicy {
    IGNORE_RANDOMIZE {
        @Override
        boolean allowRandomize() {
            return false;
        }
    }, ALLOW_RANDOMIZE {
        @Override
        boolean allowRandomize() {
            return true;
        }
    };

    abstract boolean allowRandomize();
}

package pixelitor.selection;

/**
 * Determines whether some action should ignore the selection
 */
public enum IgnoreSelection {
    YES(true), NO(false);

    private final boolean yes;

    IgnoreSelection(boolean yes) {
        this.yes = yes;
    }

    public boolean isYes() {
        return yes;
    }
}

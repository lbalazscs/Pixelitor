package pixelitor.tools.guidelines;

/**
 * Crop guidelines types
 */
public enum RectGuidelineType {
    NONE("None"),
    RULE_OF_THIRDS("Rule of thirds"),
    GOLDEN_SECTIONS("Golden sections"),
    DIAGONALS("Diagonal lines");

    private final String guiName;

    RectGuidelineType(String guiName) {
        this.guiName = guiName;
    }

    @Override
    public String toString() {
        return guiName;
    }
}

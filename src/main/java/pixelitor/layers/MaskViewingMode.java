package pixelitor.layers;

// TODO every composition or every layer should have one?
public enum MaskViewingMode {
    CTRL_1(""),
    CTRL_2(""),
    CTRL_3("");

    private String guiName;

    MaskViewingMode(String guiName) {
        this.guiName = guiName;
    }

    @Override
    public String toString() {
        return guiName;
    }
}

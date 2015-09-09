package pixelitor;

import pixelitor.tools.FgBgColorSelector;

import java.awt.Color;

public class FgBgColors {
    private static FgBgColorSelector gui;

    private FgBgColors() {
    }

    public static void setGUI(FgBgColorSelector gui) {
        FgBgColors.gui = gui;
    }

    public static FgBgColorSelector getGUI() {
        return gui;
    }

    public static Color getFG() {
        return gui.getFgColor();
    }

    public static Color getBG() {
        return gui.getBgColor();
    }

    public static void setFG(Color c) {
        gui.setFgColor(c);
    }

    public static void setBG(Color c) {
        gui.setBgColor(c);
    }

    public static void setRandomColors() {
        gui.randomizeColorsAction.actionPerformed(null);
    }

    public static void setLayerMaskEditing(boolean b) {
        gui.setLayerMaskEditing(b);
    }
}

package pixelitor.colors;

import com.bric.swing.ColorPicker;
import com.bric.swing.ColorSwatch;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.GUIUtils;

import java.awt.*;

/**
 * Color picker dialog helper for esy use with ColorSwatch
 * When user select new color provided action is performed
 */
public class ColorPickerDialog {
    private final ColorSwatch colorSwatch;
    private final ColorPickerDialogAction action;

    public ColorPickerDialog(ColorSwatch colorSwatch, ColorPickerDialogAction action) {
        this.colorSwatch = colorSwatch;
        this.action = action;
        GUIUtils.addColorDialogListener(colorSwatch, this::showColorDialog);
    }

    private void showColorDialog() {
        Color color = ColorPicker.showDialog(
            PixelitorWindow.getInstance(),
            "Select Color",
            colorSwatch.getForeground(),
            false
        );

        // ok was pressed
        if (color != null) {
            colorSwatch.setForeground(color);
            this.action.colorChanged(color);
        }
    }
}

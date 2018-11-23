package pixelitor.colors;

import java.awt.Color;

/**
 * The action taken after color change
 */
public interface ColorPickerDialogAction {
    void colorChanged(Color color);
}

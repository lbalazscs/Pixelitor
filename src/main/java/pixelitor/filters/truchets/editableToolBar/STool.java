package pixelitor.filters.truchets.editableToolBar;

import pixelitor.gui.utils.VectorIcon;

import java.awt.Color;
import java.awt.Graphics2D;

public interface STool {
    void takeAction(int x, int y);

    void paintIcon(Graphics2D g, int size);

    default VectorIcon getIcon(Color iconColor, int iconSize) {
        return new VectorIcon(iconColor, iconSize, iconSize) {
            @Override
            protected void paintIcon(Graphics2D g) {
                STool.this.paintIcon(g, iconSize);
            }
        };
    }

    String getName();
}

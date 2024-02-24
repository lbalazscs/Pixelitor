package pixelitor.filters.truchets;

import javax.swing.*;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class TruchetTileDisplay extends JPanel {
    private TruchetSwatch swatch;

    public TruchetTileDisplay(TruchetSwatch swatch) {
        this.swatch = swatch;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int W = getWidth(), H = getHeight();
        if (W * H == 0) {
            return;
        }
        int columns = swatch.getWidth(1), rows = swatch.getHeight(1);
        Graphics2D g2 = (Graphics2D) g;
        boolean widthFirst = W * 1d / H > columns * 1d / rows;
        int tileSize = widthFirst ? H / rows : W / columns;
        g.translate(widthFirst ? (W - columns * tileSize) / 2 : 0, widthFirst ? 0 : (H - rows * tileSize) / 2);
        swatch.draw(g2, tileSize, 1);
        g.translate(widthFirst ? -(W - columns * tileSize) / 2 : 0, widthFirst ? 0 : -(H - rows * tileSize) / 2);
    }
}
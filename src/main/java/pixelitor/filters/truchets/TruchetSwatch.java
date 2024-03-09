package pixelitor.filters.truchets;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

public class TruchetSwatch {
    List<TileState> tiles = new ArrayList<>();
    int rows, columns;

    public int getHeight(int tileSize) {
        return rows * tileSize;
    }

    public int getWidth(int tileSize) {
        return columns * tileSize;
    }

    public void draw(Graphics2D g, int tileSize, int lineWidth) {
        int x = 0;
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < this.columns; j++) {
                g.translate(j * tileSize, i * tileSize);
                tiles.get(x++).draw(g, tileSize, lineWidth);
                g.translate(-j * tileSize, -i * tileSize);
            }
        }
    }

    public void adapt(TruchetPalette palette, TruchetPattern pattern) {
        rows = pattern.getRows();
        columns = pattern.getColumns();
        int size = rows * columns;
        for (int i = tiles.size(); i < size; i++) {
            tiles.add(palette.getTileState(0, null));
        }
        while (tiles.size() > size) {
            tiles.removeLast();
        }
        int x = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                TileState tileState = tiles.get(x++);
                palette.getTileState(pattern.getState(i, j), tileState);
                pattern.sharePatternTweaks(i, j, tileState);
            }
        }
    }
}
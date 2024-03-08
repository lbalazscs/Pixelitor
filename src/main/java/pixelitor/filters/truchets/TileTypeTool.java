package pixelitor.filters.truchets;

import pixelitor.filters.truchets.editableToolBar.STool;

import java.awt.Graphics2D;

public class TileTypeTool implements STool {

    private TileType tileType;
    private TruchetPalette palette;
    private TruchetConfigurablePattern pattern;

    public TileTypeTool(TileType tileType, TruchetPalette palette, TruchetConfigurablePattern pattern) {
        this.tileType = tileType;
        this.palette = palette;
        this.pattern = pattern;
    }

    @Override
    public void takeAction(int x, int y) {
        int start = palette.getFirstStateOf(tileType);
        int current = pattern.getState(y, x);
        System.out.println(current + " " + start);
        pattern.setState(y, x,
            current < start || current >= start + tileType.rotationalDegree ? start :
                (current + 1 - start) % tileType.rotationalDegree + start);
    }

    @Override
    public void paintIcon(Graphics2D g, int size) {
        tileType.draw(g, size, 2);
    }

    @Override
    public String getName() {
        return tileType.toString();
    }
}

package pixelitor.filters.truchets;

import pixelitor.utils.Utils;

import java.awt.Graphics2D;

public class TileState {
    public TileType type;
    public int rotation;
    public boolean flipAboutHorizontal;
    public boolean flipAboutVertical;

    public TileState(TileType type) {
        this(type, 0, false, false);
    }

    public TileState(TileType type, int rotation, boolean flipAboutHorizontal, boolean flipAboutVertical) {
        this.type = type;
        this.rotation = rotation;
        this.flipAboutHorizontal = flipAboutHorizontal;
        this.flipAboutVertical = flipAboutVertical;
    }

    public void draw(Graphics2D g, int tileSize, int lineWidth) {
        type.draw(g, tileSize, lineWidth, rotation, flipAboutHorizontal, flipAboutVertical);
    }

    public void copyFrom(TileState tileState) {
        this.type = tileState.type;
        this.rotation = tileState.rotation;
        this.flipAboutHorizontal = tileState.flipAboutHorizontal;
        this.flipAboutVertical = tileState.flipAboutVertical;
    }
}

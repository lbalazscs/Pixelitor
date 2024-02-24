package pixelitor.filters.truchets;

import pixelitor.utils.Utils;

import java.util.ArrayList;

public enum TruchetPalette {
    TRIANGLES(TileType.TRIANGE, TileType.SQUARE, TileType.BLANK),
    QUARTER_CIRCLES(TileType.QUARTER_CIRCLE, TileType.PLUS, TileType.CIRCLE_CROSS),
    DIAGONALS(TileType.DIAGONAL, TileType.LINE, TileType.LINE_DOWN),
    ;

    public final TileType[] tiles;
    private final TileState[] tileStates;

    TruchetPalette(TileType... tiles) {
        this.tiles = tiles;
        var list = new ArrayList<TileState>();
        for (TileType tile : tiles) {
            for (int i = 0; i < tile.rotationalDegree; i++) {
                list.add(new TileState(tile, i, false, false));
            }
//            list.add(new TileState(tile, 0, false, false));
//            if (tile.isSymmetricAboutHorizontal) {
//                if (!tile.isSymmetricAboutVertical) {
//                    list.add(new TileState(tile, 0, false, true));
//                }
//            } else {
//                if (tile.isSymmetricAboutVertical) {
//                    list.add(new TileState(tile, 0, true, false));
//                } else {
//                    list.add(new TileState(tile, 0, false, true));
//                    list.add(new TileState(tile, 0, true, true));
//                    list.add(new TileState(tile, 0, true, false));
//                }
//            }
        }
        this.tileStates = list.toArray(TileState[]::new);
        System.out.println(this.tileStates.length);
    }

    @Override
    public String toString() {
        return Utils.screamingSnakeCaseToSentenceCase(super.toString());
    }

    public TileState getTileState(int state, TileState tileState) {
        if (tileState == null) {
            tileState = new TileState(tiles[0]);
        }
        tileState.copyFrom(tileStates[(state) % tileStates.length]);
        return tileState;
    }
}

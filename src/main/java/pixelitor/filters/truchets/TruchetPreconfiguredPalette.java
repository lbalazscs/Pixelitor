package pixelitor.filters.truchets;

import pixelitor.utils.Utils;

import java.util.ArrayList;

public enum TruchetPreconfiguredPalette implements TruchetPalette {
    TRIANGLES(TileType.TRIANGLE, TileType.SQUARE, TileType.BLANK),
    CIRCULAR(TileType.QUARTER_CIRCLE, TileType.CIRCLE_CROSS),
    DIAGONALS(TileType.DIAGONAL, TileType.PARALLEL),
    CIRCUIT(TileType.PLUS, TileType.CENTER_WELL, TileType.CORNER),
    STREETS(TileType.PLUS, TileType.FLAT_JOIN, TileType.DIVIDE, TileType.THREE_WAY),
    BOIDS(TileType.FILLED_QUARTERS, TileType.CORNER_BOID),
    ;
    private final TileState[] tileStates;

    TruchetPreconfiguredPalette(TileType... tiles) {
        var list = new ArrayList<TileState>();
        for (TileType tile : tiles) {
            for (int i = 0; i < tile.rotationalDegree; i++) {
                list.add(new TileState(tile, i, false, false));
            }
        }
        this.tileStates = list.toArray(TileState[]::new);
    }

    @Override
    public String toString() {
        return Utils.screamingSnakeCaseToSentenceCase(super.toString());
    }

    public TileState getTileState(int state, TileState tileState) {
        if (tileState == null) {
            tileState = new TileState(tileStates[0].type);
        }
        tileState.copyFrom(tileStates[(state) % tileStates.length]);
        return tileState;
    }

    @Override
    public int getDegree() {
        return tileStates.length;
    }
}

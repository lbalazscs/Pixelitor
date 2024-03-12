package pixelitor.filters.truchets;

import pixelitor.utils.Utils;

import java.util.ArrayList;

public enum TruchetPreconfiguredPalette implements TruchetPalette {
    TRIANGLES(TileType.TRIANGLE, TileType.BLANK, TileType.SQUARE),
    CIRCULAR(TileType.QUARTER_CIRCLE, TileType.QUARTER_CIRCLE, TileType.PLUS, TileType.CIRCLE_CROSS),
    DIAGONALS(TileType.DIAGONAL, TileType.DIAGONAL, TileType.PARALLEL),
    CELLS(TileType.QUARTER_CIRCLE, TileType.CIRCLE_CROSS),
    KNOTS(TileType.FLAT_JOIN, TileType.PLUS),
    SPACE_BEND(TileType.PLUS, TileType.CIRCLE_CROSS),
    WALLS(TileType.FLAT_JOIN),
    WEAVE(TileType.SQUARE, TileType.PARALLEL),
    ROUND_EDGE(TileType.QUARTER_CIRCLE, TileType.THREE_WAY),
    BOIDS(TileType.FILLED_QUARTERS, TileType.CORNER_BOID),


    // Very High Degree Filters (>6) (Too high for TruchetPreconfiguredPattern)
//    CIRCUIT(TileType.PLUS, TileType.WELL, TileType.CORNER, TileType.JUMP),
//    STREETS(TileType.PLUS, TileType.FLAT_JOIN, TileType.DIVIDE, TileType.THREE_WAY),
    ;
    private final TileType[] tileTypes;
    private final TileState[] tileStates;

    TruchetPreconfiguredPalette(TileType... tiles) {
        this.tileTypes = tiles;
        var list = new ArrayList<TileState>();
        for (TileType tile : tiles) {
            for (int i = 0; i < tile.rotationalDegree; i++) {
                list.add(new TileState(tile, i, false, false));
            }
        }
        this.tileStates = list.toArray(TileState[]::new);
        System.out.println(this + " degree\t : " + getDegree());
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

    @Override
    public int getFirstStateOf(TileType tileType) {
        int state = 0;
        for (TileState tileState : tileStates) {
            if (tileState.type == tileType) {
                return state;
            }
            state += tileState.type.rotationalDegree;
        }
        throw new IllegalStateException("Tile type " + tileType + " not in palette!");
    }

    @Override
    public void updateFrom(TruchetPalette source) {
        throw new UnsupportedOperationException("Enums are immutable!");
    }

    public TileType[] getTileTypes() {
        return tileTypes;
    }
}

package pixelitor.filters.truchets;

import pixelitor.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;

public class TruchetConfigurablePalette implements TruchetPalette {

    final ArrayList<TileState> tileStates = new ArrayList<>();
    final ArrayList<TileType> tileTypes = new ArrayList<>();

    public void updateStates(Iterable<TileType> tiles) {
        int x = 0;
        tileTypes.clear();
        for (TileType tile : tiles) {
            tileTypes.add(tile);
            for (int i = 0; i < tile.rotationalDegree; i++) {
                if (x < tileStates.size()) {
                    TileState tileState = tileStates.get(x);
                    tileState.type = tile;
                    tileState.rotation = i;
                    x++;
                } else {
                    tileStates.add(new TileState(tile, i, false, false));
                    x++;
                }
            }
        }
        while (x < tileStates.size()) {
            tileStates.removeLast();
        }
    }

    @Override
    public String toString() {
        return Utils.screamingSnakeCaseToSentenceCase(super.toString());
    }

    public TileState getTileState(int state, TileState tileState) {
        if (tileState == null) {
            tileState = new TileState(tileStates.get(0).type);
        }
        tileState.copyFrom(tileStates.get(state % tileStates.size()));
        return tileState;
    }

    @Override
    public int getDegree() {
        return tileStates.size();
    }

    @Override
    public int getFirstStateOf(TileType tileType) {
        int state = 0;
        for (TileState tileState : tileStates) {
            if (tileState.type == tileType) {
                return state;
            }
            state++;
        }
        throw new IllegalStateException("Tile type " + tileType + " not in palette!");
    }

    @Override
    public void updateFrom(TruchetPalette source) {
        if (source instanceof TruchetPreconfiguredPalette palette) {
            updateStates(Arrays.asList(palette.getTileTypes()));
        } else if (source instanceof TruchetConfigurablePalette palette) {
            tileTypes.clear();
            tileTypes.addAll(palette.tileTypes);
            int x = Math.min(this.tileStates.size(), palette.tileStates.size());
            for (int i = 0; i < x; i++) {
                this.tileStates.get(i).copyFrom(palette.tileStates.get(i));
            }
            if (this.tileStates.size() > palette.tileStates.size()) {
                while (this.tileStates.size() > x) {
                    this.tileStates.removeLast();
                }
            }
            if (this.tileStates.size() < palette.tileStates.size()) {
                for (int i = x; i < palette.tileStates.size(); i++) {
                    TileState state = new TileState(null);
                    state.copyFrom(palette.tileStates.get(i));
                    this.tileStates.add(state);
                }
            }
        } else {
            throw new UnsupportedOperationException("Not implemented!");
        }
    }
}
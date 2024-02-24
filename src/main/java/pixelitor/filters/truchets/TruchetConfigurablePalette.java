package pixelitor.filters.truchets;

import pixelitor.utils.Utils;

import java.util.ArrayList;

public class TruchetConfigurablePalette implements TruchetPalette {

    private final ArrayList<TileState> tileStates = new ArrayList<>();

    public void updateStates(Iterable<TileType> tiles) {
        int x = 0;
        for (TileType tile : tiles) {
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
}

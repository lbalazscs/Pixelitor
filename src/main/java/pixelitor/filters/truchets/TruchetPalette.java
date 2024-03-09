package pixelitor.filters.truchets;

public interface TruchetPalette {
    TileState getTileState(int state, TileState tileState);

    int getDegree();

    int getFirstStateOf(TileType tileType);

    void updateFrom(TruchetPalette source);
}

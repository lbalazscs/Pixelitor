package pixelitor.filters.truchets;

public interface TruchetPattern {
    int getRows();

    int getColumns();

    int getState(int row, int column);

    default void sharePatternTweaks(int row, int column, TileState tileState){}
}

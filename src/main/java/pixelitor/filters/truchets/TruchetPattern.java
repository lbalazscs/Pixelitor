package pixelitor.filters.truchets;

import java.awt.Point;
import java.util.stream.Stream;

public interface TruchetPattern {
    int getRows();

    int getColumns();

    int getState(int row, int column);

    default void sharePatternTweaks(int row, int column, TileState tileState){}

    Stream<Point> streamHighlightRule(int mouseX, int mouseY);
}

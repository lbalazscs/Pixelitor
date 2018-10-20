package pixelitor.tools;

import pixelitor.gui.View;
import pixelitor.tools.util.DraggablePoint;

import java.awt.Graphics2D;

/**
 * Something that can be drawn over the image as part of a tool's
 * functionality and can respond to mouse and keyboard events.
 */
public interface ToolWidget {
    /**
     * If a {@link DraggablePoint}'s handle was "hit" by a
     * mouse event, return the point, otherwise return null;
     */
    DraggablePoint handleWasHit(double x, double y);

    /**
     * Paint the widget on the given Graphics2D,
     * which is expected to be in component space
     */
    void paint(Graphics2D g);

    /**
     * The component-space coordinates of this widget
     * have to be recalculated based on the image-space coordinates
     * because the active view changed (zoom, canvas resize etc.)
     */
    void coCoordsChanged(View view);
}

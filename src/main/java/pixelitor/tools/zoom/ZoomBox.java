package pixelitor.tools.zoom;

import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.tools.ToolWidget;
import pixelitor.tools.crop.CropHandle;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.PRectangle;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public class ZoomBox implements ToolWidget {

    private static final int MODE_NONE = 0;
    private final PRectangle rect;

    // type of user transform
    private int transformMode = MODE_NONE;

    // the width/height ratio of the selected area
    private double aspectRatio = 0;

    public ZoomBox(PRectangle rect, View view) {
        this.rect = rect;
    }

    @Override
    public void paint(Graphics2D g) {
    }

    @Override
    public CropHandle handleWasHit(double x, double y) {
        return null;
    }

    public PRectangle getRect() {
        return rect;
    }

    @Override
    public void coCoordsChanged(View view) {
        rect.coCoordsChanged(view);
    }

    @Override
    public void imCoordsChanged(AffineTransform at, Composition comp) {
        rect.imCoordsChanged(comp.getView(), at);
    }

    /**
     * Returns true while the user is adjusting the handles.
     */
    public boolean isAdjusting() {
        return transformMode != MODE_NONE;
    }

    @Override
    public void arrowKeyPressed(ArrowKey key, View view) {
    }

    @Override
    public String toString() {
        return "ZoomBox{ " +
                ", rect=" + rect +
                ", adjusting=" + isAdjusting() +
                ", transformMode=" + transformMode +
                ", aspectRatio=" + aspectRatio +
                '}';
    }
}


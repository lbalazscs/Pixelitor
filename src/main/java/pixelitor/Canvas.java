/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor;

import pixelitor.gui.View;
import pixelitor.tools.Symmetry;
import pixelitor.utils.ImageUtils;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.io.Serializable;

/**
 * The canvas represents the size of the composition.
 * A layer can be bigger than the canvas if it is partially hidden.
 */
public class Canvas implements Serializable {
    public static final int MAX_WIDTH = 9_999;
    public static final int MAX_HEIGHT = 9_999;

    // some non-transient field names are inconsistent
    // with their getters, but they can't be renamed without breaking
    // serialization compatibility with old pxc files

    // size in image space
    private int width;
    private int height;

    // size in component space
    private int zoomedWidth;
    private int zoomedHeight;

    @Serial
    private static final long serialVersionUID = -1459254568616232274L;

    public Canvas(int imWidth, int imHeight) {
        width = imWidth;
        height = imHeight;
    }

    public Canvas(Canvas orig) {
        width = orig.width;
        height = orig.height;
        zoomedWidth = orig.zoomedWidth;
        zoomedHeight = orig.zoomedHeight;
    }

    /**
     * Changes the size with values given in image space
     */
    public void changeSize(int newWidth, int newHeight, View view, boolean notify) {
        width = newWidth;
        height = newHeight;

        // also update the component space values
        recalcCoSize(view, notify);

        activeCanvasSizeChanged(this);
    }

    /**
     * Recalculates the component-space (zoomed) size
     */
    public void recalcCoSize(View view, boolean notify) {
        double viewScale = view.getScaling();

        int oldZoomedWidth = zoomedWidth;
        int oldZoomedHeight = zoomedHeight;

        zoomedWidth = (int) (viewScale * width);
        zoomedHeight = (int) (viewScale * height);

        if (notify && (zoomedWidth != oldZoomedWidth || zoomedHeight != oldZoomedHeight)) {
            view.canvasCoSizeChanged();
        }
    }

    /**
     * Either a new composition (therefore a new canvas)
     * was activated or the active canvas size changed
     */
    public static void activeCanvasSizeChanged(Canvas canvas) {
        // as long as only Symmetry must be notified,
        // a listener mechanism is not necessary
        Symmetry.activeCanvasSizeChanged(canvas);
    }

    /**
     * Returns the bounds in image space
     */
    public Rectangle getBounds() {
        return new Rectangle(0, 0, width, height);
    }

    /**
     * Returns the (zoomed) bounds in component space
     */
    public Rectangle2D getCoBounds(View view) {
        return new Rectangle2D.Double(
            view.getCanvasStartX(), view.getCanvasStartY(), zoomedWidth, zoomedHeight);
    }

    /**
     * Returns the size in image space
     */
    public Dimension getSize() {
        return new Dimension(width, height);
    }

    /**
     * Returns the (zoomed) size in component space
     */
    public Dimension getCoSize() {
        return new Dimension(zoomedWidth, zoomedHeight);
    }

    /**
     * Returns the width in image space
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height in image space
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the (zoomed) width in component space
     */
    public int getCoWidth() {
        return zoomedWidth;
    }

    /**
     * Returns the (zoomed) height in component space
     */
    public int getCoHeight() {
        return zoomedHeight;
    }

    public Shape invertShape(Shape shape) {
        Area area = new Area(shape);
        Area fullArea = new Area(getBounds());
        fullArea.subtract(area);
        return fullArea;
    }

    public Shape clip(Shape shape) {
        assert shape != null;

        Rectangle2D canvasBounds = getBounds();

        if (shape instanceof Rectangle2D) {
            // don't ruin the type information in the shape by
            // creating an unnecessary Area, because this is useful
            // info in many ways (crop selection, AA selection clipping)
            return ((Rectangle2D) shape).createIntersection(canvasBounds);
        }

        Area canvasArea = new Area(canvasBounds);
        Area shapeArea = new Area(shape);
        shapeArea.intersect(canvasArea);

        return shapeArea;
    }

    /**
     * Create a temporary image with the size of this canvas
     */
    public BufferedImage createTmpImage() {
        // it is important that the tmp image has transparency
        // even for layer masks, otherwise drawing is not possible
        return ImageUtils.createSysCompatibleImage(this);
    }

    public boolean isFullyCoveredBy(BufferedImage img) {
        return img.getWidth() >= width && img.getHeight() >= height;
    }

    public boolean hasDifferentSizeThan(BufferedImage img) {
        return width != img.getWidth() || height != img.getHeight();
    }

    @Override
    public String toString() {
        return "Canvas{width=" + width + ", height=" + height + '}';
    }
}

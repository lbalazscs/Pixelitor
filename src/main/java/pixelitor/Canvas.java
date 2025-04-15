/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.tools.util.PPoint;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Rnd;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.Debuggable;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.io.Serializable;

/**
 * The canvas represents the size of a composition.
 * A layer can be larger than the canvas if it's partially hidden,
 * for example because it was moved with the Move Tool).
 * The saved images always have the size of the canvas.
 */
public class Canvas implements Serializable, Debuggable {
    public static final int MAX_WIDTH = 9_999;
    public static final int MAX_HEIGHT = 9_999;

    // size in image space
    private int width;
    private int height;

    // size in component space (the size in image space * zooming)
    private transient int coWidth;
    private transient int coHeight;

    // cached thumbnail dimensions (calculated lazily)
    private transient Dimension thumbDimension;

    @Serial
    private static final long serialVersionUID = -1459254568616232274L;

    public Canvas(int imWidth, int imHeight) {
        validateNewSize(imWidth, imHeight);
        width = imWidth;
        height = imHeight;
    }

    private Canvas(Canvas orig) {
        width = orig.width;
        height = orig.height;
        coWidth = orig.coWidth;
        coHeight = orig.coHeight;
    }

    public Canvas copy() {
        return new Canvas(this);
    }

    private static void validateNewSize(int newWidth, int newHeight) {
        if (newWidth <= 0 || newWidth > MAX_WIDTH || newHeight <= 0 || newHeight > MAX_HEIGHT) {
            throw new IllegalArgumentException(String.format(
                "Invalid size: %dx%d. Must be between 1x1 and %dx%d.",
                newWidth, newWidth, MAX_WIDTH, MAX_HEIGHT));
        }
    }

    /**
     * Resizes the canvas using values given in image space.
     */
    public void resize(int newWidth, int newHeight, View view, boolean updateView) {
        validateNewSize(newWidth, newHeight);
        width = newWidth;
        height = newHeight;

        thumbDimension = null; // invalidate cache
        recalcCoSize(view, updateView);
        activeCanvasSizeChanged(this);
    }

    /**
     * Recalculates the component-space size.
     */
    public void recalcCoSize(View view, boolean updateView) {
        double zoomScale = view.getZoomScale();

        int prevCoWidth = coWidth;
        int prevCoHeight = coHeight;

        coWidth = (int) (zoomScale * width);
        coHeight = (int) (zoomScale * height);

        if (updateView && (coWidth != prevCoWidth || coHeight != prevCoHeight)) {
            view.canvasCoSizeChanged();
        }
    }

    /**
     * Called when a new composition (and therefore a new canvas)
     * was activated or when the active canvas size changed.
     */
    public static void activeCanvasSizeChanged(Canvas canvas) {
        // As long as only Symmetry needs to be notified,
        // a listener mechanism isn't necessary.
        Symmetry.activeCanvasSizeChanged(canvas);
    }

    /**
     * Returns the bounds in image space.
     */
    public Rectangle getBounds() {
        return new Rectangle(0, 0, width, height);
    }

    /**
     * Returns the bounds in component space.
     */
    public Rectangle getCoBounds(View view) {
        // assumes that the canvas and view are fully initialized
        return new Rectangle(
            view.getCanvasStartX(), view.getCanvasStartY(), coWidth, coHeight);
    }

    /**
     * Returns the size in image space (image pixels).
     */
    public Dimension getSize() {
        return new Dimension(width, height);
    }

    /**
     * Returns the size in component space (screen pixels).
     */
    public Dimension getCoSize() {
        return new Dimension(coWidth, coHeight);
    }

    public String getSizeString() {
        return width + "x" + height;
    }

    /**
     * Returns the width in image space.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height in image space.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the width in component space.
     */
    public int getCoWidth() {
        return coWidth;
    }

    /**
     * Returns the height in component space.
     */
    public int getCoHeight() {
        return coHeight;
    }

    /**
     * Returns the center point in image space.
     */
    public Point2D getImCenter() {
        return new Point2D.Double(width / 2.0, height / 2.0);
    }

    public double getAspectRatio() {
        return width / (double) height;
    }

    public boolean isLandscape() {
        return width > height;
    }

    /**
     * Creates an image-space AffineTransform to scale content
     * from this canvas size to a target size.
     */
    public AffineTransform createImTransformToFit(Dimension targetSize) {
        double sx = targetSize.getWidth() / width;
        double sy = targetSize.getHeight() / height;
        return AffineTransform.getScaleInstance(sx, sy);
    }

    /**
     * Inverts the given shape with respect to the canvas bounds.
     */
    public Shape invertShape(Shape shape) {
        Area shapeArea = new Area(shape);
        Area fullArea = new Area(getBounds());
        fullArea.subtract(shapeArea);
        return fullArea;
    }

    /**
     * Intersects the given image-space shape with the canvas bounds, and returns the result.
     */
    public Shape clip(Shape shape) {
        assert shape != null;

        Rectangle2D canvasBounds = getBounds();

        if (shape instanceof Rectangle2D rect) {
            // don't ruin the type information in the shape by
            // creating an unnecessary Area, because this is useful
            // info in many ways (crop selection, AA selection clipping)
            return rect.createIntersection(canvasBounds);
        }

        Area canvasArea = new Area(canvasBounds);
        Area shapeArea = new Area(shape);
        shapeArea.intersect(canvasArea);

        return shapeArea;
    }

    /**
     * Returns the intersection of the given rectangle with the canvas bounds.
     */
    public Rectangle intersect(Rectangle rectangle) {
        return SwingUtilities.computeIntersection(
            0, 0, width, height,
            rectangle
        );
    }

    /**
     * Creates a temporary, transparent image with the size of this canvas.
     */
    public BufferedImage createTmpImage() {
        // it's important that the tmp image has an alpha channel
        // even for layer masks, otherwise drawing is not possible
        return ImageUtils.createSysCompatibleImage(this);
    }

    public boolean isFullyCoveredBy(BufferedImage img) {
        return img.getWidth() >= width && img.getHeight() >= height;
    }

    public boolean hasDifferentSizeThan(BufferedImage img) {
        return width != img.getWidth() || height != img.getHeight();
    }

    public boolean hasImSize(int width, int height) {
        return this.width == width && this.height == height;
    }

    /**
     * Generates a random {@link PPoint} within the canvas boundaries.
     */
    public PPoint genRandomPoint(View view) {
        return PPoint.lazyFromIm(Rnd.nextInt(width), Rnd.nextInt(height), view);
    }

    public Dimension getThumbSize() {
        if (thumbDimension == null) {
            thumbDimension = ImageUtils.calcThumbDimensions(
                width, height, Views.thumbSize, true);
        }
        return thumbDimension;
    }

    public String createSVGElement() {
        return ("""
            <svg width="%d" height="%d"
                 xmlns="http://www.w3.org/2000/svg">""")
            .formatted(width, height);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key, this);

        node.addInt("im width", width);
        node.addInt("im height", height);
        node.addInt("co width", coWidth);
        node.addInt("co height", coHeight);

        return node;
    }

    @Override
    public String toString() {
        return "Canvas[" + getSizeString() + ']';
    }
}

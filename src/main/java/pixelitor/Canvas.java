/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.ImageComponent;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.io.Serializable;

/**
 * The painting canvas represents the size of the composition.
 * A layer can be bigger than the canvas if it is partially hidden.
 *
 * If a Composition is deserialized, then the Canvas is also deserialized,
 * and later associated with the (transient!) ImageComponent
 */
public class Canvas implements Serializable {
    public static final int MAX_WIDTH = 9_999;
    public static final int MAX_HEIGHT = 9_999;

    // implementation note: some non-transient field names are inconsistent
    // with their getters, but they cannot be renamed without breaking
    // serialization compatibility with old pxc files

    // size in image space
    private int width;
    private int height;

    // size in component space
    private int zoomedWidth;
    private int zoomedHeight;

    private transient ImageComponent ic;

    // for compatibility with Pixelitor 2.1.0
    private static final long serialVersionUID = -1459254568616232274L;

    public Canvas(int imWidth, int imHeight) {
        this.width = imWidth;
        this.height = imHeight;
    }

    public Canvas(Canvas orig) {
        this.width = orig.width;
        this.height = orig.height;
        this.zoomedWidth = orig.zoomedWidth;
        this.zoomedHeight = orig.zoomedHeight;
    }

    /**
     * Changes the size with values given in image space
     */
    public void changeImSize(int newImWidth, int newImHeight) {
        width = newImWidth;
        height = newImHeight;

        // also update the component space values
        recalcCoSize();
    }

    /**
     * Recalculates the component-space (zoomed) size
     */
    public void recalcCoSize() {
        double viewScale = ic.getViewScale();
        zoomedWidth = (int) (viewScale * width);
        zoomedHeight = (int) (viewScale * height);

        ic.canvasCoSizeChanged();
    }

    /**
     * Returns the bounds in image space, relative to the canvas
     */
    public Rectangle getImBounds() {
        return new Rectangle(0, 0, width, height);
    }

    /**
     * Returns the width in image space
     */
    public int getImWidth() {
        return width;
    }

    /**
     * Returns the height in image space
     */
    public int getImHeight() {
        return height;
    }

    /**
     * Returns the (zoomed) size in component space
     */
    public Dimension getCoSize() {
        return new Dimension(zoomedWidth, zoomedHeight);
    }

    /**
     * Returns the width in component space
     */
    public int getCoWidth() {
        return zoomedWidth;
    }

    /**
     * Returns the height in component space
     */
    public int getCoHeight() {
        return zoomedHeight;
    }

    public void setIC(ImageComponent ic) {
        this.ic = ic;
        if (ic != null) {
            recalcCoSize();
        }
    }

    public Shape invertShape(Shape shape) {
        Area area = new Area(shape);
        Area fullArea = new Area(getImBounds());
        fullArea.subtract(area);
        return fullArea;
    }

    public Shape clipShapeToBounds(Shape shape) {
        assert shape != null;

        Area compBounds = new Area(getImBounds());
        Area result = new Area(shape);
        result.intersect(compBounds);
        return result;
    }

    @Override
    public String toString() {
        return "Canvas{width=" + width
                + ", height=" + height + '}';
    }
}

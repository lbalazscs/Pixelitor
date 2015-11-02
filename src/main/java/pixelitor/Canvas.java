/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.Serializable;

/**
 * The painting canvas represents the size od the composition.
 * A layer can be bigger than the canvas if it is partially hidden.
 */
public class Canvas implements Serializable {
    private int width;
    private int height;

    private int zoomedWidth;
    private int zoomedHeight;

    private transient ImageDisplay ic;

    // for consistency with Pixelitor 2.1.0
    private static final long serialVersionUID = -1459254568616232274L;


    /**
     * If a Composition is deserialized, then this object is also deserialized,
     * and later associated with the (transient!) ImageComponent
     * In the case of a new image, this object is first created in ImageComponent
     */
    public Canvas(int width, int height) {
        this.width = width;
        this.height = height;
//        this.ic = ic;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Rectangle getBounds() {
        return new Rectangle(0, 0, width, height);
    }

    public void updateSize(int newWidth, int newHeight) {
        width = newWidth;
        height = newHeight;

        double viewScale = ic.getViewScale();
        zoomedWidth = (int) (viewScale * newWidth);
        zoomedHeight = (int) (viewScale * newHeight);

        ic.canvasSizeChanged();
    }

    public void updateForZoom(double viewScale) {
        zoomedWidth = (int) (viewScale * width);
        zoomedHeight = (int) (viewScale * height);
    }

    public Dimension getZoomedSize() {
        return new Dimension(zoomedWidth, zoomedHeight);
    }

    public int getZoomedWidth() {
        return zoomedWidth;
    }

    public int getZoomedHeight() {
        return zoomedHeight;
    }

    public void setIc(ImageDisplay ic) {
        this.ic = ic;
    }

    @Override
    public String toString() {
        return "Canvas{width=" + width
                + ", height=" + height + '}';
    }
}

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

package pixelitor.layers;

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * A temporary drawing layer for the tools that use blending modes.
 */
public class TmpDrawingLayer {
    private BufferedImage image;
    private final Graphics2D g;
    private final Composite composite;

    public TmpDrawingLayer(ImageLayer imageLayer, Composite composite) {
        this.composite = Objects.requireNonNull(composite);

        // the image is never translated,
        // the coordinates are relative to the canvas
        image = imageLayer.createCanvasSizedTmpImage();

        g = image.createGraphics();
    }

    public Graphics2D getGraphics() {
        return g;
    }

    public int getWidth() {
        return image.getWidth();
    }

    public int getHeight() {
        return image.getHeight();
    }

    public void dispose() {
        g.dispose();
        image.flush();
        image = null;
    }

    public void paintOn(Graphics2D g, int tx, int ty) {
        g.setComposite(composite);
        g.drawImage(image, tx, ty, null);
    }
}

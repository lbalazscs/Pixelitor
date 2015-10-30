/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.layers;

import pixelitor.Composition;

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * "Real" in the sense that this has a different image that
 * is merged down at the end of a brush stroke, allowing tools
 * to have blending modes.
 */
public class RealTmpDrawingLayer implements TmpDrawingLayer {
    private BufferedImage image;
    protected final Graphics2D g;
    private final Composite composite;

    public RealTmpDrawingLayer(ImageLayer imageLayer, Composite composite, boolean respectSelection) {
        this.composite = composite;

        Composition comp = imageLayer.getComp();

        // the image is never translated, the coordinates are relative to the canvas
        image = imageLayer.createCompositionSizedTmpImage();
        g = image.createGraphics();

        if (respectSelection) {
            comp.applySelectionClipping(g, null);
        }
    }

    @Override
    public Graphics2D getGraphics() {
        return g;
    }

    @Override
    public int getWidth() {
        return image.getWidth();
    }

    @Override
    public int getHeight() {
        return image.getHeight();
    }

    @Override
    public void dispose() {
        g.dispose();
        image.flush();
        image = null;
    }

    @Override
    public void paintLayer(Graphics2D g, int tx, int ty) {
        if (composite == null) {
            throw new IllegalStateException("tmpDrawingComposite == null");
        }

        g.setComposite(composite);
        g.drawImage(image, tx, ty, null);
    }
}

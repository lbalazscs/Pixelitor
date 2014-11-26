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
 * A temporary drawing layer used bu the brush and gradient tools
 */
public class TmpDrawingLayer {
    private BufferedImage image;
    private final Graphics2D g;
    private final Composite composite;

    private final int tmpDrawingX = 0;
    private final int tmpDrawingY = 0;

    public TmpDrawingLayer(ImageLayer imageLayer, Composite composite, boolean respectSelection) {
        this.composite = composite;

        Composition comp = imageLayer.getComposition();

        // the image is never translated, the coordinates are relative to the canvas
        image = imageLayer.createCompositionSizedTmpImage();
        g = image.createGraphics();

        if (respectSelection) {
            comp.setSelectionClipping(g, null);
        }
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

    public void paintLayer(Graphics2D g, int translationX, int translationY) {
        if (composite == null) {
            throw new IllegalStateException("tmpDrawingComposite == null");
        }

        g.setComposite(composite);
        g.drawImage(image, tmpDrawingX + translationX, tmpDrawingY + translationY, null);
    }


}

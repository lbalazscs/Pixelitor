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

import pixelitor.Canvas;
import pixelitor.Composition;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * A "fake" temporary drawing layer used when the edited
 * image layer is a layer mask.
 * It is fake in the sense that it has no image of its
 * own, it forwards the drawing to the layer mask image directly.
 * This way it cannot support compositing.
 */
public class FakeTmpDrawingLayer implements TmpDrawingLayer {
    private final Graphics2D g;
    private final Canvas canvas;

    public FakeTmpDrawingLayer(LayerMask mask, boolean respectSelection) {
        Composition comp = mask.getComp();
        canvas = comp.getCanvas();

        // shares the pixels of the original, but translated
        BufferedImage image = mask.getCanvasSizedSubImage();
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
        return canvas.getWidth();
    }

    @Override
    public int getHeight() {
        return canvas.getHeight();
    }

    @Override
    public void dispose() {
        g.dispose();
    }

    @Override
    public void paintLayer(Graphics2D g, int translationX, int translationY) {
        // do nothing
    }
}

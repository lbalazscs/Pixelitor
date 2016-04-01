/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import pixelitor.Composition;
import pixelitor.filters.comp.Flip;
import pixelitor.filters.comp.Rotate;
import pixelitor.history.ContentLayerMoveEdit;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * A "smart object" that contains an embedded composition
 */
public class SmartObject extends ContentLayer {
    private Composition content;

    SmartObject(Composition comp, String name, Layer parent) {
        super(comp, name, parent);
    }

    @Override
    ContentLayerMoveEdit createMovementEdit(int oldTX, int oldTY) {
        return null;
    }

    @Override
    public void flip(Flip.Direction direction) {

    }

    @Override
    public void rotate(Rotate.SpecialAngle angle) {

    }

    @Override
    public void enlargeCanvas(int north, int east, int south, int west) {

    }

    @Override
    public Layer duplicate(boolean sameName) {
        return null; // TODO
    }

    @Override
    public void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer) {
        g.drawImage(content.getCompositeImage(), getTX(), getTY(), null);
    }

    @Override
    protected BufferedImage adjustImage(BufferedImage src) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resize(int targetWidth, int targetHeight, boolean progressiveBilinear) {

    }

    @Override
    public void crop(Rectangle2D cropRect) {

    }
}

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
import pixelitor.filters.comp.Flip;
import pixelitor.history.ContentLayerMoveEdit;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

/**
 *
 */
public class ShapeLayer extends ContentLayer {
    private static final long serialVersionUID = 1L;

    private Shape shape;

    public ShapeLayer(Composition comp, String name, Shape shape) {
        super(comp, name, null);
        this.shape = shape;
    }

    /**
     * This constructor is used by the TextLayer subclass. It cannot supply a Shape object right now,
     * because creating a Shape from a text depends on the current Graphics object
     */
    protected ShapeLayer(Composition comp, String name) {
        super(comp, name, null);
    }

    /**
     * Overridden in the TextLayer subclass.
     */
    public Shape getShape(Graphics2D g) {
        return shape;
    }


    @Override
    public Layer duplicate() {
        // Only immutable Shape objects can be shared this way
        ShapeLayer d = new ShapeLayer(comp, getDuplicateLayerName(), shape);
        if (hasMask()) {
            d.addMaskBack(mask.duplicate(d));
        }
        return d;
    }

    @Override
    ContentLayerMoveEdit createMovementEdit(int oldTranslationX, int oldTranslationY) {
        // TODO
        return null;
    }

    @Override
    public void flip(Flip.Direction direction) {
        // TODO
    }

    @Override
    public void rotate(int angleDegree) {
        // TODO
    }

    @Override
    public void enlargeCanvas(int north, int east, int south, int west) {
        // TODO
    }

    @Override
    public void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer) {
        // TODO
    }

    @Override
    public void resize(int targetWidth, int targetHeight, boolean progressiveBilinear) {
        resizeMask(targetWidth, targetHeight, progressiveBilinear);
        // TODO
    }

    @Override
    public void crop(Rectangle selectionBounds) {
        cropMask(selectionBounds);
        // TODO
    }
}

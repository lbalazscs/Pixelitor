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

package pixelitor.layers;

import pixelitor.Composition;
import pixelitor.compactions.Flip;
import pixelitor.history.ContentLayerMoveEdit;
import pixelitor.utils.QuadrantAngle;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

/**
 * A "smart object" that contains an embedded composition
 *
 * Not fully implemented yet!
 */
public class SmartObject extends ContentLayer {
    private Composition content;

    SmartObject(Composition comp, String name) {
        super(comp, name);
    }

    @Override
    ContentLayerMoveEdit createMovementEdit(int oldTx, int oldTy) {
        return null;
    }

    @Override
    public void flip(Flip.Direction direction) {

    }

    @Override
    public void rotate(QuadrantAngle angle) {

    }

    @Override
    public void enlargeCanvas(int north, int east, int south, int west) {

    }

    @Override
    public Rectangle getContentBounds() {
        return null;
    }

    @Override
    protected Layer createTypeSpecificDuplicate(String duplicateName) {
        return new SmartObject(comp, duplicateName);
    }

    @Override
    public void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer) {
        g.drawImage(content.getCompositeImage(), getTx(), getTy(), null);
    }

    @Override
    protected BufferedImage applyOnImage(BufferedImage src) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> resize(Dimension newSize) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void crop(Rectangle2D cropRect, boolean deleteCroppedPixels, boolean allowGrowing) {
        if (!deleteCroppedPixels) {
            super.crop(cropRect, deleteCroppedPixels, allowGrowing);
        }
    }

    @Override
    public Rectangle getEffectiveBoundingBox() {
        // unknown, return empty
        return new Rectangle();
    }

    @Override
    public Rectangle getSnappingBoundingBox() {
        // unknown, return empty
        return new Rectangle();
    }

    @Override
    public int getPixelAtPoint(Point p) {
        // unknown
        return 0x00000000;
    }

    @Override
    public BufferedImage asImage(boolean applyMask) {
        return null;
    }
}

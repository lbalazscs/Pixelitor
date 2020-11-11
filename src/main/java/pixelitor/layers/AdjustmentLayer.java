/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.Filter;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.utils.Utils;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.concurrent.CompletableFuture;

/**
 * A global adjustment to all the layers that are bellow this layer
 *
 * (Not fully implemented and not enabled by default.
 * Most importantly the editing of filter parameters is missing)
 */
public class AdjustmentLayer extends Layer {
    @Serial
    private static final long serialVersionUID = 2L;

    private final Filter filter;

    public AdjustmentLayer(Composition comp, String name, Filter filter) {
        super(comp, name, null);
        this.filter = filter;
        isAdjustment = true;
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        isAdjustment = true;
    }

    @Override
    public Layer duplicate(boolean compCopy) {
        // TODO the filter should be copied so that it can be adjusted independently
        String duplicateName = compCopy ? name : Utils.createCopyName(name);
        var copy = new AdjustmentLayer(comp, duplicateName, filter);

        duplicateMask(copy, compCopy);

        return copy;
    }

    @Override
    public CompletableFuture<Void> resize(Dimension newSize) {
        // do nothing
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void crop(Rectangle2D cropRect, boolean deleteCroppedPixels, boolean allowGrowing) {
        // do nothing
    }

    @Override
    public BufferedImage actOnImageFromLayerBellow(BufferedImage src) {
        return filter.transformImage(src);
    }

    @Override
    public void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BufferedImage getRepresentingImage() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "{" + "filter=" + (filter == null ? "null filter" : filter.getName())
                + ", super=" + super.toString() + '}';
    }

    public void configure() {
        System.out.println("AdjustmentLayer::configure: 1");
        if (!(filter instanceof FilterWithGUI)) {
            return;
        }
        System.out.println("AdjustmentLayer::configure: 2");
    }
}

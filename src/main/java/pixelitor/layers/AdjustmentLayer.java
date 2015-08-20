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
import pixelitor.filters.Filter;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * A global adjustment to all the layers that are bellow this layer
 */
public class AdjustmentLayer extends Layer {
    private static final long serialVersionUID = 2L;

    private final Filter filter;

    public AdjustmentLayer(Composition comp, String name, Filter filter) {
        super(comp, name, null);
        this.filter = filter;
        isAdjustment = true;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        isAdjustment = true;
    }

    @Override
    public Layer duplicate() {
        // TODO operation  should be copied so that it can be adjusted independently
        AdjustmentLayer d = new AdjustmentLayer(comp, getDuplicateLayerName(), filter);

        if (hasMask()) {
            d.addMaskBack(mask.duplicate(d));
        }

        return d;
    }

    @Override
    public void resize(int targetWidth, int targetHeight, boolean progressiveBilinear) {
        // do nothing
    }

    @Override
    public void crop(Rectangle2D cropRect) {
        // do nothing
    }

    @Override
    public BufferedImage adjustImage(BufferedImage src) {
        return filter.executeForOneLayer(src);
    }

    @Override
    public void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName() + "{");
        sb.append("filter=").append(filter == null ? "null filter" : filter.getName());
        sb.append(", super=").append(super.toString());
        sb.append('}');
        return sb.toString();
    }

}

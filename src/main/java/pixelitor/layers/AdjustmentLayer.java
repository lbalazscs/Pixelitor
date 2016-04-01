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
import pixelitor.filters.Filter;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.utils.Utils;

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
    public Layer duplicate(boolean sameName) {
        // TODO the filter should be copied so that it can be adjusted independently
        String duplicateName = sameName ? name : Utils.createCopyName(name);
        AdjustmentLayer d = new AdjustmentLayer(comp, duplicateName, filter);

        if (hasMask()) {
            d.addMask(mask.duplicate(d));
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

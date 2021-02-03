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

package pixelitor.filters;

import com.jhlabs.image.AbstractBufferedImageOp;

import java.awt.image.BufferedImage;
import java.util.function.Supplier;

/**
 * A filter without GUI that delegates the
 * filtering to an AbstractBufferedImageOp
 */
public class SimpleForwardingFilter extends Filter {
    private final Supplier<AbstractBufferedImageOp> factory;
    private final boolean supportsGray;
    private AbstractBufferedImageOp filter;

    public SimpleForwardingFilter(Supplier<AbstractBufferedImageOp> factory,
                                  boolean supportsGray) {
        this.factory = factory;
        this.supportsGray = supportsGray;
    }

    @Override
    protected BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = factory.get();
        }
        return filter.filter(src, dest);
    }

    @Override
    public boolean supportsGray() {
        return supportsGray;
    }
}

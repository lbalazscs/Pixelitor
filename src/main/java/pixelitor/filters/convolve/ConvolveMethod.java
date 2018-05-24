/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.convolve;

import com.jhlabs.image.ConvolveFilter;

import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

/**
 * An algorithm for convolution
 */
enum ConvolveMethod {
    JHLabs("JHLabs ConvolveFilter (Better)") {
        @Override
        BufferedImageOp getConvolveOp(Kernel kernel, String filterName) {
            ConvolveFilter filter = new ConvolveFilter(kernel, filterName);
            filter.setEdgeAction(ConvolveFilter.CLAMP_EDGES);
            filter.setPremultiplyAlpha(false);
            filter.setUseAlpha(false);
            return filter;
        }
    },
    /**
     * This is about 3x faster, but does not handle the edges,
     * and it does not work for edge finding
     */
    AWT("AWT ConvolveOp (Faster)") {
        @Override
        BufferedImageOp getConvolveOp(Kernel kernel, String filterName) {
            return new ConvolveOp(kernel);
        }
    };

    private final String guiName;

    ConvolveMethod(String guiName) {
        this.guiName = guiName;
    }

    abstract BufferedImageOp getConvolveOp(Kernel kernel, String filterName);

    @Override
    public String toString() {
        return guiName;
    }
}

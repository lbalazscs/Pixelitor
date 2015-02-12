/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
    JHLabs {
        @Override
        BufferedImageOp getConvolveOp(Kernel kernel) {
            ConvolveFilter filter = new ConvolveFilter(kernel);
            filter.setEdgeAction(ConvolveFilter.CLAMP_EDGES);
            filter.setPremultiplyAlpha(false);
            filter.setUseAlpha(false);
            return filter;
        }

        @Override
        public String toString() {
            return "JHLabs ConvolveFilter (Better)";
        }
    }, AWT {
        @Override
        BufferedImageOp getConvolveOp(Kernel kernel) {
            ConvolveOp op = new ConvolveOp(kernel);
            return op;
        }

        @Override
        public String toString() {
            return "AWT ConvolveOp (Faster)";
        }
    };

    abstract BufferedImageOp getConvolveOp(Kernel kernel);
}

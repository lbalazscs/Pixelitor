/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.MotionBlur;
import com.jhlabs.image.MotionBlurFilter;
import com.jhlabs.image.MotionBlurOp;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

enum MotionBlurQuality {
    FASTER("Faster") {
        @Override
        public MotionBlur createFilter(String filterName, BufferedImage src) {
            return new MotionBlurOp(filterName);
        }
    }, BETTER("High Quality (slower)") {
        @Override
        public MotionBlur createFilter(String filterName, BufferedImage src) {
            var filter = new MotionBlurFilter(filterName);
            filter.setPremultiplyAlpha(!src.isAlphaPremultiplied() && ImageUtils.hasPackedIntArray(src));
            filter.setWrapEdges(false);
            return filter;
        }
    };

    private final String displayName;

    MotionBlurQuality(String displayName) {
        this.displayName = displayName;
    }

    public abstract MotionBlur createFilter(String filterName, BufferedImage src);

    @Override
    public String toString() {
        return displayName;
    }
}

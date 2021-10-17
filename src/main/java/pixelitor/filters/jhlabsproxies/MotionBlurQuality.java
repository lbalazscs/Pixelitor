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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.MotionBlur;
import com.jhlabs.image.MotionBlurFilter;
import com.jhlabs.image.MotionBlurOp;

enum MotionBlurQuality {
    FASTER("Faster") {
        @Override
        public MotionBlur createFilter(String filterName) {
            return new MotionBlurOp(filterName);
        }
    }, BETTER("High Quality (slower)") {
        @Override
        public MotionBlur createFilter(String filterName) {
            var filter = new MotionBlurFilter(filterName);
            filter.setPremultiplyAlpha(true);
            filter.setWrapEdges(false);
            return filter;
        }
    };

    private final String guiName;

    MotionBlurQuality(String guiName) {
        this.guiName = guiName;
    }

    public abstract MotionBlur createFilter(String filterName);

    @Override
    public String toString() {
        return guiName;
    }
}

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

package pixelitor.filters.util;

import com.jhlabs.image.ImageMath;

public enum NoiseInterpolation {
    NONE("None") {
        @Override
        public float step(float x) {
            return x < 0.5 ? 0 : 1;
        }
    }, LINEAR("Linear (uniform)") {
        @Override
        public float step(float x) {
            return x;
        }
    }, CUBIC("Cubic (smooth)") {
        @Override
        public float step(float x) {
            return ImageMath.smoothStep01(x);
        }
    }, QUINTIC("Quintic (smoother)") {
        @Override
        public float step(float x) {
            return ImageMath.smootherStep01(x);
        }
    };

    private final String guiName;

    NoiseInterpolation(String guiName) {
        this.guiName = guiName;
    }

    /**
     * Transforms the normalized time into the normalized animation progress
     *
     * @param time a value between 0 and 1
     * @return a value between 0 and 1
     */
    public abstract float step(float x);

    @Override
    public String toString() {
        return guiName;
    }
}

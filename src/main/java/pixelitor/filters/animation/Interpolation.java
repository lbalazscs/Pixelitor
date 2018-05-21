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

package pixelitor.filters.animation;

/**
 * Different ways to interpolate between two values
 * as the time passes
 *
 * The four curves can be viewed here:
 * http://www.wolframalpha.com/input/?i=Plot[{x%2C+x*x%2C+-x*%28x+-+2%29%2C++x*x*%283+-+2*x%29}%2C+{x%2C+0%2C+1}]
 */
public enum Interpolation {
    LINEAR("Linear (uniform)") {
        @Override
        double time2progress(double time) {
            return time;
        }
    }, QUAD_EASE_IN("Ease In (slow start)") {
        @Override
        double time2progress(double time) {
            return time * time;
        }
    }, QUAD_EASE_OUT("Ease Out (slow stop)") {
        @Override
        double time2progress(double time) {
            return -time * (time - 2);
        }
    }, EASE_IN_OUT("Ease In and Out") {
        @Override
        double time2progress(double time) {
            return time * time * (3 - 2 * time);
        }
    };

    private final String guiName;

    Interpolation(String guiName) {
        this.guiName = guiName;
    }

    /**
     * Transforms the normalized time into the normalized animation progress
     *
     * @param time a value between 0 and 1
     * @return a value between 0 and 1
     */
    abstract double time2progress(double time);

    @Override
    public String toString() {
        return guiName;
    }
}

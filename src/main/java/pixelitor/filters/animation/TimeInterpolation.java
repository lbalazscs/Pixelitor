/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
 * Different interpolation methods for tweening animations.
 */
public enum TimeInterpolation {
    LINEAR("Linear (Uniform)") {
        @Override
        double time2progress(double time) {
            return time;
        }
    }, EASE_IN("Ease In (Slow Start)") {
        @Override
        double time2progress(double time) {
            // quadratic easing in: f(t) = t²
            return time * time;
        }
    }, EASE_OUT("Ease Out (Slow Stop)") {
        @Override
        double time2progress(double time) {
            // quadratic easing out: f(t) = 1 - (1-t)²
            return -time * (time - 2);
        }
    }, EASE_IN_OUT("Ease In and Out") {
        @Override
        double time2progress(double time) {
            // cubic easing in/out (smoothstep): f(t) = 3t² - 2t³
            return time * time * (3 - 2 * time);
        }
    };

    private final String displayName;

    TimeInterpolation(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Transforms a linear time value (0.0 to 1.0) into a progress
     * value that determines the actual interpolation between states.
     */
    abstract double time2progress(double time);

    @Override
    public String toString() {
        return displayName;
    }
}

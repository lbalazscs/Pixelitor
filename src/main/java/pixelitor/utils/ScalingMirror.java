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

package pixelitor.utils;

/**
 * A mirroring effect achieved with negative scaling
 */
public enum ScalingMirror {
    NONE("None") {
        @Override
        public double getScaleX(double scaleAbs) {
            return scaleAbs;
        }

        @Override
        public double getScaleY(double scaleAbs) {
            return scaleAbs;
        }
    }, VERTICAL("Vertical") {
        @Override
        public double getScaleX(double scaleAbs) {
            return -1 * scaleAbs;
        }

        @Override
        public double getScaleY(double scaleAbs) {
            return scaleAbs;
        }
    }, HORIZONTAL("Horizontal") {
        @Override
        public double getScaleX(double scaleAbs) {
            return scaleAbs;
        }

        @Override
        public double getScaleY(double scaleAbs) {
            return -1 * scaleAbs;
        }
    };

    private final String guiName;

    ScalingMirror(String guiName) {
        this.guiName = guiName;
    }

    public abstract double getScaleX(double scaleAbs);

    public abstract double getScaleY(double scaleAbs);

    @Override
    public String toString() {
        return guiName;
    }
}

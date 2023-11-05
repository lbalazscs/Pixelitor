/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
 * Units of angle, following a pattern similar to the JDK class TimeUnit.
 */
public enum AngleUnit {
    /**
     * Radians between -PI and PI, as returned form Math.atan2
     */
    RADIANS {
        @Override
        public double toRadians(double a) {
            return a;
        }

        @Override
        public double toIntuitiveRadians(double a) {
            return Geometry.atan2ToIntuitive(a);
        }

        @Override
        public double toDegrees(double a) {
            return Math.toDegrees(a);
        }

        @Override
        public double toIntuitiveDegrees(double a) {
            return Math.toDegrees(Geometry.atan2ToIntuitive(a));
        }
    },
    /**
     * Radians between 0 and 2*PI, and in the intuitive (counter-clockwise) direction
     */
    CCW_RADIANS {
        @Override
        public double toRadians(double a) {
            return Geometry.intuitiveToAtan2(a);
        }

        @Override
        public double toIntuitiveRadians(double a) {
            return a;
        }

        @Override
        public double toDegrees(double a) {
            return Math.toDegrees(Geometry.intuitiveToAtan2(a));
        }

        @Override
        public double toIntuitiveDegrees(double a) {
            return Math.toDegrees(a);
        }
    },
    /**
     * Degrees between -180 and 180, in the clockwise direction
     */
    DEGREES {
        @Override
        public double toRadians(double a) {
            return Math.toRadians(a);
        }

        @Override
        public double toIntuitiveRadians(double a) {
            return Math.toRadians(Geometry.atan2ToIntuitive(a));
        }

        @Override
        public double toDegrees(double a) {
            return a;
        }

        @Override
        public double toIntuitiveDegrees(double a) {
            if (a <= 0) {
                a = -a;
            } else {
                a = 360 - a;
            }
            return a;
        }
    },
    /**
     * Degrees between 0 and 360, and in the intuitive (counter-clockwise) direction
     */
    CCW_DEGREES {
        @Override
        public double toRadians(double a) {
            assert a >= 0 && a <= 360 : "a = " + a;
            return Math.toRadians(360 - a);
        }

        @Override
        public double toIntuitiveRadians(double a) {
            return Math.toRadians(a);
        }

        @Override
        public double toDegrees(double a) {
            if (a > 180) {
                return 360 - a;
            } else {
                return -a;
            }
        }

        @Override
        public double toIntuitiveDegrees(double a) {
            return a;
        }
    };

    public abstract double toRadians(double a);

    public abstract double toIntuitiveRadians(double a);

    public abstract double toDegrees(double a);

    public abstract double toIntuitiveDegrees(double a);
}


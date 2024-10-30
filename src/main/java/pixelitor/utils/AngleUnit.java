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

package pixelitor.utils;

/**
 * Units of angle measurement, with conversion functions between
 * different representations. It's similar to the JDK class TimeUnit.
 */
public enum AngleUnit {
    /**
     * Clockwise radians between -π and π, as returned from Math.atan2()
     */
    RADIANS {
        @Override
        public double toRadians(double angle) {
            return angle;
        }

        @Override
        public double toIntuitiveRadians(double angle) {
            return Geometry.atan2ToIntuitive(angle);
        }

        @Override
        public double toDegrees(double angle) {
            return Math.toDegrees(angle);
        }

        @Override
        public double toIntuitiveDegrees(double angle) {
            return Geometry.toIntuitiveDegrees(angle);
        }
    },
    /**
     * Counter-clockwise radians between 0 and 2π
     */
    INTUITIVE_RADIANS {
        @Override
        public double toRadians(double angle) {
            return Geometry.intuitiveToAtan2(angle);
        }

        @Override
        public double toIntuitiveRadians(double angle) {
            return angle;
        }

        @Override
        public double toDegrees(double angle) {
            return Math.toDegrees(Geometry.intuitiveToAtan2(angle));
        }

        @Override
        public double toIntuitiveDegrees(double angle) {
            return Math.toDegrees(angle);
        }
    },
    /**
     * Clockwise degrees between -180 and 180
     */
    DEGREES {
        @Override
        public double toRadians(double angle) {
            return Math.toRadians(angle);
        }

        @Override
        public double toIntuitiveRadians(double angle) {
            return Math.toRadians(Geometry.atan2ToIntuitive(angle));
        }

        @Override
        public double toDegrees(double angle) {
            return angle;
        }

        @Override
        public double toIntuitiveDegrees(double angle) {
            if (angle <= 0) {
                angle = -angle;
            } else {
                angle = 360 - angle;
            }
            return angle;
        }
    },
    /**
     * Degrees between 0 and 360, and in the intuitive (counter-clockwise) direction
     */
    INTUITIVE_DEGREES {
        @Override
        public double toRadians(double angle) {
            assert angle >= 0 && angle <= 360 : "a = " + angle;
            return Math.toRadians(360 - angle);
        }

        @Override
        public double toIntuitiveRadians(double angle) {
            return Math.toRadians(angle);
        }

        @Override
        public double toDegrees(double angle) {
            if (angle > 180) {
                return 360 - angle;
            } else {
                return -angle;
            }
        }

        @Override
        public double toIntuitiveDegrees(double angle) {
            return angle;
        }
    };

    public abstract double toRadians(double angle);

    public abstract double toIntuitiveRadians(double angle);

    public abstract double toDegrees(double angle);

    public abstract double toIntuitiveDegrees(double angle);
}


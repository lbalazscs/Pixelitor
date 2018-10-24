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

package pixelitor.utils;

/**
 * Angle units, similar in spirit to the JDK TimeUnit
 */
public enum AngleUnit {
    /**
     * Radians between -PI and PI, as returned form Math.atan2
     */
    ATAN2_RADIANS {
        @Override
        public double toAtan2Radians(double a) {
            return a;
        }

        @Override
        public double toIntuitiveRadians(double a) {
            return Utils.atan2AngleToIntuitive(a);
        }

        @Override
        public double toAtan2Degrees(double a) {
            return Math.toDegrees(a);
        }

        @Override
        public double toIntuitiveDegrees(double a) {
            return Math.toDegrees(Utils.atan2AngleToIntuitive(a));
        }
    },
    /**
     * Radians between 0 and 2*PI, and in the intuitive (CCW) direction
     */
    INTUITIVE_RADIANS {
        @Override
        public double toAtan2Radians(double a) {
            return Utils.intuitiveToAtan2Angle(a);
        }

        @Override
        public double toIntuitiveRadians(double a) {
            return a;
        }

        @Override
        public double toAtan2Degrees(double a) {
            return Math.toDegrees(Utils.intuitiveToAtan2Angle(a));
        }

        @Override
        public double toIntuitiveDegrees(double a) {
            return Math.toDegrees(a);
        }
    },
    /**
     * Degrees between -180 and 180, corresponding to ATAN2_RADIANS
     */
    ATAN2_DEGREES {
        @Override
        public double toAtan2Radians(double a) {
            return Math.toRadians(a);
        }

        @Override
        public double toIntuitiveRadians(double a) {
            return Math.toRadians(Utils.atan2AngleToIntuitive(a));
        }

        @Override
        public double toAtan2Degrees(double a) {
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
     * Degrees between 0 and 360, corresponding to INTUITIVE_RADIANS
     */
    INTUITIVE_DEGREES {
        @Override
        public double toAtan2Radians(double a) {
            return Utils.intuitiveToAtan2Angle(Math.toRadians(a));
        }

        @Override
        public double toIntuitiveRadians(double a) {
            return Math.toRadians(a);
        }

        @Override
        public double toAtan2Degrees(double a) {
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

    public abstract double toAtan2Radians(double a);

    public abstract double toIntuitiveRadians(double a);

    public abstract double toAtan2Degrees(double a);

    public abstract double toIntuitiveDegrees(double a);
}


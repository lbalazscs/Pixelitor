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

package pixelitor.tools.brushes;

/**
 * Angle-related settings for the brushes
 */
public class AngleSettings {
    private final boolean angleAware;
    private final double maxAngleJitter;

    // global shared immutable instances for the most common cases
    public static final AngleSettings NOT_ANGLE_AWARE = new AngleSettings(false, 0);
    public static final AngleSettings ANGLE_AWARE_NO_JITTER = new AngleSettings(true, 0);

    public AngleSettings(boolean angleAware, double maxAngleJitter) {
        this.angleAware = angleAware;
        this.maxAngleJitter = maxAngleJitter;
    }

    public boolean isAngleAware() {
        return angleAware;
    }

    public boolean shouldJitterAngle() {
        return maxAngleJitter > 0;
    }

    public double calculatePerturbedAngle(double theta) {
        double retVal = theta;

        retVal -= maxAngleJitter;
        retVal += 2 * maxAngleJitter * Math.random();

        return retVal;
    }
}

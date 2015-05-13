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
    private final boolean shouldScatterAngle;
    private final double maxAngleScattering;

    // global shared immutable instance for the most common case
    public static final AngleSettings NOT_ANGLE_AWARE = new AngleSettings(false, false, 0);

    public AngleSettings(boolean angleAware, boolean shouldScatterAngle, double maxAngleScattering) {
        if (angleAware) {
            if (shouldScatterAngle && maxAngleScattering == 0) {
                throw new IllegalArgumentException("no maxAngleScattering");
            }
        } else {
            if (shouldScatterAngle || maxAngleScattering != 0) {
                throw new IllegalArgumentException("shouldScatterAngle = " + shouldScatterAngle + ", maxAngleScattering = " + maxAngleScattering);
            }
        }

        this.angleAware = angleAware;
        this.shouldScatterAngle = shouldScatterAngle;
        this.maxAngleScattering = maxAngleScattering;
    }

    public boolean isAngleAware() {
        return angleAware;
    }

    public boolean shouldScatterAngle() {
        return shouldScatterAngle;
    }

    public double calculateScatteredAngle(double theta) {
        double usedAngle = theta;

        usedAngle -= maxAngleScattering;
        usedAngle += 2 * maxAngleScattering * Math.random();

        return usedAngle;
    }
}

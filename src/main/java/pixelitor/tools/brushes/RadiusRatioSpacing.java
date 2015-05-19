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
 * A spacing strategy where the spacing is proportional to the radius
 */
public class RadiusRatioSpacing implements SpacingStrategy {
    private double spacingRatio = 2.0; // the spacing relative to the radius

    public RadiusRatioSpacing(double spacingRatio) {
        this.spacingRatio = spacingRatio;
    }

    @Override
    public double getSpacing(int radius) {
        return radius * spacingRatio;
    }
}

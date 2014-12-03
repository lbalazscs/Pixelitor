/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters.gui;

import com.jhlabs.image.ImageMath;

public class RangeParamState implements ParamState {
    double value;

    public RangeParamState(double value) {
        this.value = value;
    }

    @Override
    public RangeParamState interpolate(ParamState endState, double progress) {
        RangeParamState rpEndState = (RangeParamState) endState;
        double interpolated = ImageMath.lerp(progress, value, rpEndState.value);
        return new RangeParamState(interpolated);
    }

    public double getValue() {
        return value;
    }
}

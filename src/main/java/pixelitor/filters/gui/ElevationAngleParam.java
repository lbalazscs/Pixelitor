/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.gui;

import pixelitor.utils.Rnd;

/**
 * A filter parameter for selecting the
 * elevation (altitude) angle of a light source
 */
public class ElevationAngleParam extends AngleParam {

    public ElevationAngleParam(String name, double defaultValue) {
        super(name, defaultValue);
    }

    @Override
    public AbstractAngleUI getAngleSelectorUI() {
        return new ElevationAngleUI(this);
    }

    @Override
    public int getMaxAngleInDegrees() {
        return 90;
    }

    @Override
    public void setValue(double r, boolean trigger) {
        if (r >= 1.5 * Math.PI) {
            // values between 1.5*PI and 2*PI are coming
            // when the user drags the slider, they are OK
        } else if (r > 0) {
            r = 0;
        } else if (r < -Math.PI / 2) {
            r = -Math.PI / 2;
        }

        super.setValue(r, trigger);
    }

    @Override
    public void randomize() {
        int val = Rnd.nextInt(90);
        setValueInDegrees(val, false);
    }
}

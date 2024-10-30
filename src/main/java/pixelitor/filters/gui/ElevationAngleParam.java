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

package pixelitor.filters.gui;

import pixelitor.utils.AngleUnit;
import pixelitor.utils.Rnd;

/**
 * A specialized angle parameter for selecting elevation (altitude)
 * angles. Unlike the regular AngleParam which allows full 360-degree
 * rotation, this is constrained to angles between 0 and 90 degrees.
 */
public class ElevationAngleParam extends AngleParam {
    public ElevationAngleParam(String name, double def) {
        super(name, def);
    }

    public ElevationAngleParam(String name, double def, AngleUnit unit) {
        super(name, def, unit);
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
            r = 0; // clamp to horizontal
        } else if (r < -Math.PI / 2) {
            r = -Math.PI / 2; // clamp to vertical
        }

        super.setValue(r, trigger);
    }

    @Override
    protected void doRandomize() {
        int val = Rnd.nextInt(91);
        setValueInDegrees(val, false);
    }
}

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
package pixelitor.tryout;

import pixelitor.filters.impl.PolarTilesFilter;

public class TestBrokenGlass {
    public static void main(String[] args) {
        PolarTilesFilter filter = new PolarTilesFilter();
        filter.setZoom(1.0f);

        filter.setCenterX(0.5f);
        filter.setCenterY(0.5f);
        filter.setRandomness(0.0f);
        filter.setT(0.0f);
        filter.setNumADivisions(7);
        filter.setRotateResult(0.5);
    }


}

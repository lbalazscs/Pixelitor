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

package pixelitor.filters.gui;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertNotNull;

@RunWith(Parameterized.class)
public class ParamStateTest {
    private ParamState start;
    private ParamState end;

    public ParamStateTest(ParamState start, ParamState end) {
        this.end = end;
        this.start = start;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> instancesToTest() {
        GUIParam angleParamStart = new AngleParam("AngleParam", 0);
        GUIParam angleParamEnd = new AngleParam("AngleParam", 1);

        GUIParam rangeParamStart = new RangeParam("RangeParam", 0, 100, 10);
        GUIParam rangeParamEnd = new RangeParam("RangeParam", 0, 100, 100);

        GUIParam groupedRangeParamStart = new GroupedRangeParam("GroupedRangeParam", 0, 200, 0);
        GUIParam groupedRangeParamEnd = new GroupedRangeParam("GroupedRangeParam", 0, 200, 0);

        GUIParam gradientParamStart = new GradientParam("GradientParam", Color.BLACK, Color.GREEN);
        GUIParam gradientParamEnd = new GradientParam("GradientParam", Color.BLUE, Color.RED);

        GUIParam imagePositionParamStart = new ImagePositionParam("ImagePositionParam", 0.1f, 0.0f);
        GUIParam imagePositionParamEnd = new ImagePositionParam("ImagePositionParam", 0.9f, 1.0f);

        GUIParam colorParamStart = new ColorParam("ColorParam", Color.RED, true, true);
        GUIParam colorParamEnd = new ColorParam("ColorParam", Color.BLUE, true, true);

        return Arrays.asList(new Object[][]{
                {angleParamStart.copyState(), angleParamEnd.copyState()},
                {rangeParamStart.copyState(), rangeParamEnd.copyState()},
                {groupedRangeParamStart.copyState(), groupedRangeParamEnd.copyState()},
                {gradientParamStart.copyState(), gradientParamEnd.copyState()},
                {imagePositionParamStart.copyState(), imagePositionParamEnd.copyState()},
                {colorParamStart.copyState(), colorParamEnd.copyState()},
        });
    }

    @Test
    public void testInterpolate() {
        ParamState interpolated = start.interpolate(end, 0.0);
        assertNotNull(interpolated);

        interpolated = start.interpolate(end, 0.5);
        assertNotNull(interpolated);

        interpolated = start.interpolate(end, 1.0);
        assertNotNull(interpolated);
    }
}
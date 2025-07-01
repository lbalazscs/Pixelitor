/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.gmic;

import pixelitor.filters.gui.GroupedColorsParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;

import java.awt.Color;
import java.io.Serial;
import java.util.List;

import static pixelitor.filters.gui.TransparencyMode.ALPHA_ENABLED;

public class Bokeh extends GMICFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String NAME = "Bokeh";

    private final RangeParam scales = new RangeParam("Number of Scales", 1, 3, 10);
    private final IntChoiceParam shape = new IntChoiceParam("Shape", new IntChoiceParam.Item[]{
        new IntChoiceParam.Item("Circular", 8),
        new IntChoiceParam.Item("Triangle", 0),
        new IntChoiceParam.Item("Square", 1),
        new IntChoiceParam.Item("Diamond", 2),
        new IntChoiceParam.Item("Pentagon", 3),
        new IntChoiceParam.Item("Hexagon", 4),
        new IntChoiceParam.Item("Octogon", 5),
        new IntChoiceParam.Item("Decagon", 6),
        new IntChoiceParam.Item("Star", 7),
    });

    private final GroupedColorsParam colors = new GroupedColorsParam("Color",
        "Start", new Color(210, 210, 80, 160),
        "End", new Color(170, 130, 20, 110),
        ALPHA_ENABLED, true, false);

    private final GroupedRangeParam density = new GroupedRangeParam("Density",
        "Start", "End", 1, 30, 256, false);

    private final GroupedRangeParam radius = new GroupedRangeParam("Radius",
        new RangeParam[]{
            new RangeParam("Start", 0, 8, 50),
            new RangeParam("End", 0, 20, 50)
        }, false);

    private final GroupedRangeParam outline = new GroupedRangeParam("Outline",
        new RangeParam[]{
            new RangeParam("Start", 0, 4, 100),
            new RangeParam("End", 0, 20, 100)
        }, false);

    private final GroupedRangeParam innerShade = new GroupedRangeParam("Inner Shade",
        new RangeParam[]{
            new RangeParam("Start", 0, 30, 100),
            new RangeParam("End", 0, 100, 100)
        }, false);

    private final GroupedRangeParam smoothness = new GroupedRangeParam("Smoothness",
        new RangeParam[]{
            new RangeParam("Start", 0, 20, 800),
            new RangeParam("End", 0, 200, 800)
        }, false);

    private final GroupedRangeParam colorDispersion = new GroupedRangeParam("Color Dispersion",
        new RangeParam[]{
            new RangeParam("Start", 0, 70, 100),
            new RangeParam("End", 0, 15, 100)
        }, false);

    public Bokeh() {
        initParams(scales, shape, colors,
            density, radius, outline, innerShade,
            smoothness, colorDispersion).withReseedGmicAction(this);
    }

    @Override
    public List<String> getArgs() {
        return List.of("fx_bokeh",
            scales.getValue() + "," +
                shape.getValue() + "," +
                seed + "," +

                density.getValue(0) + "," +
                radius.getValue(0) + "," +
                outline.getValue(0) + "," +
                innerShade.getPercentage(0) + "," +
                smoothness.getPercentage(0) + "," +
                colors.getColorStr(0) + "," +
                colorDispersion.getPercentage(0) + "," +

                density.getValue(1) + "," +
                radius.getValue(1) + "," +
                outline.getValue(1) + "," +
                innerShade.getPercentage(1) + "," +
                smoothness.getPercentage(1) + "," +
                colors.getColorStr(1) + "," +
                colorDispersion.getPercentage(1));
    }
}

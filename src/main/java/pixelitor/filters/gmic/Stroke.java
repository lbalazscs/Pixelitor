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

import pixelitor.colors.Colors;
import pixelitor.filters.gui.GroupedColorsParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;

import java.awt.Color;
import java.io.Serial;
import java.util.List;

import static pixelitor.filters.gui.TransparencyMode.ALPHA_ENABLED;
import static pixelitor.filters.gui.TransparencyMode.MANUAL_ALPHA_ONLY;

public class Stroke extends GMICFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String NAME = "Stroke";

    private final RangeParam thickness = new RangeParam("Thickness", 1, 3, 256);
    private final RangeParam threshold = new RangeParam("Threshold", 0, 50, 100);
    private final RangeParam smoothness = new RangeParam("Smoothness", 0, 0, 10);
    private final IntChoiceParam shape = new IntChoiceParam("Shape", new IntChoiceParam.Item[]{
        new IntChoiceParam.Item("Round", 2),
        new IntChoiceParam.Item("Square", 0),
        new IntChoiceParam.Item("Diamond", 1)
    });
    private final IntChoiceParam direction = new IntChoiceParam("Direction", new IntChoiceParam.Item[]{
        new IntChoiceParam.Item("Outward", 1),
        new IntChoiceParam.Item("Inward", 0)
    });
    private final RangeParam zoom = new RangeParam("Zoom", 1, 100, 300);
    private final GroupedRangeParam shift = new GroupedRangeParam("Shift",
        -256, 0, 256, false);
    private final GroupedColorsParam strokeColor = new GroupedColorsParam("Stroke Color",
        "Start", Color.WHITE,
        "End", Color.WHITE,
        MANUAL_ALPHA_ONLY, true, true);
    private final GroupedColorsParam fillColor = new GroupedColorsParam("Fill Color",
        "Inside", Colors.TRANSPARENT_BLACK,
        "Outside", Colors.TRANSPARENT_BLACK,
        ALPHA_ENABLED, true, true);

    public Stroke() {
        initParams(thickness,
            threshold, smoothness, shape,
            strokeColor, fillColor,
            direction, zoom, shift.notLinkable());
    }

    @Override
    public List<String> getArgs() {
        return List.of("fx_stroke",
            thickness.getValue() + "," +
                threshold.getValue() + "," +
                smoothness.getValue() + "," +
                shape.getValue() + "," +
                direction.getValue() + "," +
                zoom.getValue() + "," +
                shift.getValue(0) + "," +
                shift.getValue(1) + "," +
                strokeColor.getColorStr(0) + "," +
                strokeColor.getColorStr(1) + "," +
                fillColor.getColorStr(0) + "," +
                fillColor.getColorStr(1) + ",0,1", "blend", "alpha"
        );
    }
}

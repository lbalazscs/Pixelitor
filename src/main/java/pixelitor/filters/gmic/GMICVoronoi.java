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

package pixelitor.filters.gmic;

import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;

import java.awt.Color;
import java.io.Serial;
import java.util.List;

public class GMICVoronoi extends GMICFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String NAME = "Voronoi";

    private final RangeParam threshold = new RangeParam("Threshold", 0, 160, 255);
    private final IntChoiceParam thresholdOn = new IntChoiceParam("Threshold On", new Item[]{
        new Item("Gradient values", 1),
        new Item("Pixel values", 0)
    });
    private final RangeParam smoothness = new RangeParam("Smoothness", 0, 50, 1000);
    private final RangeParam subsampling = new RangeParam("Subsampling", 0, 50, 100);
    private final IntChoiceParam flatColor = new IntChoiceParam("Flat Color", new Item[]{
        new Item("Image", 3),
        new Item("Black", 0),
        new Item("White", 1),
        new Item("Transparent", 2),
    });
    private final RangeParam outlineThickness = new RangeParam("Outline Thickness", 0, 1, 8);
    private final ColorParam outlineColor = new ColorParam("Outline Color", new Color(0, 0, 0, 100));
    private final RangeParam centersRadius = new RangeParam("Centers Radius", 0, 2, 10);
    private final ColorParam centersColor = new ColorParam("Centers Color", new Color(255, 255, 255, 40));
    private final IntChoiceParam antiAliasing = new IntChoiceParam("Anti-aliasing", new Item[]{
        new Item("None", 0),
        new Item("x1.5", 1),
        new Item("x2", 2),
        new Item("x2.5", 3)
    }).withDefaultChoice(1);

    public GMICVoronoi() {
        setParams(
            threshold,
            thresholdOn,
            smoothness,
            subsampling,
            flatColor,
            outlineThickness,
            outlineColor,
            centersRadius,
            centersColor,
            antiAliasing
        ).withReseedGmicAction(this);
    }

    @Override
    public List<String> getArgs() {
        return List.of("srand", String.valueOf(seed), "fx_voronoi",
            threshold.getValue() + "," +
                thresholdOn.getValue() + "," +
                smoothness.getPercentage() + "," +
                subsampling.getValue() + "," +
                flatColor.getValue() + "," +
                outlineThickness.getValue() + "," +
                outlineColor.getColorStr() + "," +
                centersRadius.getValue() + "," +
                centersColor.getColorStr() + "," +
                antiAliasing.getValue()
        );
    }
}

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

package pixelitor.filters;

import pixelitor.GUIMode;
import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.layers.BlendingMode;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.test.RandomGUITest;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Serial;

import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import static pixelitor.filters.gui.TransparencyPolicy.FREE_TRANSPARENCY;

/**
 * A test {@link ParametrizedFilter} with all {@link FilterParam} objects
 */
public class ParamTest extends ParametrizedFilter {
    public static final String NAME = "Param Test";

    @Serial
    private static final long serialVersionUID = 7920135228910788174L;

    public ParamTest() {
        super(true);

        setParams(getTestParams());
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (GUIMode.isDevelopment() && !RandomGUITest.isRunning()) {
            System.out.println("ParamTest.transform CALLED");
        }

        return ImageUtils.copyImage(src);
    }

    public static FilterParam[] getTestParams() {
        float[] defaultThumbPositions = {0.0f, 0.5f, 1.0f};
        Color[] defaultValues = {BLACK, BLUE, WHITE};

        RangeParam a = new RangeParam("A", 0, 50, 100);
        RangeParam b = new RangeParam("B", 0, 25, 100);
        RangeParam c = new RangeParam("C", 0, 25, 100);

        return new FilterParam[]{
            new GradientParam("Colors", defaultThumbPositions, defaultValues),
            new RangeParam("RangeParam", 0, 50, 100),
            new GroupedRangeParam("Normalized Group", new RangeParam[]{
                a, b, c}, false).autoNormalized(),
            new RangeWithColorsParam(RED, BLUE, "RangeWithColorsParam", 0, 50, 100),
            new ImagePositionParam("ImagePositionParam"),
            new IntChoiceParam("IntChoiceParam", new Item[]{
                new Item("value 1", 1),
                new Item("value 2", 2),
            }),
            new ColorParam("ColorParam", WHITE, FREE_TRANSPARENCY),
            new GroupedColorsParam("GroupedColorsParam", "A", WHITE, "B", Color.YELLOW, FREE_TRANSPARENCY, true, false),
            new AngleParam("AngleParam", 0),
            new ElevationAngleParam("ElevationAngleParam", 0),
            new BlendingModeParam(BlendingMode.values()),
            new BooleanParam("BooleanParam", false),
            new TextParam("TextParam", "default value", true),
            new LogZoomParam("Zoom", 200, 200, 1000),
        };
    }
}
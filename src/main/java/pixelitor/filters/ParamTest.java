/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.AppContext;
import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.layers.BlendingMode;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.test.RandomGUITest;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static java.awt.Color.*;
import static pixelitor.filters.gui.ColorParam.TransparencyPolicy.FREE_TRANSPARENCY;

/**
 * A test {@link ParametrizedFilter} with all {@link FilterParam} objects
 */
public class ParamTest extends ParametrizedFilter {
    public ParamTest() {
        super(ShowOriginal.YES);

        setParams(getTestParams());
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (AppContext.isDevelopment() && !RandomGUITest.isRunning()) {
            System.out.println("ParamTest.doTransform CALLED");
        }

        dest = ImageUtils.copyImage(src);
        return dest;
    }

    public static FilterParam[] getTestParams() {
        float[] defaultThumbPositions = {0.0f, 0.5f, 1.0f};
        Color[] defaultValues = {BLACK, BLUE, WHITE};

        return new FilterParam[]{
            new GradientParam("Colors", defaultThumbPositions, defaultValues),
            new RangeParam("RangeParam", 0, 50, 100),
            new RangeWithColorsParam(RED, BLUE, "RangeWithColorsParam", 0, 50, 100),
            new ImagePositionParam("ImagePositionParam"),
            new IntChoiceParam("IntChoiceParam", new Item[]{
                new Item("value 1", 1),
                new Item("value 2", 2),
            }),
            new ColorParam("ColorParam", WHITE, FREE_TRANSPARENCY),
            new AngleParam("AngleParam", 0),
            new ElevationAngleParam("ElevationAngleParam", 0),
            new BlendingModeParam(BlendingMode.values()),
            new BooleanParam("BooleanParam", false),
            new TextParam("TextParam", "default value"),
            new LogZoomParam("Zoom", 200, 200, 1000),
        };
    }
}
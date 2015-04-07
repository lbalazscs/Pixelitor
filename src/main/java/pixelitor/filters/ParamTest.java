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

package pixelitor.filters;

import pixelitor.Build;
import pixelitor.filters.gui.ActionParam;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.BlendingModeParam;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.ElevationAngleParam;
import pixelitor.filters.gui.GUIParam;
import pixelitor.filters.gui.GradientParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.RangeWithColorsParam;
import pixelitor.filters.gui.TextParam;
import pixelitor.layers.BlendingMode;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * A test operation with all GUIParam objects
 */
public class ParamTest extends FilterWithParametrizedGUI {
    public ParamTest() {
        super("ParamTest", true, false);
        setParamSet(new ParamSet(getTestParams()));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if ((Build.CURRENT == Build.DEVELOPMENT) && (!Build.CURRENT.isRobotTest()) && (!Build.CURRENT.isPerformanceTest())) {
            System.out.println("ParamTest.doTransform CALLED");
        }

//        Thread.dumpStack();

        dest = ImageUtils.copyImage(src);
        return dest;
    }

    public static GUIParam[] getTestParams() {
        float[] defaultThumbPositions = {0.0f, 0.5f, 1.0f};
        Color[] defaultValues = {Color.BLACK, Color.BLUE, Color.WHITE};

        return new GUIParam[]{
                new GradientParam("Colors", defaultThumbPositions, defaultValues),
                new RangeParam("RangeParam", 0, 100, 50),
                new RangeWithColorsParam(Color.RED, Color.BLUE, "RangeWithColorsParam", 0, 100, 50),
                new ImagePositionParam("ImagePositionParam"),
                new IntChoiceParam("IntChoiceParam", new IntChoiceParam.Value[]{
                        new IntChoiceParam.Value("value 1", 1),
                        new IntChoiceParam.Value("value 2", 2),
                }),
                new ColorParam("ColorParam:", Color.WHITE, true, true),
                new ActionParam("ActionParam", e -> {
                }, "Tooltip"),
                new AngleParam("AngleParam", 0),
                new ElevationAngleParam("ElevationAngleParam", 0),
                new BlendingModeParam(BlendingMode.values()),
                new BooleanParam("BooleanParam", false),
                new TextParam("TextParam", "default value"),
        };
    }
}
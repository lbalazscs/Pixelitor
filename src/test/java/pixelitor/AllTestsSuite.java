/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

package pixelitor;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import pixelitor.compactions.MultiLayerEditTest;
import pixelitor.filters.RandomFilterSourceTest;
import pixelitor.filters.gui.BooleanParamTest;
import pixelitor.filters.gui.FilterParamTest;
import pixelitor.filters.gui.IntChoiceParamTest;
import pixelitor.filters.gui.ParamSetTest;
import pixelitor.filters.gui.ParamStateTest;
import pixelitor.filters.gui.RangeParamTest;
import pixelitor.filters.levels.LevelsTest;
import pixelitor.guides.GuidesTest;
import pixelitor.history.PixelitorUndoManagerTest;
import pixelitor.layers.ContentLayerTest;
import pixelitor.layers.ImageLayerTest;
import pixelitor.layers.LayerBlendingModesTest;
import pixelitor.layers.LayerTest;
import pixelitor.layers.TextLayerTest;
import pixelitor.tools.AbstractBrushToolTest;
import pixelitor.tools.crop.CompositionGuideTest;
import pixelitor.tools.gradient.GradientHandlesTest;
import pixelitor.tools.transform.TransformBoxTest;
import pixelitor.transform.TransformHelperTest;
import pixelitor.utils.ShapesTest;
import pixelitor.utils.TrackedIOTest;
import pixelitor.utils.UtilsTest;

/**
 * Allows the test cases to run from the command line.
 * For finding all files with unit tests, do
 * grep -Rl "@Test" . | xargs -L 1 basename | sed -e 's/java/class,/g' | sort
 * in the test directory
 *
 * See http://stackoverflow.com/questions/24510742/can-you-run-all-junit-tests-in-a-package-from-the-command-line-without-explicitl
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
// commented out because this test suite is intended to run with
// a test jar, but this test requires the test files to be
// unpacked on the file system
//        CompositionCreationTest.class,

// also it should not contain itself
//        AllTestsSuite.class,

        AbstractBrushToolTest.class,
        BooleanParamTest.class,
        CompositionIOTest.class,
        CompositionTest.class,
        ContentLayerTest.class,
        FilterParamTest.class,
        GradientHandlesTest.class,
        GuidesTest.class,
        ImageLayerTest.class,
        IntChoiceParamTest.class,
        LayerBlendingModesTest.class,
        LayerTest.class,
        LevelsTest.class,
        MultiLayerEditTest.class,
        ParamSetTest.class,
        ParamStateTest.class,
        PixelitorUndoManagerTest.class,
        RandomFilterSourceTest.class,
        RangeParamTest.class,
        CompositionGuideTest.class,
        ShapesTest.class,
        TextLayerTest.class,
        TrackedIOTest.class,
        TransformBoxTest.class,
        TransformHelperTest.class,
        UtilsTest.class,})
public class AllTestsSuite {
    // empty
}

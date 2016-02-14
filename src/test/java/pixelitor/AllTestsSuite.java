/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.filters.RandomFilterSourceTest;
import pixelitor.filters.comp.MultiLayerEditTest;
import pixelitor.filters.gui.BooleanParamTest;
import pixelitor.filters.gui.FilterParamTest;
import pixelitor.filters.gui.IntChoiceParamTest;
import pixelitor.filters.gui.ParamSetTest;
import pixelitor.filters.gui.ParamStateTest;
import pixelitor.filters.gui.RangeParamTest;
import pixelitor.filters.levels.LevelsTest;
import pixelitor.history.PixelitorUndoManagerTest;
import pixelitor.layers.ContentLayerTest;
import pixelitor.layers.ImageLayerTest;
import pixelitor.layers.LayerBlendingModesTest;
import pixelitor.layers.LayerTest;
import pixelitor.layers.TextLayerTest;
import pixelitor.tools.AbstractBrushToolTest;
import pixelitor.tools.ToolTest;

/**
 * Allows the test cases to run from the command line.
 *
 * See http://stackoverflow.com/questions/24510742/can-you-run-all-junit-tests-in-a-package-from-the-command-line-without-explicitl
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
// commented out because this test suite is intended to run with
// a test jar, but this test requires the test files to be
// unpacked on the file system
//        CompositionCreationTest.class,

        PixelitorUndoManagerTest.class,
        CompositionTest.class,
        MultiLayerEditTest.class,
        BooleanParamTest.class,
        FilterParamTest.class,
        IntChoiceParamTest.class,
        ParamSetTest.class,
        ParamStateTest.class,
        RangeParamTest.class,
        LevelsTest.class,
        RandomFilterSourceTest.class,
        ContentLayerTest.class,
        ImageLayerTest.class,
        LayerBlendingModesTest.class,
        LayerTest.class,
        TextLayerTest.class,
        ToolTest.class,
        AbstractBrushToolTest.class,
})
public class AllTestsSuite {
    // empty
}

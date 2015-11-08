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
import pixelitor.layers.ContentLayerTest;
import pixelitor.layers.ImageLayerTest;
import pixelitor.layers.LayerBlendingModesTest;
import pixelitor.layers.LayerTest;
import pixelitor.layers.TextLayerTest;
import pixelitor.tools.ToolTest;

/**
 * See http://stackoverflow.com/questions/24510742/can-you-run-all-junit-tests-in-a-package-from-the-command-line-without-explicitl
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
//        CompositionCreationTest.class,
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
})
public class AllTestsSuite {
    // empty
}

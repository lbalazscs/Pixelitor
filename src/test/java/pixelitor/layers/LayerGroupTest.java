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

package pixelitor.layers;

import org.junit.jupiter.api.*;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.history.History;

import java.util.ArrayList;
import java.util.List;

import static pixelitor.assertions.PixelitorAssertions.assertThat;

@DisplayName("layer group tests")
@TestMethodOrder(MethodOrderer.Random.class)
class LayerGroupTest {
    private Composition comp;
    private LayerGroup group;
    private Layer layer1, layer2, layer3, layer4;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        // create a compositon with two layers and no masks
        comp = TestHelper.createComp("LayerGroupTest", 2, false);
        layer1 = comp.getLayer(0); // "layer 1"
        layer2 = comp.getLayer(1); // "layer 2", the active layer by default

        assertThat(comp)
            .numLayersIs(2)
            .layerNamesAre("layer 1", "layer 2")
            .activeLayerIs(layer2)
            .invariantsAreOK();

        // create a group with two layers
        layer3 = TestHelper.createEmptyImageLayer(comp, "layer 3");
        layer4 = TestHelper.createEmptyImageLayer(comp, "layer 4");

        String groupName = "Test Group";
        group = new LayerGroup(comp, groupName, List.of(layer3, layer4));
        assertThat(layer3).holderIs(group);
        assertThat(layer4).holderIs(group);

        assertThat(group)
            .nameIs(groupName)
            .opacityIs(1.0f)
            .isVisible()
            .blendingModeIs(BlendingMode.PASS_THROUGH)
            .isPassThrough()
            .hasNumLayers(2)
            .layersAre(layer3, layer4)
            .isNotRasterizable()
            .isNotConvertibleToSmartObject();

        comp.addLayerWithoutUI(group);
        group.activate();

        assertThat(comp)
            .numLayersIs(3)
            .layerNamesAre("layer 1", "layer 2", groupName)
            .activeLayerIs(group)
            .invariantsAreOK();

        assertThat(group).holderIs(comp);

        History.clear();
    }

    @Test
    @DisplayName("Set blending mode changes pass-through status")
    void testSetBlendingMode() {
        // change to Normal (non-pass-through)
        group.setBlendingMode(BlendingMode.NORMAL, true, true);
        TestHelper.assertHistoryEditsAre("Blending Mode Change");
        assertThat(group)
            .blendingModeIs(BlendingMode.NORMAL)
            .isNotPassThrough()
            .isRasterizable() // should now be rasterizable
            .isConvertibleToSmartObject(); // should now be convertible

        // change back to Pass Through
        History.undo("Blending Mode Change");
        assertThat(group)
            .blendingModeIs(BlendingMode.PASS_THROUGH)
            .isPassThrough()
            .isNotRasterizable()
            .isNotConvertibleToSmartObject();

        History.redo("Blending Mode Change");
        assertThat(group)
            .blendingModeIs(BlendingMode.NORMAL)
            .isNotPassThrough()
            .isRasterizable()
            .isConvertibleToSmartObject();
    }

    @Test
    @DisplayName("Add layer increases layer count")
    void testAddLayer() {
        Layer layer5 = TestHelper.createEmptyImageLayer(comp, "layer 5");

        group.addWithHistory(layer5, "Add Layer");
        assertThat(group)
            .hasNumLayers(3)
            .layersAre(layer3, layer4, layer5)
            .listContainsLayer(layer5);
        assertThat(layer5).holderIs(group);
        TestHelper.assertHistoryEditsAre("Add Layer");

        History.undo("Add Layer");
        assertThat(group)
            .hasNumLayers(2)
            .layersAre(layer3, layer4);

        History.redo("Add Layer");
        assertThat(group)
            .hasNumLayers(3)
            .layersAre(layer3, layer4, layer5);
    }

    @Test
    @DisplayName("Delete first layer in group updates active layer")
    void testDeleteFirstLayer() {
        // precondition: group has layer3, layer4. Group is active.
        assertThat(group).layersAre(layer3, layer4);
        assertThat(comp).activeLayerIs(group);

        // activate the first layer inside the group
        layer3.activate();
        assertThat(comp).activeLayerIs(layer3);

        // delete the first layer (layer3)
        group.deleteLayer(layer3, true);
        TestHelper.assertHistoryEditsAre("Delete " + layer3.getName());
        assertThat(group)
            .hasNumLayers(1)
            .layersAre(layer4);
        assertThat(comp)
            .activeLayerIs(layer4)
            .invariantsAreOK();

        History.undo("Delete " + layer3.getName());
        assertThat(group)
            .hasNumLayers(2)
            .layersAre(layer3, layer4);
        assertThat(comp)
            .activeLayerIs(layer3) // active layer restored (it was active before delete)
            .invariantsAreOK();

        History.redo("Delete " + layer3.getName());
        assertThat(group)
            .hasNumLayers(1)
            .layersAre(layer4);
        assertThat(comp)
            .activeLayerIs(layer4)
            .invariantsAreOK();
    }

    @Test
    @DisplayName("Delete last layer in group updates active layer")
    void testDeleteLastLayer() {
        // precondition: group has layer3, layer4. Group is active.
        assertThat(group).layersAre(layer3, layer4);
        assertThat(comp).activeLayerIs(group);

        // activate the last layer inside the group
        layer4.activate();
        assertThat(comp).activeLayerIs(layer4);

        // delete the last layer (layer4)
        group.deleteLayer(layer4, true);
        TestHelper.assertHistoryEditsAre("Delete " + layer4.getName());
        assertThat(group)
            .hasNumLayers(1)
            .layersAre(layer3);
        assertThat(comp)
            .activeLayerIs(layer3) // active layer should become the new last (previous) layer
            .invariantsAreOK();

        History.undo("Delete " + layer4.getName());
        assertThat(group)
            .hasNumLayers(2)
            .layersAre(layer3, layer4);
        assertThat(comp)
            .activeLayerIs(layer4) // active layer restored
            .invariantsAreOK();

        History.redo("Delete " + layer4.getName());
        assertThat(group)
            .hasNumLayers(1)
            .layersAre(layer3);
        assertThat(comp)
            .activeLayerIs(layer3)
            .invariantsAreOK();
    }

    @Test
    @DisplayName("Delete only layer in group makes group active")
    void testDeleteOnlyLayer() {
        // modify setup: create group with only one layer
        comp = TestHelper.createComp("SingleLayerGroupTest", 1, false); // Comp with layer1
        layer1 = comp.getLayer(0);
        layer3 = TestHelper.createEmptyImageLayer(comp, "layer 3");
        group = new LayerGroup(comp, "Single Layer Group", new ArrayList<>(List.of(layer3)));
        comp.addLayerWithoutUI(group);
        group.activate(); // activate group first
        layer3.activate(); // then activate the layer inside

        assertThat(comp).numLayersIs(2).layerNamesAre("layer 1", "Single Layer Group").activeLayerIs(layer3);
        assertThat(group).hasNumLayers(1).layersAre(layer3);
        History.clear();

        // Delete the only layer (layer3)
        group.deleteLayer(layer3, true);
        TestHelper.assertHistoryEditsAre("Delete " + layer3.getName());
        assertThat(group).isEmpty();
        assertThat(comp)
            .activeLayerIs(group) // active layer should become the group itself
            .invariantsAreOK();

        History.undo("Delete " + layer3.getName());
        assertThat(group)
            .hasNumLayers(1)
            .layersAre(layer3);
        assertThat(comp)
            .activeLayerIs(layer3) // active layer restored
            .invariantsAreOK();

        History.redo("Delete " + layer3.getName());
        assertThat(group).isEmpty();
        assertThat(comp)
            .activeLayerIs(group)
            .invariantsAreOK();
    }

    @Test
    @DisplayName("Ungroup moves layers to parent holder and preserves active layer")
    void testUngroup() {
        // precondition: comp has layer1, layer2, group(layer3, layer4). Group is active.
        int groupIndex = comp.indexOf(group);
        assertThat(groupIndex).isEqualTo(2);
        assertThat(comp)
            .numLayersIs(3)
            .layerNamesAre("layer 1", "layer 2", "Test Group");
        assertThat(group)
            .layersAre(layer3, layer4);

        // activate a layer inside the group
        layer4.activate();
        assertThat(comp).activeLayerIs(layer4);

        // ungroup
        group.unGroup();
        TestHelper.assertHistoryEditsAre("Ungrouping");
        assertThat(comp)
            .numLayersIs(4)
            .layerNamesAre("layer 1", "layer 2", "layer 3", "layer 4")
            .activeLayerIs(layer4) // active layer preserved
            .invariantsAreOK();
        assertThat(layer3).holderIs(comp);
        assertThat(layer4).holderIs(comp);

        History.undo("Ungrouping");
        assertThat(comp)
            .numLayersIs(3)
            .layerNamesAre("layer 1", "layer 2", "Test Group")
            .layersAre(layer1, layer2, group) // group is back at index 2
            .activeLayerIs(layer4) // still active (inside group)
            .invariantsAreOK();
        assertThat(group).layersAre(layer3, layer4);
        assertThat(layer3).holderIs(group);
        assertThat(layer4).holderIs(group);

        History.redo("Ungrouping");
        assertThat(comp)
            .numLayersIs(4)
            .layerNamesAre("layer 1", "layer 2", "layer 3", "layer 4")
            .activeLayerIs(layer4) // active layer preserved
            .invariantsAreOK();
        assertThat(layer3).holderIs(comp);
        assertThat(layer4).holderIs(comp);
    }

    @Test
    @DisplayName("Duplicate layer group copies group and its contents")
    void testDuplicateLayerGroup() {
        // precondition: comp has layer1, layer2, group(layer3, layer4). Group is active.
        assertThat(comp).numLayersIs(3).activeLayerIs(group);

        // duplicate the active layer (which is the group)
        comp.duplicateActiveLayer();
        TestHelper.assertHistoryEditsAre("Duplicate Layer");

        // assertions after duplicate
        assertThat(comp).numLayersIs(4);
        LayerGroup groupCopy = (LayerGroup) comp.getLayer(3); // added on top

        assertThat(groupCopy)
            .nameIs("Test Group copy")
            .hasNumLayers(2)
            .holderIs(comp)
            .opacityIs(group.getOpacity()) // properties copied
            .isVisible(group.isVisible())
            .blendingModeIs(group.getBlendingMode())
            .isActive(); // duplicate becomes active

        Layer layer3Copy = groupCopy.getLayer(0);
        Layer layer4Copy = groupCopy.getLayer(1);

        // check copied layers properties using custom assertions
        assertThat(layer3Copy)
            .isInstanceOf(ImageLayer.class)
            .nameIs("layer 3 copy")
            .holderIs(groupCopy);
        assertThat(layer4Copy)
            .isInstanceOf(ImageLayer.class)
            .nameIs("layer 4 copy")
            .holderIs(groupCopy);

        // overall composition state
        assertThat(comp)
            .layersAre(layer1, layer2, group, groupCopy)
            .activeLayerIs(groupCopy)
            .invariantsAreOK();

        History.undo("Duplicate Layer");
        assertThat(comp)
            .numLayersIs(3)
            .layerNamesAre("layer 1", "layer 2", "Test Group")
            .activeLayerIs(group) // original group active again
            .invariantsAreOK();

        History.redo("Duplicate Layer");
        assertThat(comp)
            .numLayersIs(4)
            .layerNamesAre("layer 1", "layer 2", "Test Group", "Test Group copy");

        // verify the duplicated group again after redo
        groupCopy = (LayerGroup) comp.getLayer(3);
        assertThat(comp)
            .activeLayerIs(groupCopy) // active again
            .invariantsAreOK();

        assertThat(groupCopy)
            .hasNumLayers(2)
            .nameIs("Test Group copy")
            .holderIs(comp);
        assertThat(groupCopy.getLayer(0))
            .nameIs("layer 3 copy")
            .holderIs(groupCopy);
        assertThat(groupCopy.getLayer(1))
            .nameIs("layer 4 copy")
            .holderIs(groupCopy);
    }
}

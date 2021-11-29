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

package pixelitor.layers;

import org.junit.jupiter.api.*;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.filters.Invert;
import pixelitor.filters.NoOpFilter;
import pixelitor.filters.OneColorFilter;
import pixelitor.filters.painters.TextSettings;

import java.awt.Color;
import java.awt.Font;

import static pixelitor.Composition.fromImage;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.layers.BlendingMode.*;
import static pixelitor.utils.ImageUtils.create1x1Image;

@DisplayName("Layer blending mode tests")
@TestMethodOrder(MethodOrderer.Random.class)
class LayerBlendingModesTest {
    private Composition comp;

    private ImageLayer lowerLayer;
    private Layer upperLayer;

    private final Color lowerColor = new Color(211, 141, 86);
    private final Color upperColor = new Color(119, 86, 132);

    private AdjustmentLayer invertAdjustment;
    private AdjustmentLayer alwaysUpperColorAdjustment;
    private TextLayer upperColorTextLayer;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        comp = fromImage(create1x1Image(lowerColor), null, "test");
        TestHelper.setupMockViewFor(comp);

        upperLayer = TestHelper.createImageLayer(
            comp, create1x1Image(upperColor), "Layer 2");
        comp.addLayerInInitMode(upperLayer);

        lowerLayer = (ImageLayer) comp.getLayer(0);
        lowerLayer.createUI();

        assert lowerLayer.getComp().classInvariant();
        assert lowerLayer.getComp() == upperLayer.getComp();
        assert upperLayer == comp.getActiveLayer();

        invertAdjustment = TestHelper.createAdjustmentLayer(
            comp, "Invert", new Invert());
        alwaysUpperColorAdjustment = TestHelper.createAdjustmentLayer(
            comp, "One Color", new OneColorFilter(upperColor));
        upperColorTextLayer = createTestTextLayerWithColor(upperColor);
    }

    @Test
    void normal() {
        testBlendingMode(NORMAL, upperColor);
    }

    @Test
    void darken() {
        // MIN(211, 119) = 119
        // MIN(141,  86) = 86
        // MIN(86,  132) = 86
        testBlendingMode(DARKEN, new Color(119, 86, 86));
    }

    @Test
    void multiply() {
        // 211 * 119 / 255 = 98
        // 141 *  86 / 255 = 48
        //  86 * 132 / 255 = 45
        testBlendingMode(MULTIPLY, new Color(98, 48, 45));
    }

    @Test
    void colorBurn() {
        testBlendingMode(COLOR_BURN, new Color(161, 0, 0));
    }

    @Test
    void lighten() {
        // MAX(211, 119) = 211
        // MAX(141,  86) = 141
        // MAX(86,  132)  = 132
        testBlendingMode(LIGHTEN, new Color(211, 141, 132));
    }

    @Test
    void screen() {
        // 255 - (255 - 211)(255 - 119)/255 = 232
        testBlendingMode(SCREEN, new Color(232, 179, 173));
    }

    @Test
    void colorDodge() {
        testBlendingMode(COLOR_DODGE, new Color(255, 213, 178));
    }

    @Test
    void linearDodge() {
        // 211 + 119 = 330 -> 255
        // 141 +  86 = 227
        //  86 + 132 = 218
        testBlendingMode(LINEAR_DODGE, new Color(255, 227, 218));
    }

    @Test
    void overlay() {
        // PS: 208, 104, 89
        testBlendingMode(OVERLAY, new Color(208, 102, 90));
    }

    @Test
    void softLight() {
        testBlendingMode(SOFT_LIGHT, new Color(209, 120, 88));
    }

    @Test
    void hardLight() {
        // PS: 197, 95, 91
        testBlendingMode(HARD_LIGHT, new Color(196, 96, 91));
    }

    @Test
    void difference() {
        // ABS(211 - 119) = 92
        // ABS(141 - 86)  = 55
        // ABS(86  - 132) = 46
        testBlendingMode(DIFFERENCE, new Color(92, 55, 46));
    }

    @Test
    void exclusion() {
        // PS: 134, 131, 128
        testBlendingMode(EXCLUSION, new Color(133, 132, 129));
    }

    @Test
    void hue() {
        // PS: 205, 115, 240
        testBlendingMode(HUE, new Color(176, 86, 211));
    }

    @Test
    void saturation() {
        // PS: 176, 150, 130
        testBlendingMode(SATURATION, new Color(211, 170, 137));
    }

    @Test
    void color() {
        // PS: 174, 141, 187
        testBlendingMode(COLOR, new Color(190, 137, 211));
    }

    @Test
    void luminosity() {
        // PS: 156, 86, 31
        testBlendingMode(LUMINOSITY, new Color(132, 88, 54));
    }

    private void testBlendingMode(BlendingMode blendingMode, Color expectedColor) {
        // check that the blending mode is working as expected
        upperLayer.setBlendingMode(blendingMode, true);
        assertThat(getResultingColor()).isEqualTo(expectedColor);

        // a white mask for the upper layer should change nothing
        upperLayer.addMask(LayerMaskAddType.REVEAL_ALL);
        assertThat(getResultingColor()).isEqualTo(expectedColor);

        // a white mask for the lower layer should change nothing
        lowerLayer.addMask(LayerMaskAddType.REVEAL_ALL);
        assertThat(getResultingColor()).isEqualTo(expectedColor);

        // upper layer with a black mask: expect lower color
        upperLayer.deleteMask(true);
        upperLayer.addMask(LayerMaskAddType.HIDE_ALL);
        assertThat(getResultingColor()).isEqualTo(lowerColor);
        upperLayer.deleteMask(true);

        // adding an invert adjustment should deliver the inverted color
        Color inverted = invert(expectedColor);
        comp.addLayerInInitMode(invertAdjustment);
        assertThat(comp).numLayersIs(3);
        assertThat(getResultingColor()).isEqualTo(inverted);

        // adding a white mask to the adjustment should change nothing
        invertAdjustment.addMask(LayerMaskAddType.REVEAL_ALL);
        assertThat(getResultingColor()).isEqualTo(inverted);

        // with a black mask, the adjustment should have no effect
        invertAdjustment.deleteMask(true);
        invertAdjustment.addMask(LayerMaskAddType.HIDE_ALL);
        assertThat(getResultingColor()).isEqualTo(expectedColor);

        // merging down the invert adjustment with black mask should have no effect
        comp.mergeActiveLayerDown();
        assertThat(getResultingColor()).isEqualTo(expectedColor);

        // adding a no-op adjustment layer should change nothing
        var noOpAdjustment = new AdjustmentLayer(comp, "No-op", new NoOpFilter());
        comp.addLayerInInitMode(noOpAdjustment);
        assertThat(getResultingColor()).isEqualTo(expectedColor);

        // merging down the no-op adjustment with black mask should have no effect
        comp.mergeActiveLayerDown();
        assertThat(getResultingColor()).isEqualTo(expectedColor);

        // delete the upper layer
        comp.deleteLayer(upperLayer, true);
        assertThat(comp).numLayersIs(1);

        // test the blending mode with an OneColorFilter that outputs the upper color
        comp.addLayerInInitMode(alwaysUpperColorAdjustment);
        alwaysUpperColorAdjustment.setBlendingMode(blendingMode, true);
        assertThat(getResultingColor()).isEqualTo(expectedColor);

        // adjustment layer with with white mask
        alwaysUpperColorAdjustment.addMask(LayerMaskAddType.REVEAL_ALL);
        assertThat(getResultingColor()).isEqualTo(expectedColor);

        // adjustment layer with with black mask, expect lower color
        alwaysUpperColorAdjustment.deleteMask(true);
        alwaysUpperColorAdjustment.addMask(LayerMaskAddType.HIDE_ALL);
        assertThat(getResultingColor()).isEqualTo(lowerColor);

        // merging down the adjustment with black mask should have no effect
        comp.mergeActiveLayerDown();
        assertThat(getResultingColor()).isEqualTo(lowerColor);
        assertThat(comp).numLayersIs(1);

        // test with text layer
        comp.addLayerInInitMode(upperColorTextLayer);
        upperColorTextLayer.setBlendingMode(blendingMode, true);
        assertThat(getResultingColor()).isEqualTo(expectedColor);

        // text layer with white mask
        upperColorTextLayer.addMask(LayerMaskAddType.REVEAL_ALL);
        assertThat(getResultingColor()).isEqualTo(expectedColor);

        // text layer with with black mask, expect lower color
        upperColorTextLayer.deleteMask(true);
        upperColorTextLayer.addMask(LayerMaskAddType.HIDE_ALL);
        assertThat(getResultingColor()).isEqualTo(lowerColor);

        // merging down the text layer with black mask should have no effect
        comp.mergeActiveLayerDown();
        assertThat(getResultingColor()).isEqualTo(lowerColor);
        assertThat(comp).numLayersIs(1);

        // merging down the upper layer should result in the expected color
        comp.addLayerInInitMode(upperLayer);
        assertThat(comp).numLayersIs(2);
        upperLayer.setBlendingMode(blendingMode, true);
        assertThat(getResultingColor()).isEqualTo(expectedColor);
        comp.mergeActiveLayerDown();
        assertThat(getResultingColor()).isEqualTo(expectedColor);
        assertThat(comp).numLayersIs(1);
    }

    private TextLayer createTestTextLayerWithColor(Color color) {
        var layer = new TextLayer(comp);
        layer.createUI();
        TextSettings settings = layer.getSettings();
        settings.setText("T"); // a huge T should cover everything
        settings.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 100));
        settings.setColor(color);
        layer.applySettings(settings);
        return layer;
    }

    private Color getResultingColor() {
        comp.invalidateCompositeCache();
        return new Color(comp.getCompositeImage().getRGB(0, 0));
    }

    private static Color invert(Color in) {
        return new Color(
            255 - in.getRed(),
            255 - in.getGreen(),
            255 - in.getBlue(),
            in.getAlpha()
        );
    }
}

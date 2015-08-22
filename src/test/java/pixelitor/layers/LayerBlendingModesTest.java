package pixelitor.layers;

import org.jdesktop.swingx.painter.AbstractLayoutPainter;
import org.junit.Before;
import org.junit.Test;
import pixelitor.Composition;
import pixelitor.filters.Invert;
import pixelitor.filters.NoOpFilter;
import pixelitor.filters.OneColorFilter;
import pixelitor.filters.painters.AreaEffects;
import pixelitor.filters.painters.TextSettings;
import pixelitor.history.AddToHistory;
import pixelitor.utils.UpdateGUI;

import java.awt.Color;
import java.awt.Font;

import static org.assertj.core.api.Assertions.assertThat;
import static pixelitor.Composition.fromImage;
import static pixelitor.utils.ImageUtils.create1x1Image;

public class LayerBlendingModesTest {
    private Composition comp;

    private ImageLayer lowerLayer;
    private Layer upperLayer;

    private final Color lowerColor = new Color(211, 141, 86);
    private final Color upperColor = new Color(119, 86, 132);

    private AdjustmentLayer invertAdjustment;
    private AdjustmentLayer alwaysUpperColorAdjustment;
    private TextLayer upperColorTextLayer;

    @Before
    public void setUp() {
        comp = fromImage(create1x1Image(lowerColor), null, "test");
        ImageLayer upperLayer = new ImageLayer(comp, create1x1Image(upperColor), "Layer 2", null);
        comp.addLayerNoGUI(upperLayer);

        lowerLayer = (ImageLayer) comp.getLayer(0);
        this.upperLayer = comp.getLayer(1);

        assert lowerLayer.getComp().checkInvariant();
        assert lowerLayer.getComp() == this.upperLayer.getComp();
        assert this.upperLayer == comp.getActiveLayer();

        invertAdjustment = new AdjustmentLayer(comp, "Invert", new Invert());
        alwaysUpperColorAdjustment = new AdjustmentLayer(comp, "One Color", new OneColorFilter(upperColor));
        upperColorTextLayer = createTestTextLayerWithColor(upperColor);
    }

    @Test
    public void testNormal() {
        testBlendingMode(BlendingMode.NORMAL, upperColor);
    }

    @Test
    public void testDarken() {
        // MIN(211, 119) = 119
        // MIN(141,  86) = 86
        // MIN(86,  132) = 86
        testBlendingMode(BlendingMode.DARKEN, new Color(119, 86, 86));
    }

    @Test
    public void testMultiply() {
        // 211 * 119 / 255 = 98
        // 141 *  86 / 255 = 48
        //  86 * 132 / 255 = 45
        testBlendingMode(BlendingMode.MULTIPLY, new Color(98, 48, 45));
    }

    @Test
    public void testColorBurn() {
        testBlendingMode(BlendingMode.COLOR_BURN, new Color(161, 0, 0));
    }

    @Test
    public void testLighten() {
        // MAX(211, 119) = 211
        // MAX(141,  86) = 141
        // MAX(86,  132)  = 132
        testBlendingMode(BlendingMode.LIGHTEN, new Color(211, 141, 132));
    }

    @Test
    public void testScreen() {
        // 255 - (255 - 211)(255 - 119)/255 = 232
        testBlendingMode(BlendingMode.SCREEN, new Color(232, 179, 173));
    }

    @Test
    public void testColorDodge() {
        testBlendingMode(BlendingMode.COLOR_DODGE, new Color(255, 213, 178));
    }

    @Test
    public void testLinearDodge() {
        // 211 + 119 = 330 -> 255
        // 141 +  86 = 227
        //  86 + 132 = 218
        testBlendingMode(BlendingMode.LINEAR_DODGE, new Color(255, 227, 218));
    }

    @Test
    public void testOverlay() {
        // PS: 208, 104, 89
        testBlendingMode(BlendingMode.OVERLAY, new Color(208, 102, 90));
    }

    @Test
    public void testSoftLight() {
        testBlendingMode(BlendingMode.SOFT_LIGHT, new Color(209, 120, 88));
    }

    @Test
    public void testHardLight() {
        // PS: 197, 95, 91
        testBlendingMode(BlendingMode.HARD_LIGHT, new Color(196, 96, 91));
    }

    @Test
    public void testDifference() {
        // ABS(211 - 119) = 92
        // ABS(141 - 86)  = 55
        // ABS(86  - 132) = 46
        testBlendingMode(BlendingMode.DIFFERENCE, new Color(92, 55, 46));
    }

    @Test
    public void testExclusion() {
        // PS: 134, 131, 128
        testBlendingMode(BlendingMode.EXCLUSION, new Color(134, 133, 130));
    }

    @Test
    public void testHue() {
        // PS: 205, 115, 240
        testBlendingMode(BlendingMode.HUE, new Color(176, 86, 211));
    }

    @Test
    public void testSaturation() {
        // PS: 176, 150, 130
        testBlendingMode(BlendingMode.SATURATION, new Color(211, 170, 137));
    }

    @Test
    public void testColor() {
        // PS: 174, 141, 187
        testBlendingMode(BlendingMode.COLOR, new Color(190, 137, 211));
    }

    @Test
    public void testLuminosity() {
        // PS: 156, 86, 31
        testBlendingMode(BlendingMode.LUMINOSITY, new Color(132, 88, 54));
    }

    private void testBlendingMode(BlendingMode blendingMode, Color expectedColor) {
        // check that the blending mode is working as expected
        upperLayer.setBlendingMode(blendingMode, UpdateGUI.NO, AddToHistory.YES, true);
        assertThat(getResultingColor()).isEqualTo(expectedColor);

        // a white mask for the upper layer should change nothing
        upperLayer.addMask(LayerMaskAddType.REVEAL_ALL);
        assertThat(getResultingColor()).isEqualTo(expectedColor);

        // a white mask for the lower layer should change nothing
        lowerLayer.addMask(LayerMaskAddType.REVEAL_ALL);
        assertThat(getResultingColor()).isEqualTo(expectedColor);

        // upper layer with a black mask: expect lower color
        upperLayer.deleteMask(AddToHistory.YES, false);
        upperLayer.addMask(LayerMaskAddType.HIDE_ALL);
        assertThat(getResultingColor()).isEqualTo(lowerColor);
        upperLayer.deleteMask(AddToHistory.YES, false);

        // adding an invert adjustment should deliver the inverted color
        Color inverted = invert(expectedColor);
        comp.addLayerNoGUI(invertAdjustment);
        assertThat(comp.getNrLayers()).isEqualTo(3);
        assertThat(getResultingColor()).isEqualTo(inverted);

        // adding a white mask to the adjustment should change nothing
        invertAdjustment.addMask(LayerMaskAddType.REVEAL_ALL);
        assertThat(getResultingColor()).isEqualTo(inverted);

        // with a black mask, the adjustment should have no effect
        invertAdjustment.deleteMask(AddToHistory.YES, false);
        invertAdjustment.addMask(LayerMaskAddType.HIDE_ALL);
        assertThat(getResultingColor()).isEqualTo(expectedColor);

        // merging down the invert adjustment with black mask should have no effect
        comp.mergeDown(UpdateGUI.NO);
        assertThat(getResultingColor()).isEqualTo(expectedColor);

        // adding a no-op adjustment layer should change nothing
        AdjustmentLayer noOpAdjustment = new AdjustmentLayer(comp, "No-op", new NoOpFilter());
        comp.addLayerNoGUI(noOpAdjustment);
        assertThat(getResultingColor()).isEqualTo(expectedColor);

        // merging down the no-op adjustment with black mask should have no effect
        comp.mergeDown(UpdateGUI.NO);
        assertThat(getResultingColor()).isEqualTo(expectedColor);

        // remove the upper layer
        comp.removeLayer(upperLayer, AddToHistory.YES, UpdateGUI.NO);
        assertThat(comp.getNrLayers()).isEqualTo(1);

        // test the blending mode with an OneColorFilter that outputs the upper color
        comp.addLayerNoGUI(alwaysUpperColorAdjustment);
        alwaysUpperColorAdjustment.setBlendingMode(blendingMode, UpdateGUI.NO, AddToHistory.YES, true);
        assertThat(getResultingColor()).isEqualTo(expectedColor);

        // adjustment layer with with white mask
        alwaysUpperColorAdjustment.addMask(LayerMaskAddType.REVEAL_ALL);
        assertThat(getResultingColor()).isEqualTo(expectedColor);

        // adjustment layer with with black mask, expect lower color
        alwaysUpperColorAdjustment.deleteMask(AddToHistory.YES, false);
        alwaysUpperColorAdjustment.addMask(LayerMaskAddType.HIDE_ALL);
        assertThat(getResultingColor()).isEqualTo(lowerColor);

        // merging down the adjustment with black mask should have no effect
        comp.mergeDown(UpdateGUI.NO);
        assertThat(getResultingColor()).isEqualTo(lowerColor);
        assertThat(comp.getNrLayers()).isEqualTo(1);

        // test with text layer
        comp.addLayerNoGUI(upperColorTextLayer);
        upperColorTextLayer.setBlendingMode(blendingMode, UpdateGUI.NO, AddToHistory.YES, true);
        assertThat(getResultingColor()).isEqualTo(expectedColor);

        // text layer with white mask
        upperColorTextLayer.addMask(LayerMaskAddType.REVEAL_ALL);
        assertThat(getResultingColor()).isEqualTo(expectedColor);

        // text layer with with black mask, expect lower color
        upperColorTextLayer.deleteMask(AddToHistory.YES, false);
        upperColorTextLayer.addMask(LayerMaskAddType.HIDE_ALL);
        assertThat(getResultingColor()).isEqualTo(lowerColor);

        // merging down the text layer with black mask should have no effect
        comp.mergeDown(UpdateGUI.NO);
        assertThat(getResultingColor()).isEqualTo(lowerColor);
        assertThat(comp.getNrLayers()).isEqualTo(1);

        // merging down the upper layer should result in the expected color
        comp.addLayerNoGUI(upperLayer);
        assertThat(comp.getNrLayers()).isEqualTo(2);
        upperLayer.setBlendingMode(blendingMode, UpdateGUI.NO, AddToHistory.YES, true);
        assertThat(getResultingColor()).isEqualTo(expectedColor);
        comp.mergeDown(UpdateGUI.NO);
        assertThat(getResultingColor()).isEqualTo(expectedColor);
        assertThat(comp.getNrLayers()).isEqualTo(1);
    }

    private TextLayer createTestTextLayerWithColor(Color color) {
        TextLayer layer = new TextLayer(comp);
        layer.setSettings(new TextSettings(
                "T", // a huge T should cover everything
                new Font(Font.SANS_SERIF, Font.BOLD, 100),
                color,
                new AreaEffects(),
                AbstractLayoutPainter.HorizontalAlignment.CENTER,
                AbstractLayoutPainter.VerticalAlignment.CENTER,
                false
        ));
        return layer;
    }

    private Color getResultingColor() {
        return new Color(comp.calculateCompositeImage().getRGB(0, 0));
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

package pixelitor.layers;

import org.junit.Before;
import org.junit.Test;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.history.AddToHistory;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

import static pixelitor.utils.Tests3x3.rgbToPackedInt;

public class LayerBlendingModesTest {
    private Composition comp;
    private ImageLayer bellowLayer;
    private Layer upperLayer;

    @Before
    public void setUp() {
        comp = TestHelper.createTwoLayer3x3Comp();

        bellowLayer = (ImageLayer) comp.getLayer(0);
        upperLayer = comp.getLayer(1);

        assert bellowLayer.getComp().checkInvariant();
        assert bellowLayer.getComp() == upperLayer.getComp();
        assert upperLayer == comp.getActiveLayer();
    }

    @Test
    public void testNormal() {
        BufferedImage result = comp.calculateCompositeImage();
        assert ImageUtils.compareSmallImages(result, getExpectedImageSingleLayer());
    }

    @Test
    public void testMultiply() {
        upperLayer.setBlendingMode(BlendingMode.MULTIPLY, false, AddToHistory.YES, true);
    }

    BufferedImage getExpectedImageSingleLayer() {
        BufferedImage img = ImageUtils.createCompatibleImage(3, 3);
        img.setRGB(0, 0, rgbToPackedInt(255, 255, 0, 0));
        img.setRGB(0, 1, rgbToPackedInt(255, 255, 255, 255));
        img.setRGB(0, 2, rgbToPackedInt(255, 255, 255, 0));
        img.setRGB(1, 0, rgbToPackedInt(255, 0, 255, 0));
        img.setRGB(1, 1, rgbToPackedInt(255, 128, 128, 128));
        img.setRGB(1, 2, rgbToPackedInt(255, 255, 0, 255));
        img.setRGB(2, 0, rgbToPackedInt(255, 0, 0, 255));
        img.setRGB(2, 1, rgbToPackedInt(255, 0, 0, 0));
        img.setRGB(2, 2, rgbToPackedInt(255, 0, 255, 255));
        return img;
    }
}

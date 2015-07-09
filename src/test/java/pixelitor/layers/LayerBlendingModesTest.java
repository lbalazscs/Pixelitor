package pixelitor.layers;

import org.junit.Before;
import org.junit.Test;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.history.AddToHistory;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Tests3x3;

import java.awt.image.BufferedImage;

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
        assert ImageUtils.compareSmallImages(result, Tests3x3.getStandardImage2());
    }

    @Test
    public void testMultiply() {
        upperLayer.setBlendingMode(BlendingMode.MULTIPLY, false, AddToHistory.YES, true);
    }
}

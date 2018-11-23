package pixelitor.guides;

import org.junit.Before;
import org.junit.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

public class GuideStyleTest {
    private GuideStyle guideStyle;

    @Before
    public void setUp() {
        guideStyle = new GuideStyle();
    }

    @Test
    public void testColorA() {
        guideStyle.setColorA(Color.MAGENTA);
        assertThat(guideStyle.getColorA()).isEqualTo(Color.MAGENTA);
    }

    @Test
    public void testColorB() {
        guideStyle.setColorB(Color.MAGENTA);
        assertThat(guideStyle.getColorB()).isEqualTo(Color.MAGENTA);
    }

    @Test
    public void testStrokeType() {
        guideStyle.setStrokeType(GuideStrokeType.SOLID);
        assertThat(guideStyle.getStrokeType()).isEqualTo(GuideStrokeType.SOLID);
    }

    @Test
    public void testStrokeGetters() {
        assertThat(guideStyle.getStrokeA()).isEqualTo(guideStyle.getStrokeType().getStrokeA());
        assertThat(guideStyle.getStrokeB()).isEqualTo(guideStyle.getStrokeType().getStrokeB());
    }
}

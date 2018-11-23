package pixelitor.guides;

import org.junit.Before;
import org.junit.Test;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class GuidesRendererTest {
    private GuideStyle guideStyle;
    private GuidesRenderer guidesRenderer;

    @Before
    public void setUp() {
        guideStyle = new GuideStyle();
        guidesRenderer = new GuidesRenderer(guideStyle);
    }

    @Test
    public void testDrawNone() {
        Graphics2D g2 = mock(Graphics2D.class);
        List<Shape> lines = new ArrayList<>();

        guidesRenderer.draw(g2, lines);
        verify(g2, times(0)).draw(any());
    }

    @Test
    public void testDrawInSingleStrokeMode() {
        Graphics2D g2 = mock(Graphics2D.class);
        List<Shape> lines = new ArrayList<>();
        lines.add(new Line2D.Double(1, 2, 3, 4));

        guideStyle.setStrokeType(GuideStrokeType.SOLID);
        guidesRenderer.draw(g2, lines);
        verify(g2, times(1)).setColor(guideStyle.getColorA());
        verify(g2, times(1)).setStroke(guideStyle.getStrokeA());
        verify(g2, times(1)).draw(lines.get(0));
    }

    @Test
    public void testDrawInDoubleStrokeMode() {
        Graphics2D g2 = mock(Graphics2D.class);
        List<Shape> lines = new ArrayList<>();
        lines.add(new Line2D.Double(1, 2, 3, 4));

        guideStyle.setStrokeType(GuideStrokeType.DASHED_DOUBLE);
        guidesRenderer.draw(g2, lines);
        verify(g2, times(1)).setColor(guideStyle.getColorA());
        verify(g2, times(1)).setStroke(guideStyle.getStrokeA());
        verify(g2, times(1)).setColor(guideStyle.getColorB());
        verify(g2, times(1)).setStroke(guideStyle.getStrokeB());
        verify(g2, times(2)).draw(lines.get(0));
    }
}

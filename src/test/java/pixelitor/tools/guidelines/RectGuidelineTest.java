package pixelitor.tools.guidelines;

import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

public class RectGuidelineTest {

    private RectGuideline rectGuideline;

    @Test
    public void draw_GuideLineType_NONE() {

        Rectangle2D rect = new Rectangle2D.Double(0, 0, 90, 30);
        Graphics2D g2 = Mockito.mock(Graphics2D.class);

        rectGuideline = new RectGuideline();
        rectGuideline.draw(rect, RectGuidelineType.NONE, g2);

        Mockito.verify(g2, Mockito.never()).draw(Mockito.any(Line2D.class));
    }


    @Test
    public void draw_Type_RULE_OF_THIRDS() {

        Rectangle2D rect = new Rectangle2D.Double(0, 0, 90, 12);
        Graphics2D g2 = Mockito.mock(Graphics2D.class);

        rectGuideline = new RectGuideline();
        rectGuideline.draw(rect, RectGuidelineType.RULE_OF_THIRDS, g2);

        Mockito.verify(g2, Mockito.times(2)).draw(Matchers.refEq(new Line2D.Double(30, 0, 30, 12)));
        Mockito.verify(g2, Mockito.times(2)).draw(Matchers.refEq(new Line2D.Double(60, 0, 60, 12)));
        Mockito.verify(g2, Mockito.times(2)).draw(Matchers.refEq(new Line2D.Double(0, 4, 90, 4)));
        Mockito.verify(g2, Mockito.times(2)).draw(Matchers.refEq(new Line2D.Double(0, 8, 90, 8)));
    }

    @Test
    public void draw_Type_GOLDEN_SECTIONS() {

        Rectangle2D rect = new Rectangle2D.Double(0, 0, 90, 12);
        Graphics2D g2 = Mockito.mock(Graphics2D.class);

        rectGuideline = new RectGuideline();
        rectGuideline.draw(rect, RectGuidelineType.GOLDEN_SECTIONS, g2);

        double phi = 1.618;
        double sectionWidth = rect.getWidth() / phi;
        double sectionHeight = rect.getHeight() / phi;

        Mockito.verify(g2, Mockito.times(2)).draw(Matchers.refEq(new Line2D.Double(sectionWidth, 0, sectionWidth, 12)));
        Mockito.verify(g2, Mockito.times(2)).draw(Matchers.refEq(new Line2D.Double(90-sectionWidth, 0, 90-sectionWidth, 12)));
        Mockito.verify(g2, Mockito.times(2)).draw(Matchers.refEq(new Line2D.Double(0, sectionHeight, 90, sectionHeight)));
        Mockito.verify(g2, Mockito.times(2)).draw(Matchers.refEq(new Line2D.Double(0, 12-sectionHeight, 90, 12-sectionHeight)));
    }

    @Test
    public void draw_Type_DIAGONALS_width_gt_height() {

        // rect orientation: width >= height
        Rectangle2D rect = new Rectangle2D.Double(0, 0, 90, 12);
        Graphics2D g2 = Mockito.mock(Graphics2D.class);

        rectGuideline = new RectGuideline();
        rectGuideline.draw(rect, RectGuidelineType.DIAGONALS, g2);

        Mockito.verify(g2, Mockito.times(2)).draw(Matchers.refEq(new Line2D.Double(0, 0, 12, 12)));
        Mockito.verify(g2, Mockito.times(2)).draw(Matchers.refEq(new Line2D.Double(0, 12, 12, 0)));
        Mockito.verify(g2, Mockito.times(2)).draw(Matchers.refEq(new Line2D.Double(90, 0, 90-12, 12)));
        Mockito.verify(g2, Mockito.times(2)).draw(Matchers.refEq(new Line2D.Double(90, 12, 90-12, 0)));
    }

    @Test
    public void draw_Type_DIAGONALS_height_gt_width() {

        // rect orientation: height > width
        Rectangle2D rect = new Rectangle2D.Double(0, 0, 12, 90);
        Graphics2D g2 = Mockito.mock(Graphics2D.class);

        rectGuideline = new RectGuideline();
        rectGuideline.draw(rect, RectGuidelineType.DIAGONALS, g2);

        Mockito.verify(g2, Mockito.times(2)).draw(Matchers.refEq(new Line2D.Double(0, 0, 12, 12)));
        Mockito.verify(g2, Mockito.times(2)).draw(Matchers.refEq(new Line2D.Double(0, 12, 12, 0)));
        Mockito.verify(g2, Mockito.times(2)).draw(Matchers.refEq(new Line2D.Double(0, 90, 12, 90-12)));
        Mockito.verify(g2, Mockito.times(2)).draw(Matchers.refEq(new Line2D.Double(0, 90-12, 12, 90)));
    }
}
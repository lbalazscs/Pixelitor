package pixelitor.utils;

import org.junit.Test;
import java.awt.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests utility helpers
 */
public class UtilsTest {

    @Test
    public void toPositiveRect_fromRectangle_whenWidthHeightPositive() {
        Rectangle rect = new Rectangle(30, 40, 10, 20);
        Rectangle rectOut = Utils.toPositiveRect(rect);

        assertThat(rectOut).isEqualTo(rect);
    }

    @Test
    public void toPositiveRect_fromRectangle_whenWidthNegative() {
        Rectangle rect = new Rectangle(30, 40, -10, 20);
        Rectangle rectExcepted = new Rectangle(20, 40, 10, 20);

        Rectangle rectOut = Utils.toPositiveRect(rect);
        assertThat(rectOut).isEqualTo(rectExcepted);
    }

    @Test
    public void toPositiveRect_fromRectangle_whenHeightNegative() {
        Rectangle rect = new Rectangle(30, 40, 10, -20);
        Rectangle rectExcepted = new Rectangle(30, 20, 10, 20);

        Rectangle rectOut = Utils.toPositiveRect(rect);
        assertThat(rectOut).isEqualTo(rectExcepted);
    }

    @Test
    public void toPositiveRect_fromRectangle_whenWidthHeightNegative() {
        Rectangle rect = new Rectangle(30, 40, -10, -20);
        Rectangle rectExcepted = new Rectangle(20, 20, 10, 20);

        Rectangle rectOut = Utils.toPositiveRect(rect);
        assertThat(rectOut).isEqualTo(rectExcepted);
    }
}
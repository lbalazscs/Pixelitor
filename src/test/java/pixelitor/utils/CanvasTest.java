package pixelitor.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pixelitor.Canvas;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class CanvasTest {

    @Test
    void rectangleIntersectionCanvas() {
        Canvas canvas = new Canvas(100, 100);
        Rectangle rectangle = new Rectangle(50, 50, 100, 100);
        Rectangle result = canvas.intersect(rectangle);
        Assertions.assertEquals(50, result.x);
        Assertions.assertEquals(50, result.y);
        Assertions.assertEquals(50, result.width);
        Assertions.assertEquals(50, result.height);
    }

}

package pixelitor.transform;

import org.junit.Test;
import java.awt.*;

import static org.assertj.core.api.Assertions.assertThat;

public class TransformHelperTest {

    // --- test calcAspectRatio() -------------------------------

    @Test
    public void calcAspectRatio() {

        double aspectRatio;
        Rectangle rect;

        // width: 0
        rect = new Rectangle(30, 40, 0, 20);
        aspectRatio = TransformHelper.calcAspectRatio(rect);
        assertThat(aspectRatio).isEqualTo(0);

        // height: 0
        rect = new Rectangle(30, 40, 20, 0);
        aspectRatio = TransformHelper.calcAspectRatio(rect);
        assertThat(aspectRatio).isEqualTo(0);

        // width, height: 0
        rect = new Rectangle(30, 40, 0, 0);
        aspectRatio = TransformHelper.calcAspectRatio(rect);
        assertThat(aspectRatio).isEqualTo(0);

        // height > width
        rect = new Rectangle(30, 40, 10, 20);
        aspectRatio = TransformHelper.calcAspectRatio(rect);
        assertThat(aspectRatio).isEqualTo(0.5);

        // width > height
        rect = new Rectangle(30, 40, 20, 10);
        aspectRatio = TransformHelper.calcAspectRatio(rect);
        assertThat(aspectRatio).isEqualTo(2.0);

        // width == height
        rect = new Rectangle(30, 40, 20, 20);
        aspectRatio = TransformHelper.calcAspectRatio(rect);
        assertThat(aspectRatio).isEqualTo(1.0);
    }

    // --- test resize() -------------------------------

    @Test
    public void resize_by_north_handle() {
        // up by 20
        Rectangle rect = new Rectangle(30, 40, 10, 20);
        Rectangle rectExpected = new Rectangle(30, 20, 10, 40);
        Point endPoint = new Point(35, -20); // x can be any value

        TransformHelper.resize(rect, Cursor.N_RESIZE_CURSOR, endPoint);
        assertThat(rect).isEqualTo(rectExpected);

        // down by 20
        rect = new Rectangle(30, 40, 10, 20);
        rectExpected = new Rectangle(30, 60, 10, 0);
        endPoint = new Point(35, 20); // x can be any value

        TransformHelper.resize(rect, Cursor.N_RESIZE_CURSOR, endPoint);
        assertThat(rect).isEqualTo(rectExpected);
    }

    @Test
    public void resize_by_south_handle() {
        // up by 20
        Rectangle rect = new Rectangle(30, 40, 10, 20);
        Rectangle rectExpected = new Rectangle(30, 40, 10, 0);
        Point endPoint = new Point(45, -20); // x can be any value

        TransformHelper.resize(rect, Cursor.S_RESIZE_CURSOR, endPoint);
        assertThat(rect).isEqualTo(rectExpected);

        // down by 20
        rect = new Rectangle(30, 40, 10, 20);
        rectExpected = new Rectangle(30, 40, 10, 40);
        endPoint = new Point(45, 20); // x can be any value

        TransformHelper.resize(rect, Cursor.S_RESIZE_CURSOR, endPoint);
        assertThat(rect).isEqualTo(rectExpected);
    }

    @Test
    public void resize_by_west_handle() {
        // west by 20
        Rectangle rect = new Rectangle(30, 40, 10, 20);
        Rectangle rectExpected = new Rectangle(10, 40, 30, 20);
        Point endPoint = new Point(-20, 45); // y can be any value

        TransformHelper.resize(rect, Cursor.W_RESIZE_CURSOR, endPoint);
        assertThat(rect).isEqualTo(rectExpected);

        // east by 20
        rect = new Rectangle(30, 40, 10, 20);
        rectExpected = new Rectangle(50, 40, -10, 20);
        endPoint = new Point(20, 45); // y can be any value

        TransformHelper.resize(rect, Cursor.W_RESIZE_CURSOR, endPoint);
        assertThat(rect).isEqualTo(rectExpected);
    }

    @Test
    public void resize_by_east_handle() {
        // west by 20
        Rectangle rect = new Rectangle(30, 40, 10, 20);
        Rectangle rectExpected = new Rectangle(30, 40, -10, 20);
        Point moveOffset = new Point(-20, 45); // y can be any value

        TransformHelper.resize(rect, Cursor.E_RESIZE_CURSOR, moveOffset);
        assertThat(rect).isEqualTo(rectExpected);

        // east by 20
        rect = new Rectangle(30, 40, 10, 20);
        rectExpected = new Rectangle(30, 40, 30, 20);
        moveOffset = new Point(20, 45); // y can be any value

        TransformHelper.resize(rect, Cursor.E_RESIZE_CURSOR, moveOffset);
        assertThat(rect).isEqualTo(rectExpected);
    }

    @Test
    public void resize_by_north_east_handle() {
        // up xy by 20
        Rectangle rect = new Rectangle(30, 40, 10, 20);
        Rectangle rectExpected = new Rectangle(30, 20, 30, 40);
        Point moveOffset = new Point(20, -20);

        TransformHelper.resize(rect, Cursor.NE_RESIZE_CURSOR, moveOffset);
        assertThat(rect).isEqualTo(rectExpected);

        // down xy by 20
        rect = new Rectangle(30, 40, 10, 20);
        rectExpected = new Rectangle(30, 60, -10, 0);
        moveOffset = new Point(-20, 20);

        TransformHelper.resize(rect, Cursor.NE_RESIZE_CURSOR, moveOffset);
        assertThat(rect).isEqualTo(rectExpected);
    }

    @Test
    public void resize_by_north_west_handle() {
        // up xy by 20
        Rectangle rect = new Rectangle(30, 40, 10, 20);
        Rectangle rectExpected = new Rectangle(10, 20, 30, 40);
        Point endPoint = new Point(-20, -20);

        TransformHelper.resize(rect, Cursor.NW_RESIZE_CURSOR, endPoint);
        assertThat(rect).isEqualTo(rectExpected);

        // down xy by 20
        rect = new Rectangle(30, 40, 10, 20);
        rectExpected = new Rectangle(50, 60, -10, 0);
        endPoint = new Point(20, 20);

        TransformHelper.resize(rect, Cursor.NW_RESIZE_CURSOR, endPoint);
        assertThat(rect).isEqualTo(rectExpected);
    }

    @Test
    public void resize_by_south_east_handle() {
        // up xy by 20
        Rectangle rect = new Rectangle(30, 40, 10, 20);
        Rectangle rectExpected = new Rectangle(30, 40, -10, 0);
        Point moveOffset = new Point(-20, -20);

        TransformHelper.resize(rect, Cursor.SE_RESIZE_CURSOR, moveOffset);
        assertThat(rect).isEqualTo(rectExpected);

        // down xy by 20
        rect = new Rectangle(30, 40, 10, 20);
        rectExpected = new Rectangle(30, 40, 30, 40);
        moveOffset = new Point(20, 20);

        TransformHelper.resize(rect, Cursor.SE_RESIZE_CURSOR, moveOffset);
        assertThat(rect).isEqualTo(rectExpected);
    }

    @Test
    public void resize_by_south_west_handle() {
        // up xy by 20
        Rectangle rect = new Rectangle(30, 40, 10, 20);
        Rectangle rectExpected = new Rectangle(50, 40, -10, 0);
        Point endPoint = new Point(20, -20);

        TransformHelper.resize(rect, Cursor.SW_RESIZE_CURSOR, endPoint);
        assertThat(rect).isEqualTo(rectExpected);

        // down xy by 20
        rect = new Rectangle(30, 40, 10, 20);
        rectExpected = new Rectangle(10, 40, 30, 40);
        endPoint = new Point(-20, 20);

        TransformHelper.resize(rect, Cursor.SW_RESIZE_CURSOR, endPoint);
        assertThat(rect).isEqualTo(rectExpected);
    }

    // --- test keepAspectRatio() -------------------------------

    @Test
    public void keepAspectRatio_by_north_or_south_handle() {
        Rectangle rect, rectExpected;

        // resize from 10x20 (aspectRatio: 0.5) to 10x40
        rect = new Rectangle(30, 40, 10, 40);
        rectExpected = new Rectangle(25, 40, 20, 40);

        TransformHelper.keepAspectRatio(rect, Cursor.N_RESIZE_CURSOR, 0.5);
        assertThat(rect).isEqualTo(rectExpected);

        TransformHelper.keepAspectRatio(rect, Cursor.S_RESIZE_CURSOR, 0.5);
        assertThat(rect).isEqualTo(rectExpected);
    }

    @Test
    public void keepAspectRatio_by_west_or_east_handle() {
        Rectangle rect, rectExpected;

        // resize from 10x20 (aspectRatio: 0.5) to 20x20
        rect = new Rectangle(30, 40, 20, 20);
        rectExpected = new Rectangle(30, 30, 20, 40);

        TransformHelper.keepAspectRatio(rect, Cursor.E_RESIZE_CURSOR, 0.5);
        assertThat(rect).isEqualTo(rectExpected);

        TransformHelper.keepAspectRatio(rect, Cursor.W_RESIZE_CURSOR, 0.5);
        assertThat(rect).isEqualTo(rectExpected);
    }

    @Test
    public void keepAspectRatio_by_north_east_handle() {
        Rectangle rect, rectExpected;

        // resize from 10x20 (aspectRatio: 0.5) to 20x20 (aspectRatio: 1.0) -> adjust height
        rect = new Rectangle(30, 40, 20, 20);
        rectExpected = new Rectangle(30, 20, 20, 40); // adjust: y, height

        TransformHelper.keepAspectRatio(rect, Cursor.NE_RESIZE_CURSOR, 0.5);
        assertThat(rect).isEqualTo(rectExpected);

        // resize from 10x20 (aspectRatio: 0.5) to 10x40 (aspectRatio: 0.25) -> adjust width
        rect = new Rectangle(30, 40, 10, 40);
        rectExpected = new Rectangle(30, 40, 20, 40); // adjust x, width

        TransformHelper.keepAspectRatio(rect, Cursor.NE_RESIZE_CURSOR, 0.5);
        assertThat(rect).isEqualTo(rectExpected);
    }

    @Test
    public void keepAspectRatio_by_north_west_handle() {
        Rectangle rect, rectExpected;

        // resize from 10x20 (aspectRatio: 0.5) to 20x20 (aspectRatio: 1.0) -> adjust height
        rect = new Rectangle(30, 40, 20, 20);
        rectExpected = new Rectangle(30, 20, 20, 40); // adjust: y, height

        TransformHelper.keepAspectRatio(rect, Cursor.NW_RESIZE_CURSOR, 0.5);
        assertThat(rect).isEqualTo(rectExpected);

        // resize from 10x20 (aspectRatio: 0.5) to 5x40 (aspectRatio: 0.125) -> adjust width
        rect = new Rectangle(30, 40, 5, 40);
        rectExpected = new Rectangle(15, 40, 20, 40); // adjust: x, width

        TransformHelper.keepAspectRatio(rect, Cursor.NW_RESIZE_CURSOR, 0.5);
        assertThat(rect).isEqualTo(rectExpected);
    }
}
/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.crop;

import org.junit.jupiter.api.*;
import pixelitor.TestHelper;

import java.awt.*;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CropBox tests")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@TestMethodOrder(MethodOrderer.Random.class)
class CropBoxTest {
    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @Test
    void aspectRatio() {
        double aspectRatio;
        Rectangle rect;

        // width: 0
        rect = new Rectangle(30, 40, 0, 20);
        aspectRatio = CropBox.calcAspectRatio(rect);
        assertThat(aspectRatio).isEqualTo(0);

        // height: 0
        rect = new Rectangle(30, 40, 20, 0);
        aspectRatio = CropBox.calcAspectRatio(rect);
        assertThat(aspectRatio).isEqualTo(0);

        // width, height: 0
        rect = new Rectangle(30, 40, 0, 0);
        aspectRatio = CropBox.calcAspectRatio(rect);
        assertThat(aspectRatio).isEqualTo(0);

        // height > width
        rect = new Rectangle(30, 40, 10, 20);
        aspectRatio = CropBox.calcAspectRatio(rect);
        assertThat(aspectRatio).isEqualTo(0.5);

        // width > height
        rect = new Rectangle(30, 40, 20, 10);
        aspectRatio = CropBox.calcAspectRatio(rect);
        assertThat(aspectRatio).isEqualTo(2.0);

        // width == height
        rect = new Rectangle(30, 40, 20, 20);
        aspectRatio = CropBox.calcAspectRatio(rect);
        assertThat(aspectRatio).isEqualTo(1.0);
    }

    @Test
    void resize_by_north_handle() {
        // up by 20
        Rectangle rect = new Rectangle(30, 40, 10, 20);
        Rectangle expectedRect = new Rectangle(30, 20, 10, 40);
        Point endPoint = new Point(35, -20); // x can be any value

        CropBox.resize(rect, Cursor.N_RESIZE_CURSOR, endPoint);
        assertThat(rect).isEqualTo(expectedRect);

        // down by 20
        rect = new Rectangle(30, 40, 10, 20);
        expectedRect = new Rectangle(30, 60, 10, 0);
        endPoint = new Point(35, 20); // x can be any value

        CropBox.resize(rect, Cursor.N_RESIZE_CURSOR, endPoint);
        assertThat(rect).isEqualTo(expectedRect);
    }

    @Test
    void resize_by_south_handle() {
        // up by 20
        Rectangle rect = new Rectangle(30, 40, 10, 20);
        Rectangle expectedRect = new Rectangle(30, 40, 10, 0);
        Point endPoint = new Point(45, -20); // x can be any value

        CropBox.resize(rect, Cursor.S_RESIZE_CURSOR, endPoint);
        assertThat(rect).isEqualTo(expectedRect);

        // down by 20
        rect = new Rectangle(30, 40, 10, 20);
        expectedRect = new Rectangle(30, 40, 10, 40);
        endPoint = new Point(45, 20); // x can be any value

        CropBox.resize(rect, Cursor.S_RESIZE_CURSOR, endPoint);
        assertThat(rect).isEqualTo(expectedRect);
    }

    @Test
    void resize_by_west_handle() {
        // west by 20
        Rectangle rect = new Rectangle(30, 40, 10, 20);
        Rectangle expectedRect = new Rectangle(10, 40, 30, 20);
        Point endPoint = new Point(-20, 45); // y can be any value

        CropBox.resize(rect, Cursor.W_RESIZE_CURSOR, endPoint);
        assertThat(rect).isEqualTo(expectedRect);

        // east by 20
        rect = new Rectangle(30, 40, 10, 20);
        expectedRect = new Rectangle(50, 40, -10, 20);
        endPoint = new Point(20, 45); // y can be any value

        CropBox.resize(rect, Cursor.W_RESIZE_CURSOR, endPoint);
        assertThat(rect).isEqualTo(expectedRect);
    }

    @Test
    void resize_by_east_handle() {
        // west by 20
        Rectangle rect = new Rectangle(30, 40, 10, 20);
        Rectangle expectedRect = new Rectangle(30, 40, -10, 20);
        Point moveOffset = new Point(-20, 45); // y can be any value

        CropBox.resize(rect, Cursor.E_RESIZE_CURSOR, moveOffset);
        assertThat(rect).isEqualTo(expectedRect);

        // east by 20
        rect = new Rectangle(30, 40, 10, 20);
        expectedRect = new Rectangle(30, 40, 30, 20);
        moveOffset = new Point(20, 45); // y can be any value

        CropBox.resize(rect, Cursor.E_RESIZE_CURSOR, moveOffset);
        assertThat(rect).isEqualTo(expectedRect);
    }

    @Test
    void resize_by_north_east_handle() {
        // up xy by 20
        Rectangle rect = new Rectangle(30, 40, 10, 20);
        Rectangle expectedRect = new Rectangle(30, 20, 30, 40);
        Point moveOffset = new Point(20, -20);

        CropBox.resize(rect, Cursor.NE_RESIZE_CURSOR, moveOffset);
        assertThat(rect).isEqualTo(expectedRect);

        // down xy by 20
        rect = new Rectangle(30, 40, 10, 20);
        expectedRect = new Rectangle(30, 60, -10, 0);
        moveOffset = new Point(-20, 20);

        CropBox.resize(rect, Cursor.NE_RESIZE_CURSOR, moveOffset);
        assertThat(rect).isEqualTo(expectedRect);
    }

    @Test
    void resize_by_north_west_handle() {
        // up xy by 20
        Rectangle rect = new Rectangle(30, 40, 10, 20);
        Rectangle expectedRect = new Rectangle(10, 20, 30, 40);
        Point endPoint = new Point(-20, -20);

        CropBox.resize(rect, Cursor.NW_RESIZE_CURSOR, endPoint);
        assertThat(rect).isEqualTo(expectedRect);

        // down xy by 20
        rect = new Rectangle(30, 40, 10, 20);
        expectedRect = new Rectangle(50, 60, -10, 0);
        endPoint = new Point(20, 20);

        CropBox.resize(rect, Cursor.NW_RESIZE_CURSOR, endPoint);
        assertThat(rect).isEqualTo(expectedRect);
    }

    @Test
    void resize_by_south_east_handle() {
        // up xy by 20
        Rectangle rect = new Rectangle(30, 40, 10, 20);
        Rectangle expectedRect = new Rectangle(30, 40, -10, 0);
        Point moveOffset = new Point(-20, -20);

        CropBox.resize(rect, Cursor.SE_RESIZE_CURSOR, moveOffset);
        assertThat(rect).isEqualTo(expectedRect);

        // down xy by 20
        rect = new Rectangle(30, 40, 10, 20);
        expectedRect = new Rectangle(30, 40, 30, 40);
        moveOffset = new Point(20, 20);

        CropBox.resize(rect, Cursor.SE_RESIZE_CURSOR, moveOffset);
        assertThat(rect).isEqualTo(expectedRect);
    }

    @Test
    void resize_by_south_west_handle() {
        // up xy by 20
        Rectangle rect = new Rectangle(30, 40, 10, 20);
        Rectangle expectedRect = new Rectangle(50, 40, -10, 0);
        Point endPoint = new Point(20, -20);

        CropBox.resize(rect, Cursor.SW_RESIZE_CURSOR, endPoint);
        assertThat(rect).isEqualTo(expectedRect);

        // down xy by 20
        rect = new Rectangle(30, 40, 10, 20);
        expectedRect = new Rectangle(10, 40, 30, 40);
        endPoint = new Point(-20, 20);

        CropBox.resize(rect, Cursor.SW_RESIZE_CURSOR, endPoint);
        assertThat(rect).isEqualTo(expectedRect);
    }

    @Test
    void keepAspectRatio_by_north_or_south_handle() {
        Rectangle rect, expectedRect;

        // resize from 10x20 (aspectRatio: 0.5) to 10x40
        rect = new Rectangle(30, 40, 10, 40);
        expectedRect = new Rectangle(25, 40, 20, 40);

        keepAspectRatio(rect, Cursor.N_RESIZE_CURSOR);
        assertThat(rect).isEqualTo(expectedRect);

        keepAspectRatio(rect, Cursor.S_RESIZE_CURSOR);
        assertThat(rect).isEqualTo(expectedRect);
    }

    @Test
    void keepAspectRatio_by_west_or_east_handle() {
        Rectangle rect, expectedRect;

        // resize from 10x20 (aspectRatio: 0.5) to 20x20
        rect = new Rectangle(30, 40, 20, 20);
        expectedRect = new Rectangle(30, 30, 20, 40);

        keepAspectRatio(rect, Cursor.E_RESIZE_CURSOR);
        assertThat(rect).isEqualTo(expectedRect);

        keepAspectRatio(rect, Cursor.W_RESIZE_CURSOR);
        assertThat(rect).isEqualTo(expectedRect);
    }

    @Test
    void keepAspectRatio_by_north_east_handle() {
        Rectangle rect, expectedRect;

        // resize from 10x20 (aspectRatio: 0.5) to 20x20 (aspectRatio: 1.0) -> adjust height
        rect = new Rectangle(30, 40, 20, 20);
        expectedRect = new Rectangle(30, 20, 20, 40); // adjust: y, height

        keepAspectRatio(rect, Cursor.NE_RESIZE_CURSOR);
        assertThat(rect).isEqualTo(expectedRect);

        // resize from 10x20 (aspectRatio: 0.5) to 10x40 (aspectRatio: 0.25) -> adjust width
        rect = new Rectangle(30, 40, 10, 40);
        expectedRect = new Rectangle(30, 40, 20, 40); // adjust x, width

        keepAspectRatio(rect, Cursor.NE_RESIZE_CURSOR);
        assertThat(rect).isEqualTo(expectedRect);
    }

    @Test
    void keepAspectRatio_by_north_west_handle() {
        Rectangle rect, expectedRect;

        // resize from 10x20 (aspectRatio: 0.5) to 20x20 (aspectRatio: 1.0) -> adjust height
        rect = new Rectangle(30, 40, 20, 20);
        expectedRect = new Rectangle(30, 20, 20, 40); // adjust: y, height

        keepAspectRatio(rect, Cursor.NW_RESIZE_CURSOR);
        assertThat(rect).isEqualTo(expectedRect);

        // resize from 10x20 (aspectRatio: 0.5) to 5x40 (aspectRatio: 0.125) -> adjust width
        rect = new Rectangle(30, 40, 5, 40);
        expectedRect = new Rectangle(15, 40, 20, 40); // adjust: x, width

        keepAspectRatio(rect, Cursor.NW_RESIZE_CURSOR);
        assertThat(rect).isEqualTo(expectedRect);
    }

    private static void keepAspectRatio(Rectangle rect, int cursor) {
        CropBox.keepAspectRatio(rect, cursor, 0.5, null);
    }
}
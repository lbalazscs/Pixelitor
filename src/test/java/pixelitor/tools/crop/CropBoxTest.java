/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import pixelitor.TestHelper;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CropBox tests")
@TestMethodOrder(MethodOrderer.Random.class)
class CropBoxTest {
    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @DisplayName("aspect ratio tests")
    @ParameterizedTest(name = "aspect ratio for rect({0}x{1}) should be {2}")
    @CsvSource({
        // width, height, expected
        " 0,     20,      0.0", // width is 0
        "20,      0,      0.0", // height is 0
        " 0,      0,      0.0", // width and height are 0
        "10,     20,      0.5", // height > width
        "20,     10,      2.0", // width > height
        "20,     20,      1.0"  // width == height
    })
    void aspect_ratio(int width, int height, double expected) {
        double actual = CropBox.calcAspectRatio(new Rectangle(30, 40, width, height));
        assertThat(actual).isEqualTo(expected);
    }

    @DisplayName("resize by handle")
    @ParameterizedTest(name = "{0}")
    @MethodSource("resizeArguments")
    void resize_by_handle(String displayName, int cursor, Point moveOffset, Rectangle expectedRect) {
        Rectangle rect = new Rectangle(30, 40, 10, 20);
        CropBox.resize(rect, cursor, moveOffset);
        assertThat(rect).isEqualTo(expectedRect);
    }

    private static Stream<Arguments> resizeArguments() {
        return Stream.of(
            // arguments: displayName, cursor, moveOffset, expectedRect
            Arguments.of("North handle, move up", Cursor.N_RESIZE_CURSOR, new Point(0, -20), new Rectangle(30, 20, 10, 40)),
            Arguments.of("North handle, move down", Cursor.N_RESIZE_CURSOR, new Point(0, 20), new Rectangle(30, 60, 10, 0)),
            Arguments.of("South handle, move up", Cursor.S_RESIZE_CURSOR, new Point(0, -20), new Rectangle(30, 40, 10, 0)),
            Arguments.of("South handle, move down", Cursor.S_RESIZE_CURSOR, new Point(0, 20), new Rectangle(30, 40, 10, 40)),
            Arguments.of("West handle, move west", Cursor.W_RESIZE_CURSOR, new Point(-20, 0), new Rectangle(10, 40, 30, 20)),
            Arguments.of("West handle, move east", Cursor.W_RESIZE_CURSOR, new Point(20, 0), new Rectangle(50, 40, -10, 20)),
            Arguments.of("East handle, move west", Cursor.E_RESIZE_CURSOR, new Point(-20, 0), new Rectangle(30, 40, -10, 20)),
            Arguments.of("East handle, move east", Cursor.E_RESIZE_CURSOR, new Point(20, 0), new Rectangle(30, 40, 30, 20)),
            Arguments.of("North-East handle, move up-right", Cursor.NE_RESIZE_CURSOR, new Point(20, -20), new Rectangle(30, 20, 30, 40)),
            Arguments.of("North-East handle, move down-left", Cursor.NE_RESIZE_CURSOR, new Point(-20, 20), new Rectangle(30, 60, -10, 0)),
            Arguments.of("North-West handle, move up-left", Cursor.NW_RESIZE_CURSOR, new Point(-20, -20), new Rectangle(10, 20, 30, 40)),
            Arguments.of("North-West handle, move down-right", Cursor.NW_RESIZE_CURSOR, new Point(20, 20), new Rectangle(50, 60, -10, 0)),
            Arguments.of("South-East handle, move up-left", Cursor.SE_RESIZE_CURSOR, new Point(-20, -20), new Rectangle(30, 40, -10, 0)),
            Arguments.of("South-East handle, move down-right", Cursor.SE_RESIZE_CURSOR, new Point(20, 20), new Rectangle(30, 40, 30, 40)),
            Arguments.of("South-West handle, move up-right", Cursor.SW_RESIZE_CURSOR, new Point(20, -20), new Rectangle(50, 40, -10, 0)),
            Arguments.of("South-West handle, move down-left", Cursor.SW_RESIZE_CURSOR, new Point(-20, 20), new Rectangle(10, 40, 30, 40))
        );
    }

    /**
     * Tests that resizing maintains the correct aspect ratio.
     */
    @DisplayName("keep aspect ratio by handle")
    @ParameterizedTest(name = "{0}")
    @MethodSource("keepAspectRatioArguments")
    void keepAspectRatio_by_handle(String displayName, int cursor, Rectangle rect, Rectangle expectedRect) {
        // the test uses a fixed aspect ratio of 0.5
        CropBox.keepAspectRatio(rect, cursor, 0.5, null);
        assertThat(rect).isEqualTo(expectedRect);
    }

    private static Stream<Arguments> keepAspectRatioArguments() {
        return Stream.of(
            // arguments: displayName, cursor, initialRect, expectedRect
            Arguments.of("North handle", Cursor.N_RESIZE_CURSOR, new Rectangle(30, 40, 10, 40), new Rectangle(25, 40, 20, 40)),
            Arguments.of("South handle", Cursor.S_RESIZE_CURSOR, new Rectangle(30, 40, 10, 40), new Rectangle(25, 40, 20, 40)),
            Arguments.of("East handle", Cursor.E_RESIZE_CURSOR, new Rectangle(30, 40, 20, 20), new Rectangle(30, 30, 20, 40)),
            Arguments.of("West handle", Cursor.W_RESIZE_CURSOR, new Rectangle(30, 40, 20, 20), new Rectangle(30, 30, 20, 40)),
            Arguments.of("North-East handle, adjust height", Cursor.NE_RESIZE_CURSOR, new Rectangle(30, 40, 20, 20), new Rectangle(30, 20, 20, 40)),
            Arguments.of("North-East handle, adjust width", Cursor.NE_RESIZE_CURSOR, new Rectangle(30, 40, 10, 40), new Rectangle(30, 40, 20, 40)),
            Arguments.of("North-West handle, adjust height", Cursor.NW_RESIZE_CURSOR, new Rectangle(30, 40, 20, 20), new Rectangle(30, 20, 20, 40)),
            Arguments.of("North-West handle, adjust width", Cursor.NW_RESIZE_CURSOR, new Rectangle(30, 40, 5, 40), new Rectangle(15, 40, 20, 40))
        );
    }
}

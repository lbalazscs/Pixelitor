/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import org.junit.jupiter.params.provider.MethodSource;
import pixelitor.TestHelper;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.stream.Stream;

import static java.awt.Cursor.E_RESIZE_CURSOR;
import static java.awt.Cursor.NE_RESIZE_CURSOR;
import static java.awt.Cursor.NW_RESIZE_CURSOR;
import static java.awt.Cursor.N_RESIZE_CURSOR;
import static java.awt.Cursor.SE_RESIZE_CURSOR;
import static java.awt.Cursor.SW_RESIZE_CURSOR;
import static java.awt.Cursor.S_RESIZE_CURSOR;
import static java.awt.Cursor.W_RESIZE_CURSOR;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CropBox tests")
@TestMethodOrder(MethodOrderer.Random.class)
class CropBoxTest {
    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
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
            Arguments.of("N handle, move up", N_RESIZE_CURSOR, new Point(0, -20), new Rectangle(30, 20, 10, 40)),
            Arguments.of("N handle, move down", N_RESIZE_CURSOR, new Point(0, 20), new Rectangle(30, 60, 10, 0)),
            Arguments.of("S handle, move up", S_RESIZE_CURSOR, new Point(0, -20), new Rectangle(30, 40, 10, 0)),
            Arguments.of("S handle, move down", S_RESIZE_CURSOR, new Point(0, 20), new Rectangle(30, 40, 10, 40)),
            Arguments.of("W handle, move west", W_RESIZE_CURSOR, new Point(-20, 0), new Rectangle(10, 40, 30, 20)),
            Arguments.of("W handle, move east", W_RESIZE_CURSOR, new Point(20, 0), new Rectangle(50, 40, -10, 20)),
            Arguments.of("E handle, move west", E_RESIZE_CURSOR, new Point(-20, 0), new Rectangle(30, 40, -10, 20)),
            Arguments.of("E handle, move east", E_RESIZE_CURSOR, new Point(20, 0), new Rectangle(30, 40, 30, 20)),
            Arguments.of("NE handle, move up-right", NE_RESIZE_CURSOR, new Point(20, -20), new Rectangle(30, 20, 30, 40)),
            Arguments.of("NE handle, move down-left", NE_RESIZE_CURSOR, new Point(-20, 20), new Rectangle(30, 60, -10, 0)),
            Arguments.of("NW handle, move up-left", NW_RESIZE_CURSOR, new Point(-20, -20), new Rectangle(10, 20, 30, 40)),
            Arguments.of("NW handle, move down-right", NW_RESIZE_CURSOR, new Point(20, 20), new Rectangle(50, 60, -10, 0)),
            Arguments.of("SE handle, move up-left", SE_RESIZE_CURSOR, new Point(-20, -20), new Rectangle(30, 40, -10, 0)),
            Arguments.of("SE handle, move down-right", SE_RESIZE_CURSOR, new Point(20, 20), new Rectangle(30, 40, 30, 40)),
            Arguments.of("SW handle, move up-right", SW_RESIZE_CURSOR, new Point(20, -20), new Rectangle(50, 40, -10, 0)),
            Arguments.of("SW handle, move down-left", SW_RESIZE_CURSOR, new Point(-20, 20), new Rectangle(10, 40, 30, 40))
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
            Arguments.of("N handle", N_RESIZE_CURSOR, new Rectangle(30, 40, 10, 40), new Rectangle(25, 40, 20, 40)),
            Arguments.of("S handle", S_RESIZE_CURSOR, new Rectangle(30, 40, 10, 40), new Rectangle(25, 40, 20, 40)),
            Arguments.of("E handle", E_RESIZE_CURSOR, new Rectangle(30, 40, 20, 20), new Rectangle(30, 30, 20, 40)),
            Arguments.of("W handle", W_RESIZE_CURSOR, new Rectangle(30, 40, 20, 20), new Rectangle(30, 30, 20, 40)),
            Arguments.of("NE handle, adjust height", NE_RESIZE_CURSOR, new Rectangle(30, 40, 20, 20), new Rectangle(30, 20, 20, 40)),
            Arguments.of("NE handle, adjust width", NE_RESIZE_CURSOR, new Rectangle(30, 40, 10, 40), new Rectangle(30, 40, 20, 40)),
            Arguments.of("NW handle, adjust height", NW_RESIZE_CURSOR, new Rectangle(30, 40, 20, 20), new Rectangle(30, 20, 20, 40)),
            Arguments.of("NW handle, adjust width", NW_RESIZE_CURSOR, new Rectangle(30, 40, 5, 40), new Rectangle(15, 40, 20, 40))
        );
    }
}

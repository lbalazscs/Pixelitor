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


package pixelitor.guides;

import org.junit.jupiter.api.*;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.compactions.FlipDirection;
import pixelitor.compactions.QuadrantAngle;
import pixelitor.gui.View;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Guides tests")
@TestMethodOrder(MethodOrderer.Random.class)
class GuidesTest {
    private Guides guides;
    private Composition comp;
    private View view;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode(true);
    }

    @BeforeEach
    void beforeEachTest() {
        comp = TestHelper.createEmptyComp("GuidesTest");
        guides = new Guides();
        view = comp.getView();
    }

    @AfterEach
    void afterEachTest() {
        TestHelper.verifyAndClearHistory();
    }

    @Test
    @DisplayName("horizontal line, relative")
    void addHorRelative() {
        guides.addHorRelative(0.25);
        assertThat(guides.getHorizontals()).containsOnly(0.25);
    }

    @Test
    @DisplayName("horizontal line, absolute")
    void addHorAbsolute() {
        guides.addHorAbsolute((int) (TestHelper.TEST_WIDTH * 0.75), comp.getCanvas());
        assertThat(guides.getHorizontals()).containsOnly(0.75);
    }

    @Test
    @DisplayName("vertical line, relative")
    void addVerRelative() {
        guides.addVerRelative(0.33);
        assertThat(guides.getVerticals()).containsOnly(0.33);
    }

    @Test
    @DisplayName("vertical line, absolute")
    void addVerAbsolute() {
        guides.addVerAbsolute((int) (TestHelper.TEST_HEIGHT * 0.5), comp.getCanvas());
        assertThat(guides.getVerticals()).containsOnly(0.5);
    }

    @Test
    @DisplayName("relative grid")
    void addRelativeGrid() {
        guides.addRelativeGrid(4, 4);
        assertThat(guides.getHorizontals()).containsOnly(0.25, 0.5, 0.75);
        assertThat(guides.getVerticals()).containsOnly(0.25, 0.5, 0.75);
    }

    @Test
    @DisplayName("flip horizontally")
    void copyFlippedHorizontally() {
        guides.addVerRelative(0.25);
        guides.addHorRelative(0.4);

        Guides flippedGuides = guides.copyFlipped(FlipDirection.HORIZONTAL, view);

        assertThat(flippedGuides.getVerticals()).containsOnly(0.75);
        assertThat(flippedGuides.getHorizontals()).containsOnly(0.4);
    }

    @Test
    @DisplayName("flip vertically")
    void copyFlippedVertically() {
        guides.addVerRelative(0.25);
        guides.addHorRelative(0.4);

        Guides flippedGuides = guides.copyFlipped(FlipDirection.VERTICAL, view);

        assertThat(flippedGuides.getVerticals()).containsOnly(0.25);
        assertThat(flippedGuides.getHorizontals()).containsOnly(0.6);
    }

    @Test
    @DisplayName("rotate 90 degrees")
    void copyRotated90() {
        guides.addHorRelative(0.2);
        guides.addVerRelative(0.4);

        Guides rotatedGuides = guides.copyRotated(QuadrantAngle.ANGLE_90, view);

        // horizontal at y becomes vertical at 1 - y
        // vertical at x becomes horizontal at x
        assertThat(rotatedGuides.getVerticals()).containsOnly(1.0 - 0.2);
        assertThat(rotatedGuides.getHorizontals()).containsOnly(0.4);
    }

    @Test
    @DisplayName("rotate 180 degrees")
    void copyRotated180() {
        guides.addHorRelative(0.2);
        guides.addVerRelative(0.4);

        Guides rotatedGuides = guides.copyRotated(QuadrantAngle.ANGLE_180, view);

        // horizontal at y becomes horizontal at 1 - y
        // vertical at x becomes vertical at 1 - x
        assertThat(rotatedGuides.getHorizontals()).containsOnly(1.0 - 0.2);
        assertThat(rotatedGuides.getVerticals()).containsOnly(1.0 - 0.4);
    }

    @Test
    @DisplayName("rotate 270 degrees")
    void copyRotated270() {
        guides.addHorRelative(0.2);
        guides.addVerRelative(0.4);

        Guides rotatedGuides = guides.copyRotated(QuadrantAngle.ANGLE_270, view);

        // horizontal at y becomes vertical at y
        // vertical at x becomes horizontal at 1 - x
        assertThat(rotatedGuides.getVerticals()).containsOnly(0.2);
        assertThat(rotatedGuides.getHorizontals()).containsOnly(1.0 - 0.4);
    }
}

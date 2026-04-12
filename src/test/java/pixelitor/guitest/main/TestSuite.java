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

package pixelitor.guitest.main;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Test suite configurations for the {@link MainGuiTest}.
 */
public enum TestSuite {
    ALL(MainGuiTest::testAll), // test everything
    TOOLS(MainGuiTest::testTools), // tools + Selection menu
    FILE(MainGuiTest::testFileMenu), // "File" menu except auto paint
    AUTO_PAINT(MainGuiTest::testAutoPaint),
    EDIT(MainGuiTest::testEditMenu), // "Edit" menu
    IMAGE(MainGuiTest::testImageMenu), // "Image" menu
    FILTERS(MainGuiTest::testFilters), // "Colors" and "Filters" menus
    LAYERS(MainGuiTest::testLayers), // "Layers" menu and layer buttons
    REST(MainGuiTest::testRest); // "View" and "Help" menus

    private final Consumer<TestContext> runner;

    TestSuite(Consumer<TestContext> runner) {
        this.runner = runner;
    }

    public void run(TestContext context) {
        runner.accept(context);
    }

    public static TestSuite load() {
        String property = System.getProperty("test.target");

        if (property == null || property.isBlank()) {
            return ALL; // default
        }

        try {
            return valueOf(property.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            String msg = "Target " + property + " not found.\n" +
                "Available targets: " + Arrays.toString(values());
            throw new IllegalArgumentException(msg, e);
        }
    }
}

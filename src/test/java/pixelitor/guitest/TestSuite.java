/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.guitest;

import java.util.Arrays;
import java.util.Locale;

/**
 * Test suite configurations for the {@link MainGuiTest}.
 */
public enum TestSuite {
    ALL { // Test everything
        @Override
        public void run(MainGuiTest tester) {
            tester.testAll();
        }
    }, TOOLS { // Tools + Selection menu
        @Override
        public void run(MainGuiTest tester) {
            tester.testTools();
        }
    }, FILE { // The "File" menu except auto paint
        @Override
        public void run(MainGuiTest tester) {
            tester.testFileMenu();
        }
    }, AUTO_PAINT {
        @Override
        public void run(MainGuiTest tester) {
            tester.testAutoPaint();
        }
    }, EDIT { // "Edit" menu
        @Override
        public void run(MainGuiTest tester) {
            tester.testEditMenu();
        }
    }, IMAGE { // "Image" menu
        @Override
        public void run(MainGuiTest tester) {
            tester.testImageMenu();
        }
    }, FILTERS { // "Colors" and "Filters" menus
        @Override
        public void run(MainGuiTest tester) {
            tester.testFilters();
        }
    }, LAYERS { // "Layers" menu and layer buttons
        @Override
        public void run(MainGuiTest tester) {
            tester.testLayers();
        }
    }, REST { // "View" and "Help" menus
        @Override
        public void run(MainGuiTest tester) {
            tester.testViewMenu();
            tester.testColors();
            tester.testHelpMenu();
        }
    };

    public abstract void run(MainGuiTest tester);

    public static TestSuite load() {
        String property = System.getProperty("test.target");

        if (property == null || property.trim().isEmpty()) {
            return ALL; // default
        }

        TestSuite suite = null;
        try {
            suite = valueOf(property.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            // fatal misconfiguration
            String msg = "Target " + property + " not found.\n" +
                "Available targets: " + Arrays.toString(values());
            System.err.println(msg);
            System.exit(1);
        }
        return suite;
    }
}

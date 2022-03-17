/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

/**
 * Test targets for the {@link AssertJSwingTest}.
 */
public enum TestTarget {
    ALL { // Test everything
        @Override
        public void run(AssertJSwingTest tester) {
            tester.testAll();
        }
    }, TOOLS { // Tools + Selection menus
        @Override
        public void run(AssertJSwingTest tester) {
            tester.testTools();
        }
    }, FILE { // The "File" menu except auto paint
        @Override
        public void run(AssertJSwingTest tester) {
            tester.testFileMenu();
        }
    }, AUTOPAINT {
        @Override
        public void run(AssertJSwingTest tester) {
            tester.testAutoPaint();
        }
    }, EDIT { // "Edit" menus
        @Override
        public void run(AssertJSwingTest tester) {
            tester.testEditMenu();
        }
    }, IMAGE { // "Image" menus
        @Override
        public void run(AssertJSwingTest tester) {
            tester.testImageMenu();
        }
    }, FILTERS { // "Colors" and "Filters" menus
        @Override
        public void run(AssertJSwingTest tester) {
            tester.testFilters();
        }
    }, LAYERS { // "Layers" menus and layer buttons
        @Override
        public void run(AssertJSwingTest tester) {
            tester.testLayers();
        }
    }, REST { // "View" and "Help" menus
        @Override
        public void run(AssertJSwingTest tester) {
            tester.testViewMenu();
            tester.testColors();
            tester.testHelpMenu();
        }
    };

    public abstract void run(AssertJSwingTest tester);
}

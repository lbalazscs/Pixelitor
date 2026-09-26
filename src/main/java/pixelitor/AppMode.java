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

package pixelitor;

import pixelitor.utils.Utils;

/**
 * The execution mode of the application (standard GUI,
 * development mode, or headless unit testing).
 */
public enum AppMode {
    /**
     * The mode used by end-users.
     */
    STANDARD_GUI,
    /**
     * In this mode there are additional development menus and runtime checks.
     */
    DEVELOPMENT_GUI,
    /**
     * In this mode there is no GUI, and some objects might be mocked.
     */
    UNIT_TESTS;

    private static AppMode activeMode = STANDARD_GUI;

    /**
     * Returns true if the app was started in development mode.
     * In this mode, additional menus and correctness checks are enabled.
     */
    public static boolean isDevelopment() {
        return activeMode == DEVELOPMENT_GUI;
    }

    public static boolean isUnitTesting() {
        return activeMode == UNIT_TESTS;
    }

    public static void detectDevMode() {
        // the app can be put into development mode by
        // adding -Dpixelitor.development=true to the command line
        if ("true".equals(System.getProperty("pixelitor.development"))) {
            Utils.ensureAssertionsEnabled();
            activeMode = DEVELOPMENT_GUI;
        }
    }

    public static void setUnitTestingMode() {
        activeMode = UNIT_TESTS;
    }
}

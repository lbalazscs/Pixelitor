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

package pixelitor;

/**
 * The way in which the GUI is created.
 */
public enum AppMode {
    /**
     * The mode used by end-users.
     */
    STANDARD_GUI() {
    },
    /**
     * In this mode there are additional development menus and runtime checks.
     */
    DEVELOPMENT_GUI() {
    },
    /**
     * In this mode there is no GUI, and some objects might be mocked.
     */
    UNIT_TESTS() {
    };

    public static AppMode ACTIVE = STANDARD_GUI;

    /**
     * Returns true if the app was started in development mode.
     * In this mode, additional menus and correctness checks are enabled.
     */
    public static boolean isDevelopment() {
        return ACTIVE == DEVELOPMENT_GUI;
    }

    public static boolean isUnitTesting() {
        return ACTIVE == UNIT_TESTS;
    }

    public static void setUnitTestingMode() {
        ACTIVE = UNIT_TESTS;
    }
}

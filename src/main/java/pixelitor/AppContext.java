/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.utils.AppPreferences;
import pixelitor.utils.Lazy;

import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.threadInfo;

/**
 * The context in which Pixelitor code is running.
 */
public enum AppContext {
    /**
     * The mode used by end-users.
     */
    FINAL_GUI() {
    },
    /**
     * In this mode there are are additional menus and runtime checks.
     */
    DEVELOPMENT_GUI() {
    },
    /**
     * In this mode there is no GUI, and some objects might be mocked.
     */
    UNIT_TESTS() {
    };

    // feature flags to avoid diverging branches
    public static boolean enableExperimentalFeatures = AppPreferences.loadExperimentalFeatures();
    public static final boolean enableAdjLayers = false;
    public static final boolean enableImageMode = false;

    public static AppContext CURRENT = FINAL_GUI;

    // Lazy because it should be calculated after the CURRENT is set.
    private static final Lazy<String> fixTitle = Lazy.of(AppContext::calcFixTitle);

    public static boolean isDevelopment() {
        return CURRENT == DEVELOPMENT_GUI;
    }

    public static boolean isFinal() {
        return !isDevelopment();
    }

    private static String calcFixTitle() {
        String s = "Pixelitor " + Pixelitor.VERSION_NUMBER;
        if (CURRENT != FINAL_GUI) {
            s += " DEVELOPMENT " + System.getProperty("java.version");
        }
        return s;
    }

    public static String getPixelitorWindowFixTitle() {
        assert calledOnEDT() : threadInfo();

        return fixTitle.get();
    }

    public static boolean isUnitTesting() {
        return CURRENT == UNIT_TESTS;
    }

    public static void setUnitTestingMode() {
        CURRENT = UNIT_TESTS;
    }
}

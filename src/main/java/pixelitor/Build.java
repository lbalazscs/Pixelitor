/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.utils.Lazy;
import pixelitor.utils.Utils;

import java.awt.EventQueue;

/**
 * The type of the "build" - in development mode there are additional
 * menus and runtime checks.
 */
public enum Build {
    DEVELOPMENT() {
    }, FINAL() {
    };

    private static boolean testing = false;

    public static final boolean enableAdjLayers = false;

    public static Build CURRENT = FINAL;

    public static final String VERSION_NUMBER = "4.2.0";

    // Lazy because it should be calculated after the CURRENT is set.
    private static final Lazy<String> fixTitle = Lazy.of(Build::calcFixTitle);

    public static boolean isDevelopment() {
        return CURRENT == DEVELOPMENT;
    }

    public static boolean isFinal() {
        return !isDevelopment();
    }

    private static String calcFixTitle() {
        String s = "Pixelitor " + Build.VERSION_NUMBER;
        if (CURRENT != FINAL) {
            s += " DEVELOPMENT " + System.getProperty("java.version");
        }
        return s;
    }

    public static String getPixelitorWindowFixTitle() {
        assert EventQueue.isDispatchThread() : "not EDT thread";

        return fixTitle.get();
    }

    public static synchronized boolean isTesting() {
        return testing;
    }

    public static void setTestingMode() {
        testing = true;
        Utils.makeSureAssertionsAreEnabled();
    }
}

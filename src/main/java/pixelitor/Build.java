/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import javax.swing.*;

/**
 * The type of the "build" - in development mode there are additional
 * menus and runtime checks.
 */
public enum Build {
    DEVELOPMENT(true) {
    }, FINAL(false) {
    };

    private final boolean development;

    Build(boolean development) {
        this.development = development;
    }

    public boolean isDevelopment() {
        return development;
    }

    public static final boolean enableAdjLayers = false;

    public static Build CURRENT = FINAL;

    public static final String VERSION_NUMBER = "4.0.2";

    private static String fixTitle = null;

    public static String getPixelitorWindowFixTitle() {
        assert SwingUtilities.isEventDispatchThread();
        if (fixTitle == null) {
            //noinspection NonThreadSafeLazyInitialization
            fixTitle = "Pixelitor " + Build.VERSION_NUMBER;
            if (CURRENT != FINAL) {
                fixTitle += " DEVELOPMENT " + System.getProperty("java.version");
            }
        }

        return fixTitle;
    }
}

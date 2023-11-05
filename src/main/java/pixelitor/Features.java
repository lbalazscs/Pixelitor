/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.utils.Dialogs;
import pixelitor.utils.AppPreferences;

import java.awt.EventQueue;

/**
 * Feature flags.
 */
public class Features {
    public static boolean enableExperimental = AppPreferences.loadExperimentalFeatures();
    public static final boolean enableFreeTransform = false;
    public static final boolean enableImageMode = false;

    private Features() {
    }

    public static void enableExperimental(boolean newValue) {
        if (enableExperimental == newValue) {
            return;
        }
        enableExperimental = newValue;
        if (!newValue) {
            return;
        }
        String msg = "<html>The following experimental features have been enabled:<ul>" +
            "<li>Smart objects</li>" +
            "<li>Layer groups</li>" +
            "<li>Gradient fill layers</li>" +
            "<li>Color fill layers</li>" +
            "<li>Shape layers</li>" +
            "</ul><br>Note that future versions of Pixelitor might not be able<br>to open pxc files with experimental features." +
            "<br><br>Some experimental features will be fully activated<br>only after restarting Pixelitor.";
        // show the new dialog only after the main dialog is closed
        EventQueue.invokeLater(() ->
            Dialogs.showWarningDialog("Experimental Features", msg));
    }
}

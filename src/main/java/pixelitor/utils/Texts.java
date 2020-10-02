/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.utils;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * The internationalized texts of the UI
 */
public class Texts {
    // this locale is used in the tests, the GUI replaces it
    private static ResourceBundle resources = ResourceBundle.getBundle("texts", Locale.US);

    private Texts() {
    }

    public static ResourceBundle getResources() {
        return resources;
    }

    public static String get(String key) {
        return resources.getString(key);
    }

    public static void setLocale(Locale newLocale) {
        resources = ResourceBundle.getBundle("texts", newLocale);
    }
}

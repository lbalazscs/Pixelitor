/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * The internationalized texts of the UI
 */
public class Texts {
    private static ResourceBundle resources;

    private Texts() {
    }

    public static ResourceBundle getResources() {
        return resources;
    }

    public static String i18n(String key) {
        return resources.getString(key);
    }

    public static String formatI18N(String key, Object[] args) {
        return new MessageFormat(i18n(key)).format(args);
    }

    public static void init() {
        resources = ResourceBundle.getBundle("texts", Locale.getDefault());
    }
}

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

import pixelitor.OpenImages;
import pixelitor.menus.MenuAction;

import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * The internationalized texts of the UI
 */
public class Texts {
    private static Language currentLang = Language.ENGLISH;
    private static ResourceBundle resources;
    private static final Language[] languages = Language.values();

    private Texts() {
    }

    public static boolean isLangCodeSupported(String code) {
        for (Language lang : languages) {
            if(lang.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }

    public static Language getCurrentLanguage() {
        return currentLang;
    }

    public static void loadLanguage() {
        String loadedCode = AppPreferences.loadLanguageCode();

        Language loadedLang = Arrays.stream(languages)
              .filter(lang -> lang.getCode().equals(loadedCode))
              .findFirst()
              .orElse(Language.ENGLISH);

        setCurrentLang(loadedLang);
    }

    public static void setCurrentLang(Language lang) {
        currentLang = lang;
        Locale newLocale = new Locale(currentLang.getCode());
        Locale.setDefault(newLocale);
        resources = ResourceBundle.getBundle("texts", newLocale);
    }

    public static ResourceBundle getResources() {
        return resources;
    }

    public static String get(String key) {
        return resources.getString(key);
    }

    public static final MenuAction CLOSE_ALL_ACTION = new MenuAction(get("close_all")) {
        @Override
        public void onClick() {
            OpenImages.warnAndCloseAll();
        }
    };
    public static final MenuAction CLOSE_ACTIVE_ACTION = new MenuAction(get("close")) {
        @Override
        public void onClick() {
            OpenImages.warnAndCloseActive();
        }
    };
    public static final MenuAction CLOSE_UNMODIFIED_ACTION = new MenuAction("Close Unmodified") {
        @Override
        public void onClick() {
            OpenImages.warnAndCloseUnmodified();
        }
    };

    public static final String NEW_IMAGE_STRING = get("new_image");
}

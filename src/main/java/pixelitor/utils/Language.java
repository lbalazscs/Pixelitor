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

package pixelitor.utils;

import java.util.Arrays;
import java.util.Locale;

/**
 * The languages supported by Pixelitor.
 */
public enum Language {
    DUTCH("Dutch", "nl"),
    ENGLISH("English", "en"),
    FRENCH("French", "fr"),
    GERMAN("German", "de"),
    ITALIAN("Italian", "it"),
    PORTUGUESE("Portuguese (Br)", "pt-br"),
    RUSSIAN("Russian", "ru"),
    SPANISH("Spanish", "es");

    public static Locale sysLocale;
    private static Language activeLang = ENGLISH;
    private static final Language[] SUPPORTED_LANGUAGES = values();

    private final String displayName;
    private final String code;

    Language(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static boolean isSupported(String code) {
        for (Language lang : SUPPORTED_LANGUAGES) {
            if (lang.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }

    public static void init() {
        // store system locale for number formatting
        sysLocale = Locale.getDefault();

        if (!isSupported(sysLocale.getLanguage())) {
            // if a language is not supported yet, then set English
            // in order to avoid mixed-language problems (see issue #35)
            Locale.setDefault(Locale.US);
        }

        String loadedCode = AppPreferences.loadLanguageCode();
        Language loadedLang = Arrays.stream(SUPPORTED_LANGUAGES)
            .filter(lang -> lang.getCode().equals(loadedCode))
            .findFirst()
            .orElse(ENGLISH);

        setActive(loadedLang);
    }

    public static Language getActive() {
        return activeLang;
    }

    public static void setActive(Language lang) {
        activeLang = lang;
        Locale newLocale = Locale.forLanguageTag(activeLang.getCode());
        Locale.setDefault(newLocale);
        Texts.init();
    }
}

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

package pixelitor.utils;

import java.util.Arrays;
import java.util.Locale;

/**
 * The languages supported by Pixelitor
 */
public enum Language {
    DUTCH("Dutch", "nl") {},
    ENGLISH("English", "en") {},
    FRENCH("French", "fr") {},
    GERMAN("German", "de") {},
    ITALIAN("Italian", "it") {},
    PORTUGUESE("Portuguese (Br)", "pt-br") {},
    RUSSIAN("Russian", "ru") {},
    SPANISH("Spanish", "es") {};


    private final String guiName;
    private final String code;

    Language(String guiName, String code) {
        this.guiName = guiName;
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return guiName;
    }

    // from here static members and methods

    private static Language currentLang = ENGLISH;
    private static final Language[] languages = values();

    public static boolean isCodeSupported(String code) {
        for (Language lang : languages) {
            if (lang.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }

    public static void load() {
        String loadedCode = AppPreferences.loadLanguageCode();

        Language loadedLang = Arrays.stream(languages)
            .filter(lang -> lang.getCode().equals(loadedCode))
            .findFirst()
            .orElse(ENGLISH);

        setCurrent(loadedLang);
    }

    public static Language getCurrent() {
        return currentLang;
    }

    public static void setCurrent(Language lang) {
        currentLang = lang;
        Locale newLocale = Locale.forLanguageTag(currentLang.getCode());
        Locale.setDefault(newLocale);
        Texts.init();
    }
}

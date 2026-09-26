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

package pixelitor.gui.utils;

import javax.swing.*;

/**
 * The available Swing Look and Feels.
 */
public enum Theme {
    NIMBUS("Nimbus", false, false, 35),
    FLAT_DARK("Flat Dark", true, true, 30),
    FLAT_LIGHT("Flat Light", false, true, 30),
    SYSTEM("System", false, false, 30);

    private final String displayName;
    private final boolean dark;
    private final boolean flat;
    private final int frameDecorationHeight;

    Theme(String displayName, boolean dark, boolean flat, int frameDecorationHeight) {
        this.displayName = displayName;
        this.dark = dark;
        this.flat = flat;
        this.frameDecorationHeight = frameDecorationHeight;
    }

    public String getLAFClassName() {
        return switch (this) {
            case NIMBUS -> "javax.swing.plaf.nimbus.NimbusLookAndFeel";
            case FLAT_DARK -> "com.formdev.flatlaf.FlatDarculaLaf";
            case FLAT_LIGHT -> "com.formdev.flatlaf.FlatIntelliJLaf";
            case SYSTEM -> UIManager.getSystemLookAndFeelClassName();
        };
    }

    public boolean isDark() {
        return dark;
    }

    public boolean isNimbus() {
        return this == NIMBUS;
    }

    public boolean isFlat() {
        return flat;
    }

    /**
     * Returns a stable, unique identifier for persistence.
     */
    public String getPrefsCode() {
        return name();
    }

    public int getFrameDecorationHeight() {
        return frameDecorationHeight;
    }

    public static Theme fromPrefsCode(String code) {
        if (code == null || code.isEmpty()) {
            return Themes.DEFAULT;
        }

        for (Theme theme : values()) {
            if (theme.getPrefsCode().equals(code)) {
                return theme;
            }
        }

        // legacy fallback: match against display name
        for (Theme theme : values()) {
            if (theme.displayName.equals(code)) {
                return theme;
            }
        }

        return Themes.DEFAULT;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

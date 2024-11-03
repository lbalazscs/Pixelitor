/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
    NIMBUS("Nimbus", false, false) {
        @Override
        String getLAFClassName() {
            return "javax.swing.plaf.nimbus.NimbusLookAndFeel";
        }
    }, FLAT_DARK("Flat Dark", true, true) {
        @Override
        String getLAFClassName() {
            return "com.formdev.flatlaf.FlatDarculaLaf";
        }
    }, FLAT_LIGHT("Flat Light", false, true) {
        @Override
        String getLAFClassName() {
            return "com.formdev.flatlaf.FlatIntelliJLaf";
        }
    }, SYSTEM("System", false, false) {
        @Override
        String getLAFClassName() {
            return UIManager.getSystemLookAndFeelClassName();
        }
    };

    private final String displayName;
    private final boolean dark;
    private final boolean flat;

    Theme(String displayName, boolean dark, boolean flat) {
        this.displayName = displayName;
        this.dark = dark;
        this.flat = flat;
    }

    abstract String getLAFClassName();

    public boolean isDark() {
        return dark;
    }

    public boolean isNimbus() {
        return this == NIMBUS;
    }

    public boolean isFlat() {
        return flat;
    }

    public String getSaveCode() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

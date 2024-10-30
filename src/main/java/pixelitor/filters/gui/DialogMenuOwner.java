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

package pixelitor.filters.gui;

/**
 * A {@link PresetOwner} that can use a {@link DialogMenuBar} to
 * add menus to configuration dialogs.
 */
public interface DialogMenuOwner extends PresetOwner {
    default boolean hasBuiltinPresets() {
        return false;
    }

    default Preset[] getBuiltinPresets() {
        // the subclasses override this if they have built-in presets
        throw new UnsupportedOperationException();
    }

    default boolean hasHelp() {
        return false;
    }

    default String getHelpURL() {
        // the subclasses override this if they have a help URL
        throw new UnsupportedOperationException();
    }
}

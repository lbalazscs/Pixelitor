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

import javax.swing.*;

/**
 * Represents a collection of widget states that can be loaded
 * to configure a {@link PresetOwner}.
 * Built-in presets are predefined configurations that are hardcoded,
 * as opposed to user-created presets that can be saved to disk in
 * the preset directory at runtime. While user-created presets are
 * always implemented as a {@link UserPreset}, the built-in presets
 * can be implemented either as a {@link UserPreset}
 * or as a {@link FilterState}.
 */
public interface Preset {
    /**
     * Converts this preset into an {@link Action} that can be added
     * to menus. When triggered, the action applies this
     * preset's settings to the given {@link PresetOwner}.
     */
    Action createAction(PresetOwner owner);
}

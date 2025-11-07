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

package pixelitor.filters.gui;

/**
 * Represents a component (such as a filter or tool) that can
 * load and save its current state as {@link Preset} objects.
 */
public interface PresetOwner {
    /**
     * Determines if this component supports saving and loading {@link UserPreset}s.
     */
    boolean supportsUserPresets();

    /**
     * Determines if menu items related to {@link UserPreset}s should be added for this component.
     * <p>
     * Should return true only if {@link #supportsUserPresets()} returns true.
     * Trivial filters might not have menu items even if they could support presets.
     */
    default boolean shouldHaveUserPresetsMenu() {
        return supportsUserPresets();
    }

    /**
     * Returns the directory name where this component's
     * {@link UserPreset}s are stored. The directory will be created as
     * a subdirectory of the application's preset root directory.
     */
    String getPresetDirName();

    /**
     * Creates a new user preset with the current state of this component.
     * Should not be called if {@link #supportsUserPresets()} returns false.
     */
    default UserPreset createUserPreset(String presetName) {
        UserPreset preset = new UserPreset(presetName, getPresetDirName());
        saveStateTo(preset);
        return preset;
    }

    /**
     * Saves the current state of this component to the given {@link UserPreset}.
     */
    void saveStateTo(UserPreset preset);

    /**
     * Loads the state stored in the given {@link UserPreset}.
     */
    void loadUserPreset(UserPreset preset);

    /**
     * Loads a {@link FilterState} into this component.
     * <p>
     * This method is used to apply built-in presets or animation states,
     * which are represented as {@link FilterState} objects.
     */
    default void loadFilterState(FilterState filterState, boolean reset) {
        // used only for parametrized filters
        throw new UnsupportedOperationException();
    }
}

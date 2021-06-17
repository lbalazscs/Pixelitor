/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.utils.VisibleForTesting;

import java.awt.Dimension;

/**
 * The model for a filter parameter, which (unlike a button) holds
 * some value.
 *
 * For practical reasons implementations of this are also found
 * outside filters as models of GUI elements, but then only
 * a subset of their functionality is used.
 */
public interface FilterParam extends FilterSetting, Resettable {

    /**
     * Sets a random value without triggering the filter
     */
    void randomize();

    /**
     * Can be used to adjust the maximum and default values
     * according to the size of the current image
     */
    default void adaptToImageSize(Dimension size) {
        // by default does nothing, as most controls are unaffected
    }

    /**
     * Captures the state of this parameter into the returned
     * "memento" object.
     */
    ParamState<?> copyState();

    default String getPresetKey() {
        return getName();
    }

    /**
     * Sets the internal state according to the given {@link ParamState}
     * without triggering the filter.
     */
    void loadStateFrom(ParamState<?> state, boolean updateGUI);

    /**
     * Sets the internal state according to the given saved
     * preset string, without triggering the filter. The GUI is updated.
     */
    void loadStateFrom(String savedValue);

    /**
     * Load the state from the given user preset.
     */
    default void loadStateFrom(UserPreset preset) {
        // overridden in the composite params
        String key = getPresetKey();
        String savedValue = preset.get(key);

        if (savedValue != null) {
            loadStateFrom(savedValue);
        }
    }

    /**
     * Save the state to the given user preset.
     */
    default void saveStateTo(UserPreset preset) {
        // overridden in the composite params
        preset.put(getPresetKey(), copyState().toSaveString());
    }

    /**
     * True if the value can be interpolated in some useful way.
     * All implementing classes return either always true or always false.
     */
    boolean canBeAnimated();

    /**
     * Whether a filter parameter was configured to be
     * affected when the user presses "Randomize"
     */
    boolean allowRandomize();

    /**
     * Override the randomize policy
     */
    @VisibleForTesting
    void setRandomizePolicy(RandomizePolicy policy);

    void setToolTip(String tip);

    /**
     * Returns the parameter value.
     * The return type can't be more specific than Object,
     * but this is still useful for testing.
     */
    @VisibleForTesting
    Object getParamValue();
}

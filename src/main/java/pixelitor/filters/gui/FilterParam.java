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

import java.awt.Rectangle;

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
     * A filter param can implement this to
     * adjust the maximum and default values
     * according to the size of the current image
     */
    void considerImageSize(Rectangle bounds);

    /**
     * Captures the state of this parameter into the returned
     * "memento" object.
     */
    ParamState<?> copyState();

    /**
     * Sets the internal state according to the given {@link ParamState}
     * without triggering the filter.
     */
    void setState(ParamState<?> state, boolean updateGUI);

    /**
     * Sets the internal state according to the given saved
     * preset string, without triggering the filter. The GUI is updated.
     */
    void setState(String savedValue);

    /**
     * Load the state from the given user preset.
     */
    default void loadStateFrom(UserPreset preset) {
        // overridden in the composite params
        setState(preset.get(getName()));
    }

    /**
     * Save the state to the given user preset.
     */
    default void saveStateTo(UserPreset preset) {
        // overridden in the composite params
        preset.put(getName(), copyState().toSaveString());
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

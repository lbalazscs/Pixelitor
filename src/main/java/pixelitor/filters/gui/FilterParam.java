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

import pixelitor.layers.Filterable;
import pixelitor.utils.debug.Debuggable;

/**
 * The model for a filter parameter, which (unlike a button) holds
 * a value that can be changed by the user.
 *
 * For practical reasons, implementations of this interface are also
 * used outside of filters as models of GUI elements. In such cases,
 * only a subset of their functionality is used.
 */
public interface FilterParam extends FilterSetting, Resettable, Debuggable {

    /**
     * Sets a random value without triggering any filter update.
     */
    void randomize();

    /**
     * Can be used to adapt the offered ranges/choices to the current
     * layer, composition or the list of open compositions.
     */
    default void adaptToContext(Filterable layer, boolean changeValue) {
        // by default does nothing, as most controls are unaffected
    }

    /**
     * Captures the current state of this parameter as a "memento" object.
     */
    ParamState<?> copyState();

    /**
     * Returns a unique key for saving or loading presets
     * associated with this parameter.
     */
    String getPresetKey();

    /**
     * Loads the given state into this parameter, optionally
     * updating the GUI, and without triggering the filter.
     */
    void loadStateFrom(ParamState<?> state, boolean updateGUI);

    /**
     * Loads the given state into this parameter,
     * updating the GUI, and without triggering the filter.
     */
    void loadStateFrom(String savedValue);

    /**
     * Loads the state from the given user preset,
     * updating the GUI, and without triggering the filter.
     */
    default void loadStateFrom(UserPreset preset) {
        // overridden in the composite params
        String savedValue = preset.get(getPresetKey());

        if (savedValue != null) {
            loadStateFrom(savedValue);
        }
    }

    /**
     * Saves the current state to the given user preset.
     */
    default void saveStateTo(UserPreset preset) {
        // overridden in the composite params
        preset.put(getPresetKey(), copyState().toSaveString());
    }

    /**
     * Returns true if this parameter's value can change gradually,
     * allowing it to be interpolated for tweening animations.
     */
    boolean isAnimatable();

    /**
     * Indicates whether this parameter should be randomized when
     * the "Randomize" button is clicked.
     */
    boolean shouldRandomize();

    /**
     * Sets the randomization policy for this parameter.
     */
    void setRandomizePolicy(RandomizePolicy policy);

    /**
     * Sets a tooltip message for the GUI widget associated with this parameter.
     */
    void setToolTip(String tip);

    /**
     * Returns the parameter value as an Object (useful for testing, debugging).
     */
    Object getParamValue();

    /**
     * Returns true if the filter should have presets
     * even if it has just one {@link FilterParam} (i.e. this one).
     */
    boolean isComplex();
}

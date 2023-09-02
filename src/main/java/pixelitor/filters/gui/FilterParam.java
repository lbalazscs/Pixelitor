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

package pixelitor.filters.gui;

import pixelitor.layers.Filterable;
import pixelitor.utils.debug.Debuggable;

/**
 * The model for a filter parameter, which (unlike a button) holds
 * a value that can be changed by the user.
 *
 * For practical reasons, implementations of this interface are also
 * found outside filters as models of GUI elements. In such cases,
 * only a subset of their functionality is used.
 */
public interface FilterParam extends FilterSetting, Resettable, Debuggable {

    /**
     * Sets a random value without triggering the filter.
     */
    void randomize();

    /**
     * Can be used to adapt the offered ranges/choices to the current
     * drawable, composition or the list of open compositions.
     */
    default void updateOptions(Filterable layer, boolean changeValue) {
        // by default does nothing, as most controls are unaffected
    }

    /**
     * Captures the state of this parameter into the returned
     * "memento" object.
     */
    ParamState<?> copyState();

    String getPresetKey();

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
     * Loads the state from the given user preset.
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
     * Saves the state to the given user preset.
     */
    default void saveStateTo(UserPreset preset) {
        // overridden in the composite params
        preset.put(getPresetKey(), copyState().toSaveString());
    }

    /**
     * Returns true if the value can change gradually,
     * allowing it to be used for tweening animations.
     * All implementing classes return either always true or always false.
     */
    boolean isAnimatable();

    /**
     * Indicates whether a filter parameter is configured to be
     * affected when the user presses "Randomize."
     */
    boolean canRandomize();

    void setRandomizePolicy(RandomizePolicy policy);

    void setToolTip(String tip);

    /**
     * Returns the parameter value.
     */
    Object getParamValue();
}

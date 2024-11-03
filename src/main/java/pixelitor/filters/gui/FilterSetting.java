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
 * The model of a widget that appears in a filter GUI.
 * Can be either a {@link FilterButtonModel} for buttons
 * or a {@link FilterParam} for adjustable parameters.
 */
public interface FilterSetting {
    /**
     * Returns the display name of this filter setting.
     */
    String getName();

    /**
     * Creates the GUI component that corresponds to this model.
     *
     * If this is an instance of {@link FilterParam},
     * the returned component must also implement {@link ParamGUI}.
     */
    JComponent createGUI();

    /**
     * Creates and names the GUI component for this filter setting.
     * The GUI component's name is used for lookup during automatic
     * GUI testing and it's not the same as the display name.
     */
    default JComponent createGUI(String name) {
        JComponent gui = createGUI();
        gui.setName(name);
        return gui;
    }

    /**
     * Sets the listener that will be notified when this filter setting is adjusted.
     */
    void setAdjustmentListener(ParamAdjustmentListener listener);

    /**
     * Enables or disables this setting for one of two possible reasons:
     * (1) because of the app logic or (2) because non-animatable
     * parameters should be disabled in the final animation dialogs.
     */
    void setEnabled(boolean b, EnabledReason reason);

    default void setEnabled(boolean b) {
        setEnabled(b, EnabledReason.APP_LOGIC);
    }

    /**
     * The possible reasons for enabling or disabling a filter setting.
     */
    enum EnabledReason {
        APP_LOGIC, ANIMATION_ENDING_STATE
    }
}

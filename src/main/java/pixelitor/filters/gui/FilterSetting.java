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

import javax.swing.*;

/**
 * The model of something that appears in a filter GUI.
 * If that something is a button, then the {@link FilterButtonModel}
 * sub-interface is used, otherwise the {@link FilterParam}.
 */
public interface FilterSetting {
    String getName();

    /**
     * Creates the GUI component that corresponds to this model.
     *
     * If this is an instance of {@link FilterParam},
     * then the returned JComponent must also be
     * an implementation of {@link ParamGUI}
     */
    JComponent createGUI();

    /**
     * A convenience method that also sets a name of the created GUI.
     */
    default JComponent createGUI(String name) {
        JComponent gui = createGUI();
        gui.setName(name);
        return gui;
    }

    /**
     * Sets the listener that will be notified when the filter setting is adjusted.
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
     * The possible reasons for enabling or disabling a filter.
     */
    enum EnabledReason {
        APP_LOGIC, FINAL_ANIMATION_SETTING
    }
}

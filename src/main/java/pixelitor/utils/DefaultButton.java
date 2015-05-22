/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

package pixelitor.utils;

import pixelitor.filters.gui.Resettable;

import javax.swing.*;

/**
 * A button that resets its filter parameter, and displays and arrow when the
 * filter parameter is not set to the default value
 */
public class DefaultButton extends JButton {
    private static final Icon defaultIcon = IconUtils.getWestArrowIcon();
    private final Resettable resettable;

    public DefaultButton(Resettable resettable) {
        this.resettable = resettable;
        addActionListener(e -> resettable.reset(true));
        setToolTipText("Reset the default setting");
        updateState();
    }

    public void updateState() {
        setDefault(resettable.isSetToDefault());
    }

    private void setDefault(boolean b) {
        if (b) {
            setIcon(null);
        } else {
            setIcon(defaultIcon);
        }
    }
}

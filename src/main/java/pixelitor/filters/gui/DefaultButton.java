/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.utils.Icons;

import javax.swing.*;
import java.awt.Dimension;

/**
 * A button that resets a Resettable, and displays an arrow when the
 * Resettable is not set to its default value
 */
public class DefaultButton extends JButton {
    private static final Dimension PREFERRED_SIZE = new Dimension(28, 28);
    private Resettable resettable;

    /**
     * Create a default button with no text
     */
    public DefaultButton(Resettable resettable) {
        setPreferredSize(PREFERRED_SIZE);
        init(resettable);
    }

    /**
     * Create a default button with the given text
     */
    public DefaultButton(String text, Resettable resettable) {
        super(text);
        init(resettable);
    }

    private void init(Resettable resettable) {
        this.resettable = resettable;
        addActionListener(e -> resettable.reset(true));
        setToolTipText(resettable.getResetToolTip());
        updateIcon();
    }

    public void updateIcon() {
        boolean isSetToDefault = resettable.isSetToDefault();
        setArrowIcon(isSetToDefault);
        setEnabled(!isSetToDefault);
    }

    private void setArrowIcon(boolean isSetToDefault) {
        if (isSetToDefault) {
            setIcon(null);
        } else {
            setIcon(Icons.getWestArrowIcon());
        }
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b && !resettable.isSetToDefault());
    }
}

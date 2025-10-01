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

import pixelitor.utils.Icons;

import javax.swing.*;
import java.awt.Dimension;

/**
 * A button that resets a {@link Resettable} object  when clicked, and
 * displays an arrow icon when the {@link Resettable} is not set to
 * its default value.
 */
public class ResetButton extends JButton {
    private static final Dimension PREFERRED_SIZE = new Dimension(28, 28);
    private Resettable resettable;

    public ResetButton(Resettable resettable) {
        setPreferredSize(PREFERRED_SIZE);
        initialize(resettable);
    }

    private void initialize(Resettable resettable) {
        this.resettable = resettable;
        addActionListener(e -> resettable.reset(true));
        setToolTipText(resettable.getResetToolTip());
        updateState();
    }

    public void updateState() {
        if (resettable.isAtDefault()) {
            setIcon(null);
            setEnabled(false);
        } else {
            setIcon(Icons.getResetIcon());
            setEnabled(true);
        }
    }

    @Override
    public void setEnabled(boolean b) {
        // can be disabled either because the parent component
        // component is disabled or because it's at the default value
        super.setEnabled(b && !resettable.isAtDefault());
    }
}

/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
 * A button that resets a {@link Resettable} object when clicked, and
 * displays an arrow icon when the {@link Resettable} is not set to
 * its default value.
 */
public class ResetButton extends JButton {
    private static final Dimension PREFERRED_SIZE = new Dimension(28, 28);
    private final Resettable resettable;
    private boolean enabledByParent = true;

    public ResetButton(Resettable resettable) {
        this.resettable = resettable;

        addActionListener(_ -> resettable.reset(true));
        setPreferredSize(PREFERRED_SIZE);
        setToolTipText(resettable.getResetToolTip());

        updateState();
    }

    public void updateState() {
        boolean atDefault = resettable.isAtDefault();
        setIcon(atDefault ? null : Icons.getResetIcon());
        super.setEnabled(enabledByParent && !atDefault);
    }

    @Override
    public void setEnabled(boolean b) {
        // can be disabled either because the parent component
        // is disabled or because it's at the default value
        enabledByParent = b;
        super.setEnabled(b && !resettable.isAtDefault());
    }
}

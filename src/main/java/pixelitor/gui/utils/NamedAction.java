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

package pixelitor.gui.utils;

import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * An {@link AbstractAction} with convenience methods
 * for renaming and setting the tooltip.
 */
public abstract class NamedAction extends AbstractAction {
    protected NamedAction() {
    }

    protected NamedAction(String name) {
        super(name);
    }

    protected NamedAction(String name, Icon icon) {
        super(name, icon);
    }

    public void setText(String newName) {
        putValue(Action.NAME, newName);
    }

    public String getText() {
        return (String) getValue(Action.NAME);
    }

    public void setToolTip(String toolTip) {
        putValue(Action.SHORT_DESCRIPTION, toolTip);
    }

    @Override
    public final void actionPerformed(ActionEvent e) {
        try {
            onClick(e);
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    protected abstract void onClick(ActionEvent e);
}

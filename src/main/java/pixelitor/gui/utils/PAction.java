/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
 * The "Pixelitor action" is the common superclass of most actions.
 */
public class PAction extends NamedAction {
    private final Runnable task;

    public PAction(Runnable task) {
        this.task = task;
    }

    public PAction(String name, Runnable task) {
        super(name);
        this.task = task;
    }

    public PAction(String name, Icon icon, Runnable task) {
        super(name, icon);
        this.task = task;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            task.run();
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }
}

/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.menus.edit;

import pixelitor.filters.Fade;
import pixelitor.filters.FilterAction;
import pixelitor.gui.OpenComps;
import pixelitor.gui.View;
import pixelitor.history.History;
import pixelitor.utils.CompActivationListener;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

/**
 * The Fade menu item. It is enabled only if fading is possible.
 */
public class FadeMenuItem extends JMenuItem implements UndoableEditListener, CompActivationListener {
    public static final FadeMenuItem INSTANCE = new FadeMenuItem();

    private FadeMenuItem() {
        super(new FilterAction("Fade", Fade::new));
        History.addUndoableEditListener(this);
        OpenComps.addActivationListener(this);
        setEnabled(false);
    }

    @Override
    public void undoableEditHappened(UndoableEditEvent e) {
        boolean b = History.canFade();
        refresh(b);
    }

    public void refresh(boolean canFade) {
        setEnabled(canFade);
        Action action = getAction();
        if (canFade) {
            action.putValue(Action.NAME, "Fade " + History.getLastEditName() + "...");
        } else {
            action.putValue(Action.NAME, "Fade...");
        }
    }

    @Override
    public void allCompsClosed() {
        setEnabled(false);
    }

    @Override
    public void compActivated(View oldView, View newView) {
        setEnabled(false);

// the following should be very slightly better, but goes into a complex territory:
//        setEnabled(History.canFade());
    }
}
/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.util.FilterAction;
import pixelitor.gui.View;
import pixelitor.history.History;
import pixelitor.utils.Texts;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

/**
 * The Fade action for the fade menu item. It is enabled only if fading is possible.
 */
public class FadeAction extends FilterAction implements UndoableEditListener {
    public static final FadeAction INSTANCE = new FadeAction();

    private FadeAction() {
        super(Fade.NAME, Fade::new);
        History.addUndoableEditListener(this);
        setEnabled(false);
    }

    @Override
    public void undoableEditHappened(UndoableEditEvent e) {
        boolean b = History.canFade();
        refresh(b);
    }

    public void refresh(boolean canFade) {
        setEnabled(canFade);
        String menuName;
        if (canFade) {
            Object[] msgArguments = {History.getLastEditName()};
            menuName = Texts.formatI18N("fade_filter", msgArguments);
        } else {
            menuName = Fade.NAME;
        }
        setText(menuName + "...");
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        setEnabled(false);

// the following should be very slightly better, but goes into a complex territory:
//        setEnabled(History.canFade());
    }
}
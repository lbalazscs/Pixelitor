/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import pixelitor.Composition;
import pixelitor.filters.Fade;
import pixelitor.filters.FilterAction;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.history.History;
import pixelitor.utils.ImageSwitchListener;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

/**
 * The Fade menu item. It is enabled only if fading is possible.
 */
public class FadeMenuItem extends JMenuItem implements UndoableEditListener, ImageSwitchListener {
    public static final FadeMenuItem INSTANCE = new FadeMenuItem();

    private FadeMenuItem() {
        super(new FilterAction("Fade", Fade::new));
        History.addUndoableEditListener(this);
        ImageComponents.addImageSwitchListener(this);
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
    public void noOpenImageAnymore() {
        setEnabled(false);
    }

    @Override
    public void newImageOpened(Composition comp) {
        setEnabled(false);
    }

    @Override
    public void activeImageHasChanged(ImageComponent oldIC, ImageComponent newIC) {
        setEnabled(History.canFade());
    }
}
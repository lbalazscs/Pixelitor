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

package pixelitor.menus;

import pixelitor.Composition;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.history.History;
import pixelitor.history.PixelitorEdit;
import pixelitor.utils.ImageSwitchListener;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

/**
 *
 */
public class RepeatMenuItem extends JMenuItem implements UndoableEditListener, ImageSwitchListener {
    public RepeatMenuItem(Action a) {
        super(a);
        History.addUndoableEditListener(this);
        ImageComponents.addImageSwitchListener(this);
        setEnabled(false);
    }

    @Override
    public void undoableEditHappened(UndoableEditEvent e) {
        PixelitorEdit edit = (PixelitorEdit) e.getEdit();

        if (edit == null) { // happens when all images are closed
            setEnabled(false);
            getAction().putValue(Action.NAME, "Repeat");
            return;
        }

        setEnabled(edit.canRepeat());
        getAction().putValue(Action.NAME, "Repeat " + edit.getPresentationName());
    }

    @Override
    public void noOpenImageAnymore() {
        setEnabled(false);
    }

    @Override
    public void newImageOpened(Composition comp) {
        onNewComp(comp);
    }

    @Override
    public void activeImageHasChanged(ImageComponent oldIC, ImageComponent newIC) {
        Composition comp = newIC.getComp();
        onNewComp(comp);
    }

    private void onNewComp(Composition comp) {
        if (comp.activeIsImageLayerOrMask()) {
            setEnabled(History.canRepeatOperation());
            getAction().putValue(Action.NAME, "Repeat " + History.getLastEditName());
        } else {
            setEnabled(false);
        }
    }
}
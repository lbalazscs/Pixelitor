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

package pixelitor.layers;

import pixelitor.OpenImages;
import pixelitor.gui.View;
import pixelitor.gui.utils.NamedAction;
import pixelitor.utils.Icons;
import pixelitor.utils.ViewActivationListener;

import java.awt.event.ActionEvent;

/**
 * An Action that duplicates the active layer of the active composition
 */
public class DuplicateLayerAction extends NamedAction
    implements ViewActivationListener {

    public static final DuplicateLayerAction INSTANCE = new DuplicateLayerAction();

    private DuplicateLayerAction() {
        super("Duplicate Layer", Icons.load("duplicate_layer.png"));
        setToolTip("Duplicates the active layer.");
        setEnabled(false);
        OpenImages.addActivationListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        var comp = OpenImages.getActiveComp();
        comp.duplicateActiveLayer();
    }

    @Override
    public void allViewsClosed() {
        setEnabled(false);
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        setEnabled(true);
    }
}
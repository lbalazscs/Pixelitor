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

package pixelitor.layers;

import pixelitor.Views;
import pixelitor.gui.utils.OpenViewEnabledAction;
import pixelitor.utils.Icons;

import static pixelitor.utils.Texts.i18n;

/**
 * An Action that duplicates the active layer of the active composition
 */
public class DuplicateLayerAction extends OpenViewEnabledAction {
    public static final DuplicateLayerAction INSTANCE = new DuplicateLayerAction();

    private DuplicateLayerAction() {
        super(i18n("duplicate_layer"), Icons.load("duplicate_layer.png"));
        setToolTip(i18n("duplicate_layer_tt"));
    }

    @Override
    protected void onClick() {
        var comp = Views.getActiveComp();
        comp.duplicateActiveLayer();
    }
}
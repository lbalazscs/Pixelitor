/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Composition;
import pixelitor.gui.utils.ThemedImageIcon;
import pixelitor.gui.utils.ViewEnabledAction;
import pixelitor.utils.Icons;

import static pixelitor.utils.Texts.i18n;

/**
 * An Action that duplicates the active layer of the active composition
 */
public class DuplicateLayerAction extends ViewEnabledAction {
    public static final DuplicateLayerAction INSTANCE = new DuplicateLayerAction();

    private DuplicateLayerAction() {
        super(
            i18n("duplicate_layer"),
            Icons.loadThemed("duplicate_layer.png", ThemedImageIcon.GREEN),
            Composition::duplicateActiveLayer);
        setToolTip(i18n("duplicate_layer_tt"));
    }
}
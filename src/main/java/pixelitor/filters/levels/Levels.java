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

package pixelitor.filters.levels;

import pixelitor.filters.channels.ChannelAdjustmentFilter;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.layers.Filterable;

import java.io.Serial;

import static pixelitor.utils.Texts.i18n;

/**
 * The Levels filter, adjusting the tonal range of an image.
 */
public class Levels extends ChannelAdjustmentFilter<LevelsModel> {
    public static final String NAME = i18n("levels");

    @Serial
    private static final long serialVersionUID = 1780232770405846617L;

    public Levels() {
        super(new LevelsModel());
    }

    @Override
    protected FilterGUI createChannelGUI(Filterable layer) {
        return new LevelsGUI(this, layer);
    }
}

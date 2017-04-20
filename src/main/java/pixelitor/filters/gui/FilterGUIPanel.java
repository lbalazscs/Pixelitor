/*
 * Copyright 2017 Laszlo Balazs-Csiki
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

package pixelitor.filters.gui;

import pixelitor.filters.Filter;
import pixelitor.layers.Drawable;

import javax.swing.*;

import static pixelitor.ChangeReason.OP_PREVIEW;

/**
 * The superclass of all filter adjustment panels
 */
public abstract class FilterGUIPanel extends JPanel implements PreviewExecutor {
    protected Filter filter;
    private final Drawable dr;

    protected FilterGUIPanel(Filter filter, Drawable dr) {
        this.filter = filter;
        this.dr = dr;
    }

    @Override
    public void executeFilterPreview() {
        filter.execute(dr, OP_PREVIEW, this);
    }

}

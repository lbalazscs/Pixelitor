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

package pixelitor.filters.gui;

import pixelitor.ChangeReason;
import pixelitor.filters.Filter;
import pixelitor.layers.ImageLayer;

import javax.swing.*;

/**
 * The superclass of all filter adjustment panels
 */
public abstract class AdjustPanel extends JPanel implements PreviewExecutor {
    protected Filter filter;
    private final ImageLayer layer;

    protected AdjustPanel(Filter filter, ImageLayer layer) {
        this.filter = filter;
        this.layer = layer;
    }

    @Override
    public void executeFilterPreview() {
        filter.executeFilterWithBusyCursor(layer, ChangeReason.OP_PREVIEW, this);
    }

}

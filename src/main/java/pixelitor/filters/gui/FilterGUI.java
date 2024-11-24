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

package pixelitor.filters.gui;

import pixelitor.filters.Filter;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Filterable;

import javax.swing.*;
import java.awt.Component;

/**
 * Abstract base class for all filter configuration panels.
 * Instances of FilterGUI should not be cached, as they store
 * a reference to the {@link Filterable} layer being filtered.
 */
public abstract class FilterGUI extends JPanel {
    protected Filter filter;
    private final Filterable layer;

    protected FilterGUI(Filter filter, Filterable layer) {
        this.filter = filter;
        this.layer = layer;
    }

    public void startPreview(boolean firstPreview) {
        Component busyCursorTarget = firstPreview
            ? GUIUtils.getTopmostContainer(this)
            : this;
        layer.startPreview(filter, firstPreview, busyCursorTarget);
    }
}

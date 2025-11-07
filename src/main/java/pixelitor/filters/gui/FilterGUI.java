/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.layers.Filterable;

import javax.swing.*;
import java.awt.Component;

/**
 * An abstract base class for filter configuration panels.
 * <p>
 * Instances of this class should not be cached because they hold
 * a reference to the {@link Filterable} layer being filtered.
 */
public abstract class FilterGUI extends JPanel {
    protected Filter filter;
    private final Filterable layer;

    protected FilterGUI(Filter filter, Filterable layer) {
        this.filter = filter;
        this.layer = layer;
    }

    /**
     * Starts or updates the filter preview on the associated layer.
     */
    public void startPreview(boolean initialPreview) {
        // for the initial preview, show the busy cursor on the top-level container
        Component busyCursorTarget = initialPreview
            ? SwingUtilities.getWindowAncestor(this)
            : this;
        layer.startPreview(filter, initialPreview, busyCursorTarget);
    }
}

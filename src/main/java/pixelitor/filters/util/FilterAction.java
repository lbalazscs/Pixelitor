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

package pixelitor.filters.util;

import com.jhlabs.image.AbstractBufferedImageOp;
import pixelitor.Composition;
import pixelitor.filters.Fade;
import pixelitor.filters.Filter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.SimpleForwardingFilter;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.OpenViewEnabledAction;
import pixelitor.history.History;
import pixelitor.layers.*;
import pixelitor.utils.Messages;

import java.awt.EventQueue;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.function.Supplier;

/**
 * An action for running a lazily initialized filter.
 */
public class FilterAction extends OpenViewEnabledAction.Checked {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Supplier<Filter> factory;
    private transient Filter filter;
    private final String name;
    private boolean hasDialog;

    public FilterAction(String name, Supplier<Filter> factory) {
        this(name, true, factory);
    }

    public FilterAction(String name, boolean hasDialog, Supplier<Filter> factory) {
        super(name);
        this.name = name;
        this.hasDialog = hasDialog;
        setText(hasDialog ? name + "..." : name);

        assert factory != null;
        this.factory = factory;

        if (!name.equals(Fade.NAME)) {
            Filters.addFilter(this);
        }
    }

    public static FilterAction forwarding(String name,
                                          Supplier<AbstractBufferedImageOp> op,
                                          boolean supportsGray) {
        return new FilterAction(name,
            () -> new SimpleForwardingFilter(op, supportsGray)).noGUI();
    }

    @Override
    protected void onClick(Composition comp) {
        // invoke later to prevent "frozen" menus
        EventQueue.invokeLater(() ->
            process(comp.getActiveLayer()));
    }

    private void process(Layer layer) {
        if (layer.isMaskEditing()) {
            processFilterable(layer.getMask());
        } else if (layer instanceof SmartFilter smartFilter) {
            // Smart filters are Filterable, but the filter should be started on their smart object
            processSmartObject(smartFilter.getSmartObject());
        } else if (layer instanceof AdjustmentLayer) {
            // adjustment layers are Filterable, so this must be
            // checked first to prevent running unrelated filters on them.
            Dialogs.showErrorDialog("Adjustment Layer",
                name + " can't be used on adjustment layers.");
        } else if (layer instanceof Filterable filterable) {
            processFilterable(filterable);
        } else if (layer instanceof SmartObject so) {
            processSmartObject(so);
        } else if (layer.isRasterizable()) {
            boolean rasterize = Dialogs.showRasterizeDialog(layer, name);
            if (rasterize) {
                ImageLayer newImageLayer = layer.replaceWithRasterized();
                processFilterable(newImageLayer);
            }
        } else if (layer instanceof LayerGroup group) {
            assert group.isPassThrough(); // isolated groups can be rasterized
            Messages.showUnrasterizableLayerGroupError(group, name);
        } else {
            throw new IllegalStateException("layer is " + layer.getClass().getSimpleName());
        }
    }

    private void processSmartObject(SmartObject so) {
        createCachedFilter();
        // this logic is here and not in Filter because for
        // smart objects a new Filter instance is created
        if (!filter.canBeSmart()) {
            String msg = "<html>The filter <b>" + name + "</b> can't be used as a smart filter.";
            Messages.showInfo("Dumb Filter", msg);
            return;
        }

        Filter newFilter = createNewFilterInstance();
        so.tryAddingSmartFilter(newFilter);
    }

    private void processFilterable(Filterable dr) {
        createCachedFilter();
        dr.startFilter(filter, true);
    }

    public Filter createNewFilterInstance() {
        Filter newFilter = factory.get();
        newFilter.setName(name);
        return newFilter;
    }

    private void createCachedFilter() {
        if (filter == null) {
            filter = factory.get();
            filter.setName(name);
        }
    }

    public Filter getFilter() {
        createCachedFilter();
        return filter;
    }

    // Overrides the constructor parameter.
    // A bit ugly, but it simplifies the builders.
    public FilterAction noGUI() {
        hasDialog = false;
        setText(name); // without the "..."

        return this;
    }

    public boolean isAnimationFilter() {
        if (!hasDialog) {
            return false;
        }

        createCachedFilter();
        if (!(filter instanceof ParametrizedFilter pf)) {
            return false;
        }
        if (!pf.supportsTweenAnimation()) {
            return false;
        }
        if (!pf.getParamSet().isAnimatable()) {
            return false;
        }
        if (filter instanceof Fade && !History.canFade()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FilterAction filterAction = (FilterAction) o;
        return name.equals(filterAction.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Serial
    private void writeObject(ObjectOutputStream oos) throws IOException {
        throw new IOException("should not be serialized");
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}

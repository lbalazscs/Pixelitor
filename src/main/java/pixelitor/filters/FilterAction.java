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

package pixelitor.filters;

import com.jhlabs.image.AbstractBufferedImageOp;
import pixelitor.history.History;
import pixelitor.layers.ImageLayer;
import pixelitor.menus.ImageLayerAction;

import javax.swing.*;
import java.util.function.Supplier;

/**
 * An action that invokes a filter
 */
public class FilterAction extends ImageLayerAction {
    private final Supplier<Filter> filterSupplier;
    private Filter filter;
    private String listNamePrefix = null;

    public FilterAction(String name, Supplier<Filter> filterSupplier) {
        this(name, true, filterSupplier);
    }

    public FilterAction(String name, boolean hasDialog, Supplier<Filter> filterSupplier) {
        super(name, hasDialog);

        assert filterSupplier != null;

        this.filterSupplier = filterSupplier;

        FilterUtils.addFilter(this);
    }

    public FilterAction(String name, AbstractBufferedImageOp op) {
        this(name, () -> new SimpleForwardingFilter(op));
    }

    /**
     * This name appears when all filters are listed in a combo box
     * For example "Fill with Transparent" is better than "Transparent" in this case
     */
    public String getListName() {
        if (listNamePrefix == null) {
            return name;
        } else {
            return listNamePrefix + name;
        }
    }


    @Override
    protected void process(ImageLayer layer) {
        createFilter();

        filter.execute(layer);
    }

    private void createFilter() {
        if (filter == null) {
            filter = filterSupplier.get();
            filter.setFilterAction(this);
        }
    }

    public Filter getFilter() {
        createFilter();
        return filter;
    }

    public FilterAction withListNamePrefix(String listNamePrefix) {
        this.listNamePrefix = listNamePrefix;
        return this;
    }

    public FilterAction withFillListName() {
        return withListNamePrefix("Fill with ");
    }

    public FilterAction withExtractChannelListName() {
        return withListNamePrefix("Extract Channel: ");
    }

    // overrides the constructor parameter
    // a bit ugly, but it simplifies the builders
    public FilterAction withoutGUI() {
        hasDialog = false;
        menuName = name; // without the "..."
        putValue(Action.NAME, menuName);

        return this;
    }

    public boolean isAnimationFilter() {
        if (!hasDialog) {
            return false;
        }

        createFilter();
        if (!(filter instanceof FilterWithParametrizedGUI)) {
            return false;
        }
        FilterWithParametrizedGUI fpg = (FilterWithParametrizedGUI) filter;
        if (fpg.excludeFromAnimation()) {
            return false;
        }
        if (!fpg.getParamSet().canBeAnimated()) {
            return false;
        }
        if (filter instanceof Fade) {
            if (!History.canFade()) {
                return false;
            }
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

        if (!getName().equals(filterAction.getName())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }


    @Override
    public String toString() {
        return getListName();
    }
}

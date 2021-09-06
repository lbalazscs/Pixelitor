/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.Fade;
import pixelitor.filters.Filter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.SimpleForwardingFilter;
import pixelitor.gui.utils.Dialogs;
import pixelitor.history.History;
import pixelitor.layers.Drawable;
import pixelitor.layers.SmartObject;
import pixelitor.menus.DrawableAction;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.function.Supplier;

/**
 * An action that runs a filter on the active Drawable.
 */
public class FilterAction extends DrawableAction {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Supplier<Filter> factory;
    private transient Filter filter;

    public FilterAction(String name, Supplier<Filter> factory) {
        this(name, true, factory);
    }

    public FilterAction(String name, boolean hasDialog, Supplier<Filter> factory) {
        super(name, hasDialog);

        assert factory != null;
        this.factory = factory;

        if (!name.equals(Fade.NAME)) {
            FilterUtils.addFilter(this);
        }
    }

    public static FilterAction forwarding(String name,
                                          Supplier<AbstractBufferedImageOp> op,
                                          boolean supportsGray) {
        return new FilterAction(name,
            () -> new SimpleForwardingFilter(op, supportsGray)).noGUI();
    }

    @Override
    protected void process(Drawable dr) {
        if (dr instanceof SmartObject so) {
            if (so.hasSmartFilters()) {
                handleExistingSmartFilters(so);
                return;
            }
            Filter newFilter = createNewInstanceFilter();
            if (newFilter.startOn(dr, false)) {
                so.addSmartFilter(newFilter);
            }
            return;
        }
        createCachedFilter();
        filter.startOn(dr, true);
    }

    private void handleExistingSmartFilters(SmartObject so) {
        boolean replace;
        String existingFilterName = so.getSmartFilter(0).getName();
        String msg = String.format(
            "<html>The smart object <b>%s</> already has " +
            "a smart filter (<b>%s</b>). " +
            "<br>Currently a smart object can have only one smart filter." +
            "<br>Replace <b>%s</b> with <b>%s</b>?",
            so.getName(), existingFilterName, existingFilterName, name);
        replace = Dialogs.showYesNoQuestionDialog("Second Smart Filter", msg);
        if (replace) {
            Filter newFilter = createNewInstanceFilter();
            so.replaceSmartFilter(newFilter);
        }
    }

    private Filter createNewInstanceFilter() {
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

    // overrides the constructor parameter
    // a bit ugly, but it simplifies the builders
    public FilterAction noGUI() {
        hasDialog = false;
        menuName = name; // without the "..."
        setText(menuName);

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
        if (pf.excludedFromAnimation()) {
            return false;
        }
        if (!pf.getParamSet().canBeAnimated()) {
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
        if (!getName().equals(filterAction.getName())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Serial
    private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
        throw new IOException("should not be serialized");
    }

    @Override
    public String toString() {
        return getName();
    }
}

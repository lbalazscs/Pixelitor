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

package pixelitor.filters;

import pixelitor.filters.gui.*;
import pixelitor.layers.Filterable;
import pixelitor.utils.debug.DebugNode;

import java.io.Serial;
import java.util.List;
import java.util.Objects;

/**
 * Abstract filter class that uses a {@link ParamSet} to manage
 * its settings. Subclasses only need to define a ParamSet,
 * and the GUI will be built automatically.
 */
public abstract class ParametrizedFilter extends FilterWithGUI {
    @Serial
    private static final long serialVersionUID = 3796358314893014182L;

    protected final transient ParamSet paramSet;

    // true if a "Show Original" checkbox should be available.
    private final transient boolean addShowOriginal;

    protected ParametrizedFilter(boolean addShowOriginal) {
        this.addShowOriginal = addShowOriginal;
        paramSet = new ParamSet();
    }

    @Override
    public void randomize() {
        paramSet.randomize();
    }

    @Override
    public FilterGUI createGUI(Filterable layer, boolean reset) {
        return new ParametrizedFilterGUI(this, layer, addShowOriginal, reset);
    }

    /**
     * Sets the filter parameters from a single {@link FilterParam}.
     */
    public ParamSet setParams(FilterParam param) {
        paramSet.addParam(param);
        paramSet.addCommonActions(isComplex());
        return paramSet;
    }

    /**
     * Sets the filter parameters from multiple {@link FilterParam}s.
     */
    public ParamSet setParams(FilterParam... params) {
        paramSet.addParams(params);
        paramSet.addCommonActions(isComplex());
        return paramSet;
    }

    /**
     * Adds multiple parameters to the filter's ParamSet.
     */
    public void addParams(FilterParam... params) {
        paramSet.addParams(params);
    }

    /**
     * Adds multiple parameters to the beginning of the filter's ParamSet.
     * They will appear above the existing controls in the generated GUI.
     */
    public void addParamsToFront(FilterParam... params) {
        paramSet.addParamsToFront(params);
    }

    /**
     * Inserts a parameter at a specified index in the filter's ParamSet.
     */
    public void insertParam(FilterParam param, int index) {
        paramSet.insertParam(param, index);
    }

    public ParamSet getParamSet() {
        return paramSet;
    }

    /**
     * Returns true if this filter supports tween animations.
     */
    public boolean supportsTweenAnimation() {
        return true;
    }

    @Override
    public boolean hasBuiltinPresets() {
        return paramSet.hasBuiltinPresets();
    }

    @Override
    public Preset[] getBuiltinPresets() {
        return paramSet.getBuiltinPresets();
    }

    @Override
    public boolean canHaveUserPresets() {
        // trivial filters don't have a "Save Preset" menu
        // option, but they still might be smart filters
        return paramSet.isNonTrivial();
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        paramSet.saveStateTo(preset);
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        paramSet.loadUserPreset(preset);
    }

    @Override
    public void loadFilterState(FilterState filterState, boolean reset) {
        paramSet.applyState(filterState, reset);
    }

    @Override
    public String paramsAsString() {
        UserPreset preset = new UserPreset(getName(), "Debug");
        paramSet.saveStateTo(preset);
        return preset.toString();
    }

    public void set(String paramName, String value) {
        paramSet.set(paramName, value);
    }

    /**
     * Checks if this filter has multiple parameters or a single complex one.
     */
    public boolean isComplex() {
        List<FilterParam> params = paramSet.getParams();
        if (params.size() > 1) {
            return true;
        }

        FilterParam singleParam = params.getFirst();
        return singleParam.isComplex();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParametrizedFilter that = (ParametrizedFilter) o;
        return Objects.equals(paramSet, that.paramSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paramSet);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);
        node.add(paramSet.createDebugNode("paramSet"));
        return node;
    }
}

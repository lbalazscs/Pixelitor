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
 * A filter that keeps its settings in a ParamSet object
 * The advantage is that subclasses don't need to create their own adjustment GUIs,
 * they only specify their ParamSet, and the GUI is built automatically
 */
public abstract class ParametrizedFilter extends FilterWithGUI {
    @Serial
    private static final long serialVersionUID = 3796358314893014182L;

    protected final transient ParamSet paramSet;

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

    public ParamSet setParams(FilterParam param) {
        paramSet.addParam(param);
        paramSet.addCommonActions(isNonTrivial());
        return paramSet;
    }

    public ParamSet setParams(FilterParam... params) {
        paramSet.addParams(params);
        paramSet.addCommonActions(isNonTrivial());
        return paramSet;
    }

    public ParamSet getParamSet() {
        return paramSet;
    }

    /**
     * Returns true if this filter should be included
     * into the list of tween animation filters.
     */
    public boolean supportsTweenAnimation() {
        return true;
    }

    public void addParams(FilterParam... params) {
        paramSet.addParams(params);
    }

    public void addParamsToFront(FilterParam... params) {
        paramSet.addParamsToFront(params);
    }

    public void insertParam(FilterParam param, int index) {
        paramSet.insertParam(param, index);
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
        return paramSet.canHaveUserPresets();
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

    public boolean isNonTrivial() {
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

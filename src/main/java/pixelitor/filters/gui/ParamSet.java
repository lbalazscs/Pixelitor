/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.ParametrizedFilter;
import pixelitor.layers.Filterable;
import pixelitor.utils.Icons;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.Debuggable;

import java.util.*;

import static java.util.Locale.Category.FORMAT;
import static pixelitor.filters.gui.FilterSetting.EnabledReason.FINAL_ANIMATION_SETTING;

/**
 * All the information needed to automatically
 * build the user interface of a {@link ParametrizedFilter}
 */
public class ParamSet implements Debuggable {
    private final List<FilterParam> paramList = new ArrayList<>();
    private final List<FilterButtonModel> actionList = new ArrayList<>(3);
    private ParamAdjustmentListener adjustmentListener;
    private Runnable afterResetAction;
    private Preset[] builtinPresets;
    private boolean nonTrivialFilter;

    public ParamSet(FilterParam... params) {
        paramList.addAll(List.of(params));
    }

    public ParamSet(FilterParam param) {
        paramList.add(param);
    }

    public ParamSet(List<FilterParam> params) {
        paramList.addAll(params);
    }

    /**
     * Adds the given parameters after the existing ones
     */
    public void addParams(FilterParam[] params) {
        Collections.addAll(paramList, params);
    }

    /**
     * Adds the given parameters before the existing ones
     */
    public void addParamsToFront(FilterParam[] params) {
        paramList.addAll(0, Arrays.asList(params));
    }

    public void insertParam(FilterParam param, int index) {
        paramList.add(index, param);
    }

    public void insertAction(FilterButtonModel action, int index) {
        actionList.add(index, action);
    }

    public ParamSet withActions(FilterButtonModel... actions) {
        actionList.addAll(List.of(actions));
        return this;
    }

    public ParamSet withActionsAtFront(FilterButtonModel... actions) {
        for (FilterButtonModel action : actions) {
            actionList.add(0, action);
        }
        return this;
    }

    public ParamSet withAction(FilterButtonModel action) {
        actionList.add(action);
        return this;
    }

    public void addCommonActions(boolean nonTrivial) {
        nonTrivialFilter = nonTrivial;
        addRandomizeAction();
        addResetAllAction();
    }

    private void addRandomizeAction() {
        var randomizeAction = new FilterButtonModel("Randomize Settings",
            this::randomize,
            Icons.getDiceIcon(),
            "Randomize the settings for this filter.",
            "randomize");
        actionList.add(randomizeAction);
    }

    private void addResetAllAction() {
        var resetAllAction = new FilterButtonModel("Reset All",
            this::reset,
            Icons.getWestArrowIcon(),
            Resettable.RESET_ALL_TOOLTIP,
            "resetAll");
        actionList.add(resetAllAction);
    }

    /**
     * Allows registering an action that will run after "reset all"
     */
    public void setAfterResetAllAction(Runnable afterResetAction) {
        this.afterResetAction = afterResetAction;
    }

    /**
     * Resets all params without triggering the filter
     */
    public void reset() {
        for (FilterParam param : paramList) {
            param.reset(false);
        }
        if (afterResetAction != null) {
            afterResetAction.run();
        }
    }

    public void randomize() {
        long before = Filter.runCount;

        paramList.forEach(FilterParam::randomize);

        // the filter is not supposed to be triggered
        long after = Filter.runCount;
        assert before == after : "before = " + before + ", after = " + after;
    }

    public void runFilter() {
        if (adjustmentListener != null) {
            adjustmentListener.paramAdjusted();
        }
    }

    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        adjustmentListener = listener;

        for (FilterParam param : paramList) {
            param.setAdjustmentListener(listener);
        }
        for (FilterButtonModel action : actionList) {
            action.setAdjustmentListener(listener);
        }
    }

    public void updateOptions(Filterable layer, boolean changeValue) {
        for (FilterParam param : paramList) {
            param.updateOptions(layer, changeValue);
        }
    }

    /**
     * A ParamSet can be animated if at least
     * one contained filter parameter can be
     */
    public boolean isAnimatable() {
        for (FilterParam param : paramList) {
            if (param.isAnimatable()) {
                return true;
            }
        }
        return false;
    }

    public void setFinalAnimationSettingMode(boolean b) {
        for (FilterParam param : paramList) {
            param.setEnabled(!b, FINAL_ANIMATION_SETTING);
        }
        for (FilterButtonModel action : actionList) {
            action.setEnabled(!b, FINAL_ANIMATION_SETTING);
        }
    }

    public boolean hasGradient() {
        for (FilterParam param : paramList) {
            if (param instanceof GradientParam) {
                return true;
            }
        }
        return false;
    }

    public List<FilterButtonModel> getActions() {
        return actionList;
    }

    public List<FilterParam> getParams() {
        return paramList;
    }

    public FilterState copyState(boolean animOnly) {
        return new FilterState(this, animOnly);
    }

    /**
     * Sets the state without triggering the filter.
     */
    public void setState(FilterState newStateSet, boolean forAnimation) {
        for (FilterParam param : paramList) {
            if (forAnimation && !param.isAnimatable()) {
                continue;
            }
            String name = param.getName();
            ParamState<?> newState = newStateSet.get(name);

            if (newState != null) { // a preset doesn't have to contain all key-value pairs
                param.loadStateFrom(newState, !forAnimation);
            }
        }
    }

    public void applyState(FilterState preset, boolean reset) {
        if (reset) {
            reset();
        }
        setState(preset, false);
        runFilter();
    }

    public void setBuiltinPresets(Preset... presets) {
        this.builtinPresets = presets;
    }

    public Preset[] getBuiltinPresets() {
        return builtinPresets;
    }

    public boolean hasBuiltinPresets() {
        return builtinPresets != null;
    }

    public boolean canHaveUserPresets() {
        return nonTrivialFilter;
    }

    public void loadUserPreset(UserPreset preset) {
        long runCountBefore = Filter.runCount;
        for (FilterParam param : paramList) {
            param.loadStateFrom(preset);
        }

        // check that the loading didn't trigger the filter
        assert Filter.runCount == runCountBefore :
            "runCountBefore = " + runCountBefore + ", runCount = " + Filter.runCount;

        runFilter();
    }

    public void saveStateTo(UserPreset preset) {
        Locale locale = Locale.getDefault(FORMAT);
        try {
            Locale.setDefault(FORMAT, Locale.US);
            for (FilterParam param : paramList) {
                param.saveStateTo(preset);
            }
        } finally {
            Locale.setDefault(FORMAT, locale);
        }
    }

    public void set(String paramName, String value) {
        FilterParam modified = null;
        for (FilterParam param : paramList) {
            if (param.getName().equals(paramName)) {
                modified = param;
            }
        }
        if (modified == null) {
            throw new IllegalStateException("No param called " + paramName);
        }
        modified.loadStateFrom(value);
    }

    @Override
    public String toString() {
        return "ParamSet {" + paramList.toString() + "}";
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key, this);

        for (FilterParam param : paramList) {
            node.add(param.createDebugNode(param.getName()));
        }

        return node;
    }
}

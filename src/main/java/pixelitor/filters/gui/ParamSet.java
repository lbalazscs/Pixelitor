/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.Icons;
import pixelitor.utils.Utils;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static pixelitor.filters.gui.FilterSetting.EnabledReason.FINAL_ANIMATION_SETTING;

/**
 * A fixed set of filter parameter objects
 */
public class ParamSet {
    private List<FilterParam> paramList = new ArrayList<>();
    private final List<FilterAction> actionList = new ArrayList<>(3);
    private ParamAdjustmentListener adjustmentListener;
    private Runnable beforeResetAction;

    public ParamSet(FilterParam... params) {
        paramList.addAll(Arrays.asList(params));
    }

    public ParamSet(FilterParam param) {
        paramList.add(param);
    }

    public ParamSet(List<FilterParam> params) {
        paramList.addAll(params);
    }

    public ParamSet withActions(FilterAction... actions) {
        actionList.addAll(Arrays.asList(actions));
        return this;
    }

    public ParamSet withAction(FilterAction action) {
        actionList.add(action);
        return this;
    }

    public ParamSet addCommonActions(FilterAction... actions) {
        for (FilterAction action : actions) {
            if (action != null) {
                actionList.add(action);
            }
        }

        // no need for "randomize"/"reset all"
        // if the filter has only one parameter...
        boolean addRandomizeAndResetAll = paramList.size() > 1;

        if (!addRandomizeAndResetAll) {
            FilterParam param = paramList.get(0);
            // ...except if that single parameter is grouped...
            if (param instanceof GroupedRangeParam) {
                addRandomizeAndResetAll = true;
            }
            // ...or it is a gradient param
            if (param instanceof GradientParam) {
                addRandomizeAndResetAll = true;
            }
        }
        if (addRandomizeAndResetAll) {
            addRandomizeAction();
            addResetAllAction();
        }
        return this;
    }

    private void addRandomizeAction() {
        FilterAction randomizeAction = new FilterAction("Randomize Settings",
                e -> randomize(),
                Icons.getDiceIcon(),
                "Randomize the settings for this filter.",
                "randomize");
        actionList.add(randomizeAction);
    }

    private void addResetAllAction() {
        FilterAction resetAllAction = new FilterAction("Reset All",
                e -> reset(),
                Icons.getWestArrowIcon(),
                "Reset all settings to their default values.",
                "resetAll");
        actionList.add(resetAllAction);
    }

    public void insertParam(FilterParam param, int index) {
        paramList.add(index, param);
    }

    public void insertAction(FilterAction action, int index) {
        actionList.add(index, action);
    }

    /**
     * Resets all params without triggering the filter
     */
    public void reset() {
        if (beforeResetAction != null) {
            beforeResetAction.run();
        }
        for (FilterParam param : paramList) {
            param.reset(false);
        }
    }

    public void randomize() {
        long before = Filter.runCount;

        paramList.forEach(FilterParam::randomize);

        // this call is not supposed to trigger the filter!
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
        for (FilterAction action : actionList) {
            action.setAdjustmentListener(listener);
        }
    }

    public void considerImageSize(Rectangle bounds) {
        for (FilterParam param : paramList) {
            param.considerImageSize(bounds);
        }
    }

    public ParamSetState copyState() {
        return new ParamSetState(this);
    }

    public void setState(ParamSetState newState) {
        Iterator<ParamState> newStateIterator = newState.iterator();
        paramList.stream()
                .filter(FilterParam::canBeAnimated)
                .forEach(param -> {
                    ParamState newParamState = newStateIterator.next();
                    param.setState(newParamState);
                });
    }

    /**
     * A ParamSet can be animated if at least
     * one contained filter parameter can be
     */
    public boolean canBeAnimated() {
        return Utils.anyMatch(paramList, FilterParam::canBeAnimated);
    }

    public void setFinalAnimationSettingMode(boolean b) {
        for (FilterParam param : paramList) {
            param.setEnabled(!b, FINAL_ANIMATION_SETTING);
        }
        for (FilterAction action : actionList) {
            action.setEnabled(!b, FINAL_ANIMATION_SETTING);
        }
    }

    public boolean hasGradient() {
        return Utils.anyMatch(paramList, p -> p instanceof GradientParam);
    }

    public List<FilterAction> getActions() {
        return actionList;
    }

    public List<FilterParam> getParams() {
        return paramList;
    }

    @Override
    public String toString() {
        String s = "ParamSet[";
        for (FilterParam param : paramList) {
            s += ("\n    " + param.toString());
        }
        s += "\n]";
        return s;
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
        List<FilterParam> old = paramList;
        paramList = new ArrayList<>(params.length + old.size());
        Collections.addAll(paramList, params);
        paramList.addAll(old);
    }

    /**
     * Allows registering an action that will run before "reset all"
     */
    public void setBeforeResetAction(Runnable beforeResetAction) {
        this.beforeResetAction = beforeResetAction;
    }
}

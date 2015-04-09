/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
import pixelitor.utils.IconUtils;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * A fixed set of filter parameter objects
 */
public class ParamSet {
    private final List<FilterParam> paramList = new ArrayList<>();
    private final List<FilterAction> actionList = new ArrayList<>(3);
    private ParamAdjustmentListener adjustmentListener;

    public ParamSet(FilterParam... params) {
        paramList.addAll(Arrays.asList(params));
    }

    public ParamSet(FilterParam param) {
        paramList.add(param);
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
        if (paramList.size() > 1) { // no need for "randomize"/"reset all" if the filter has only one parameter
            for (FilterAction action : actions) {
                if(action != null) {
                    actionList.add(action);
                }
            }
            addRandomizeAction();
            addResetAllAction();
        }
        return this;
    }

    private void addRandomizeAction() {
        FilterAction randomizeAction = new FilterAction("Randomize Settings", e -> randomize(), "Randomize the settings for this filter.");
        actionList.add(randomizeAction);
    }

    private void addResetAllAction() {
        FilterAction resetAllAction = new FilterAction("Reset All", e -> reset(), IconUtils.getWestArrowIcon(), "Reset all settings to their default values.");
        actionList.add(resetAllAction);
    }

    public void insertParam(FilterParam param, int index) {
        paramList.add(index, param);
    }

    public void insertAction(FilterAction action, int index) {
        actionList.add(index, action);
    }

    /**
     * Resets all params without triggering an operation
     */
    public void reset() {
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

    public void triggerFilter() {
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
     * A ParamSet can be animated if at least one contained filter parameter can be
     */
    public boolean canBeAnimated() {
        for (FilterParam param : paramList) {
            if (param.canBeAnimated()) {
                return true;
            }
        }
        return false;
    }

    public void setFinalAnimationSettingMode(boolean b) {
        for (FilterParam param : paramList) {
            param.setFinalAnimationSettingMode(b);
        }
        for (FilterAction action : actionList) {
            action.setFinalAnimationSettingMode(b);
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

    public List<FilterAction> getActionList() {
        return actionList;
    }

    public List<FilterParam> getParamList() {
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
}

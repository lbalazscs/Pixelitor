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

package pixelitor.filters.gui;

import pixelitor.filters.Filter;
import pixelitor.utils.IconUtils;

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
    private final List<ActionSetting> actionList = new ArrayList<>(3);
    private ParamAdjustmentListener adjustmentListener;

    public ParamSet(FilterParam... params) {
        paramList.addAll(Arrays.asList(params));
    }

    public ParamSet(FilterParam param) {
        paramList.add(param);
    }

    public ParamSet(List<FilterParam> params) {
        paramList.addAll(params);
    }

    public ParamSet withActions(ActionSetting... actions) {
        actionList.addAll(Arrays.asList(actions));
        return this;
    }

    public ParamSet withAction(ActionSetting action) {
        actionList.add(action);
        return this;
    }

    public ParamSet addCommonActions(ActionSetting... actions) {
        if (paramList.size() > 1) { // no need for "randomize"/"reset all" if the filter has only one parameter
            for (ActionSetting action : actions) {
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
        ActionSetting randomizeAction = new ActionSetting("Randomize Settings",
                e -> randomize(),
                IconUtils.getDiceIcon(),
                "Randomize the settings for this filter.");
        actionList.add(randomizeAction);
    }

    private void addResetAllAction() {
        ActionSetting resetAllAction = new ActionSetting("Reset All",
                e -> reset(),
                IconUtils.getWestArrowIcon(),
                "Reset all settings to their default values.");
        actionList.add(resetAllAction);
    }

    public void insertParam(FilterParam param, int index) {
        paramList.add(index, param);
    }

    public void insertAction(ActionSetting action, int index) {
        actionList.add(index, action);
    }

    /**
     * Resets all params without triggering the filter
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
        for (ActionSetting action : actionList) {
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
            param.setEnabled(!b, FINAL_ANIMATION_SETTING);
        }
        for (ActionSetting action : actionList) {
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

    public List<ActionSetting> getActionList() {
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

    public void addParamsToFront(FilterParam[] params) {
        List<FilterParam> old = paramList;
        paramList = new ArrayList<>(params.length + old.size());
        Collections.addAll(paramList, params);
        paramList.addAll(old);
    }
}

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
 * A fixed set of GUIParam objects
 */
public class ParamSet implements Iterable<GUIParam> {
    private final List<GUIParam> paramList = new ArrayList<>();
    private ParamAdjustmentListener adjustmentListener;

    public ParamSet(GUIParam... params) {
        paramList.addAll(Arrays.asList(params));
    }

    public ParamSet(GUIParam param) {
        paramList.add(param);
    }

    public void addCommonActions(GUIParam... actions ) {
        if (paramList.size() > 1) { // no need for "randomize"/"reset all" if the filter has only one parameter
            for (GUIParam action : actions) {
                if(action != null) {
                    paramList.add(action);
                }
            }
            addRandomizeAction();
            addResetAllAction();
        }
    }

    private void addRandomizeAction() {
        GUIParam randomizeAction = new ActionParam("Randomize Settings", e -> randomize(), "Randomize the settings for this filter.");
        paramList.add(randomizeAction);
    }

    private void addResetAllAction() {
        GUIParam resetAllAction = new ActionParam("Reset All", e -> reset(), IconUtils.getWestArrowIcon(), "Reset all settings to their default values.");
        paramList.add(resetAllAction);
    }

    public void insertParam(GUIParam param, int index) {
        paramList.add(index, param);
    }

    @Override
    public Iterator<GUIParam> iterator() {
        return paramList.iterator();
    }

    /**
     * Resets all params without triggering an operation
     */
    public void reset() {
        for (GUIParam param : paramList) {
            param.reset(false);
        }
    }

    public void randomize() {
        long before = Filter.runCount;

        paramList.forEach(GUIParam::randomize);

        // this call is not supposed to trigger the filter!
        long after = Filter.runCount;
        assert before == after : "before = " + before + ", after = " + after;
    }

    public void startPresetAdjusting() {
        for (GUIParam param : paramList) {
            param.setTrigger(false);
        }
    }

    public void endPresetAdjusting(boolean trigger) {
        for (GUIParam param : paramList) {
            param.setTrigger(true);
        }
        if (trigger) {
            if (adjustmentListener != null) {
                // called only once, not for each GUIParam
                adjustmentListener.paramAdjusted();
            }
        }
    }

    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        adjustmentListener = listener;

        for (GUIParam param : paramList) {
            param.setAdjustmentListener(listener);
        }
    }

    public void considerImageSize(Rectangle bounds) {
        for (GUIParam param : paramList) {
            param.considerImageSize(bounds);
        }
    }

    public ParamSetState copyState() {
        return new ParamSetState(this);
    }

    public void setState(ParamSetState newState) {
        Iterator<ParamState> newStateIterator = newState.iterator();
        paramList.stream()
                .filter(GUIParam::canBeAnimated)
                .forEach(param -> {
                    ParamState newParamState = newStateIterator.next();
                    param.setState(newParamState);
                });
    }

    /**
     * A ParamSet can be animated if at least one contained GUIParam can be
     */
    public boolean canBeAnimated() {
        for (GUIParam param : paramList) {
            if (param.canBeAnimated()) {
                return true;
            }
        }
        return false;
    }

    public void setFinalAnimationSettingMode(boolean b) {
        for (GUIParam param : paramList) {
            param.setFinalAnimationSettingMode(b);
        }
    }

    public boolean hasGradient() {
        for (GUIParam param : paramList) {
            if (param instanceof GradientParam) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        String s = "ParamSet[";
        for (GUIParam param : paramList) {
            s += ("\n    " + param.toString());
        }
        s += "\n]";
        return s;
    }
}

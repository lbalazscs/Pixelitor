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

import com.jhlabs.image.ImageMath;

import javax.swing.*;
import java.awt.Rectangle;

import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;

/**
 * Two or more range params that are grouped and can be linked.
 */
public class GroupedRangeParam extends AbstractFilterParam implements RangeBasedOnImageSize {
    private final RangeParam[] rangeParams;
    private final ButtonModel checkBoxModel;
    private final boolean linkedByDefault;
    private boolean linkable = true; // whether a "Linked" checkbox appears

    /**
     * 2 linked params: "Horizontal" and "Vertical", linked by default
     */
    public GroupedRangeParam(String name, int minValue, int defaultValue, int maxValue) {
        this(name, minValue, defaultValue, maxValue, true);
    }

    /**
     * 2 linked params: "Horizontal" and "Vertical"
     */
    public GroupedRangeParam(String name, int minValue, int defaultValue, int maxValue, boolean linked) {
        this(name, "Horizontal", "Vertical", minValue, defaultValue, maxValue, linked);
    }

    /**
     * 2 linked params
     */
    public GroupedRangeParam(String name, String firstRangeName, String secondRangeName, int minValue, int defaultValue, int maxValue, boolean linked) {
        this(name, new String[]{firstRangeName, secondRangeName}, minValue, defaultValue, maxValue, linked);
    }

    /**
     * Any number of linked params
     */
    public GroupedRangeParam(String name, String[] rangeNames, int minValue, int defaultValue, int maxValue, boolean linked) {
        this(name, createParams(rangeNames, minValue, defaultValue, maxValue), linked);
    }

    public GroupedRangeParam(String name, RangeParam[] params, boolean linked) {
        super(name, ALLOW_RANDOMIZE);
        rangeParams = params;

        checkBoxModel = new JToggleButton.ToggleButtonModel();

        this.linkedByDefault = linked;
        setLinked(linkedByDefault);

        linkParams();
    }

    @Override
    public JComponent createGUI() {
        GroupedRangeSelector selector = new GroupedRangeSelector(this);
        paramGUI = selector;
        setParamGUIEnabledState();
        return selector;
    }

    private void linkParams() {
        for (RangeParam param : rangeParams) {
            param.addChangeListener(e -> {
                if (isLinked()) {
                    // set the value of every other param to the value of the current param
                    for (RangeParam otherParam : rangeParams) {
                        if (otherParam != param) {
                            int newValue = param.getValue();
                            otherParam.setValueNoTrigger(newValue);
                        }
                    }
                }
            });
        }
    }

    public ButtonModel getCheckBoxModel() {
        return checkBoxModel;
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        for (RangeParam param : rangeParams) {
            param.setAdjustmentListener(listener);
        }
        adjustmentListener = listener;
    }

    public int getValue(int index) {
        return rangeParams[index].getValue();
    }

    public float getValueAsFloat(int index) {
        return rangeParams[index].getValueAsFloat();
    }

    public double getValueAsDouble(int index) {
        return rangeParams[index].getValueAsDouble();
    }

    public void setValue(int index, int newValue) {
        rangeParams[index].setValue(newValue);
        // if linked, the others will be set automatically
    }

    public boolean isLinked() {
        return checkBoxModel.isSelected();
    }

    public void setLinked(boolean linked) {
        checkBoxModel.setSelected(linked);
    }

    @Override
    public int getNrOfGridBagCols() {
        return 1;
    }

    @Override
    public void randomize() {
        if (isLinked()) {
            rangeParams[0].randomize();
        } else {
            for (RangeParam param : rangeParams) {
                param.randomize();
            }
        }
    }

    @Override
    public boolean isSetToDefault() {
        if (isLinked() != linkedByDefault) {
            return false;
        }
        for (RangeParam param : rangeParams) {
            if (!param.isSetToDefault()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void reset(boolean triggerAction) {
        for (RangeParam param : rangeParams) {
            // call the individual params without trigger...
            param.reset(false);
        }

        // ... and then trigger only once
        if (triggerAction) {
            adjustmentListener.paramAdjusted();
        }

        setLinked(linkedByDefault);
    }

    public RangeParam getRangeParam(int index) {
        return rangeParams[index];
    }

    @Override
    public void considerImageSize(Rectangle bounds) {
        for (RangeParam param : rangeParams) {
            param.considerImageSize(bounds);
        }
    }

    @Override
    public GroupedRangeParam adjustRangeToImageSize(double ratio) {
        for (RangeParam param : rangeParams) {
            param.adjustRangeToImageSize(ratio);
        }
        return this;
    }

    public float getValueAsPercentage(int index) {
        return rangeParams[index].getValueAsPercentage();
    }

    public int getNumParams() {
        return rangeParams.length;
    }

    public GroupedRangeParam setShowLinkedCB(boolean linkable) {
        this.linkable = linkable;
        return this;
    }

    public boolean isLinkable() {
        return linkable;
    }

    @Override
    public boolean canBeAnimated() {
        return true;
    }

    @Override
    public ParamState copyState() {
        int numParams = rangeParams.length;
        double[] values = new double[numParams];
        for (int i = 0; i < numParams; i++) {
            values[i] = rangeParams[i].getValue();
        }

        return new GRState(values);
    }

    @Override
    public void setState(ParamState state) {
        GRState grState = (GRState) state;
        double[] values = grState.values;
        for (int i = 0; i < values.length; i++) {
            double value = values[i];
            rangeParams[i].setValueAsDouble(value);
        }
    }

    private static class GRState implements ParamState {
        private final double[] values;

        public GRState(double[] values) {
            this.values = values;
        }

        @Override
        public ParamState interpolate(ParamState endState, double progress) {
            GRState apEndState = (GRState) endState;

            double[] interpolatedValues = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                interpolatedValues[i] = ImageMath.lerp(progress, values[i], apEndState.values[i]);
            }

            return new GRState(interpolatedValues);
        }
    }

    private static RangeParam[] createParams(String[] rangeNames, int minValue, int defaultValue, int maxValue) {
        RangeParam[] rangeParams = new RangeParam[rangeNames.length];
        for (int i = 0; i < rangeNames.length; i++) {
            String rangeName = rangeNames[i];
            rangeParams[i] = new RangeParam(rangeName, minValue, defaultValue, maxValue);
        }
        return rangeParams;
    }
}

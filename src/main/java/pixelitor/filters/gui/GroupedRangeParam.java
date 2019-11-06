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

import com.jhlabs.image.ImageMath;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.Rectangle;
import java.util.Arrays;

import static java.util.stream.Collectors.joining;
import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;

/**
 * Two or more {@link RangeParam} objects that are grouped visually in the GUI
 * and can be linked to move together.
 */
public class GroupedRangeParam extends AbstractFilterParam {
    private final RangeParam[] rangeParams;
    private final ButtonModel checkBoxModel;
    private final boolean linkedByDefault;
    private boolean linkable = true; // whether a "Linked" checkbox appears

    /**
     * Two linked params: "Horizontal" and "Vertical", with shared min/max/default values
     */
    public GroupedRangeParam(String name, int min, int def, int max) {
        this(name, min, def, max, true);
    }

    /**
     * Two params: "Horizontal" and "Vertical", with shared min/max/default values
     */
    public GroupedRangeParam(String name, int min, int def, int max, boolean linked) {
        this(name, "Horizontal", "Vertical", min, def, max, linked);
    }

    /**
     * Two params with custom names and shared min/max/default values
     */
    public GroupedRangeParam(String name, String firstChildName, String secondChildName,
                             int min, int def, int max, boolean linked) {
        this(name, new String[]{firstChildName, secondChildName}, min, def, max, linked);
    }

    /**
     * Any number of params with shared min/max/default values
     */
    public GroupedRangeParam(String name, String[] childNames,
                             int min, int def, int max, boolean linked) {
        this(name, createParams(childNames, min, def, max), linked);
    }

    /**
     * The most generic constructor: any number of params that can differ
     * in their min/max/default values (but linking makes sense only if they
     * have the same ranges).
     */
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
        GroupedRangeParamGUI gui = new GroupedRangeParamGUI(this);
        paramGUI = gui;
        setParamGUIEnabledState();
        return gui;
    }

    private static RangeParam[] createParams(String[] names,
                                             int min, int def, int max) {
        RangeParam[] rangeParams = new RangeParam[names.length];
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            rangeParams[i] = new RangeParam(name, min, def, max);
        }
        return rangeParams;
    }

    private void linkParams() {
        for (RangeParam param : rangeParams) {
            param.addChangeListener(e -> onParamChange(param));
        }
    }

    private void onParamChange(RangeParam param) {
        if (isLinked()) {
            // set the value of every other param to the value of the changed param
            for (RangeParam other : rangeParams) {
                if (other != param) {
                    int newValue = param.getValue();
                    other.setValueNoTrigger(newValue);
                }
            }
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
    public int getNumGridBagCols() {
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
        return Utils.allMatch(rangeParams,
                RangeParam::isSetToDefault);
    }

    @Override
    public void reset(boolean trigger) {
        for (RangeParam param : rangeParams) {
            // call the individual params without trigger...
            param.reset(false);
        }

        // ... and then trigger only once
        if (trigger) {
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

    public GroupedRangeParam withAdjustedRange(double ratio) {
        for (RangeParam param : rangeParams) {
            param.withAdjustedRange(ratio);
        }
        return this;
    }

    public float getValueAsPercentage(int index) {
        return rangeParams[index].getValueAsPercentage();
    }

    public int getNumParams() {
        return rangeParams.length;
    }

    public GroupedRangeParam notLinkable() {
        this.linkable = false;
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
    public String toString() {
        String rangeStrings = Arrays.stream(rangeParams)
                .map(RangeParam::toString)
                .collect(joining(",", "[", "]"));

        return getClass().getSimpleName() + rangeStrings;
    }

    @Override
    public ParamState copyState() {
        double[] values = Arrays.stream(rangeParams)
                .mapToDouble(RangeParam::getValue)
                .toArray();

        return new GRState(values);
    }

    @Override
    public void setState(ParamState state) {
        GRState grState = (GRState) state;
        double[] values = grState.values;
        for (int i = 0; i < values.length; i++) {
            double value = values[i];
            rangeParams[i].setValueNoGUI(value);
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
                interpolatedValues[i] = ImageMath.lerp(
                        progress, values[i], apEndState.values[i]);
            }

            return new GRState(interpolatedValues);
        }
    }
}

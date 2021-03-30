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

package pixelitor.filters.gui;

import com.jhlabs.image.ImageMath;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;

/**
 * Two or more {@link RangeParam} objects that are grouped
 * visually in the GUI and can be linked to move together.
 */
public class GroupedRangeParam extends AbstractFilterParam {
    private final RangeParam[] children;
    private final ButtonModel checkBoxModel;
    private final boolean linkedByDefault;
    private boolean linkable = true; // whether a "Linked" checkbox appears

    /**
     * Two linked children: "Horizontal" and "Vertical", with shared min/max/default values
     */
    public GroupedRangeParam(String name, int min, int def, int max) {
        this(name, min, def, max, true);
    }

    /**
     * Two children: "Horizontal" and "Vertical", with shared min/max/default values
     */
    public GroupedRangeParam(String name, int min, int def, int max, boolean linked) {
        this(name, "Horizontal", "Vertical", min, def, max, linked);
    }

    /**
     * Two children with custom names and shared min/max/default values
     */
    public GroupedRangeParam(String name, String firstChildName, String secondChildName,
                             int min, int def, int max, boolean linked) {
        this(name, new String[]{firstChildName, secondChildName}, min, def, max, linked);
    }

    /**
     * Any number of children with shared min/max/default values
     */
    public GroupedRangeParam(String name, String[] childNames,
                             int min, int def, int max, boolean linked) {
        this(name, createChildren(childNames, min, def, max), linked);
    }

    /**
     * The most generic constructor: any number of children that can differ
     * in their min/max/default values. Linking makes sense only if they
     * have the same ranges.
     */
    public GroupedRangeParam(String name, RangeParam[] children, boolean linked) {
        super(name, ALLOW_RANDOMIZE);
        this.children = children;

        checkBoxModel = new JToggleButton.ToggleButtonModel();

        linkedByDefault = linked;
        setLinked(linkedByDefault);

        linkChildren();
    }

    @Override
    public JComponent createGUI() {
        var gui = new GroupedRangeParamGUI(this);
        paramGUI = gui;
        setGUIEnabledState();
        return gui;
    }

    private static RangeParam[] createChildren(String[] names,
                                               int min, int def, int max) {
        RangeParam[] children = new RangeParam[names.length];
        for (int i = 0; i < names.length; i++) {
            children[i] = new RangeParam(names[i], min, def, max);
        }
        return children;
    }

    private void linkChildren() {
        for (RangeParam child : children) {
            child.addChangeListener(e -> propagateChange(child));
        }
    }

    private void propagateChange(RangeParam param) {
        if (isLinked()) {
            // set the value of every other child to the value of the changed child
            for (RangeParam other : children) {
                if (other != param) {
                    double newValue = param.getValueAsDouble();
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
        for (RangeParam child : children) {
            child.setAdjustmentListener(listener);
        }
        adjustmentListener = listener;
    }

    public int getValue(int index) {
        return children[index].getValue();
    }

    public float getValueAsFloat(int index) {
        return children[index].getValueAsFloat();
    }

    public double getValueAsDouble(int index) {
        return children[index].getValueAsDouble();
    }

    public void setValue(int index, int newValue) {
        children[index].setValue(newValue);
        // if linked, the others will be set automatically
    }

    public boolean isLinked() {
        return checkBoxModel.isSelected();
    }

    public void setLinked(boolean linked) {
        checkBoxModel.setSelected(linked);
    }

    @Override
    protected void doRandomize() {
        if (isLinked()) {
            children[0].randomize();
        } else {
            for (RangeParam child : children) {
                child.randomize();
            }
        }
    }

    @Override
    public boolean isSetToDefault() {
        if (isLinked() != linkedByDefault) {
            return false;
        }
        return Utils.allMatch(children, RangeParam::isSetToDefault);
    }

    @Override
    public void reset(boolean trigger) {
        for (RangeParam child : children) {
            // call the individual params without trigger...
            child.reset(false);
        }

        // ... and then trigger only once
        if (trigger) {
            adjustmentListener.paramAdjusted();
        }

        setLinked(linkedByDefault);
    }

    public RangeParam getRangeParam(int index) {
        return children[index];
    }

    @Override
    public void adaptToImageSize(Dimension size) {
        for (RangeParam child : children) {
            child.adaptToImageSize(size);
        }
    }

    public GroupedRangeParam withAdjustedRange(double ratio) {
        for (RangeParam child : children) {
            child.withAdjustedRange(ratio);
        }
        return this;
    }

    public float getValueAsPercentage(int index) {
        return children[index].getPercentageValF();
    }

    public double getValueAsDPercentage(int index) {
        return children[index].getPercentageValD();
    }

    public int getNumParams() {
        return children.length;
    }

    public GroupedRangeParam notLinkable() {
        linkable = false;
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
        String childStrings = Arrays.stream(children)
            .map(RangeParam::toString)
            .collect(joining(",", "[", "]"));

        return getClass().getSimpleName() + childStrings;
    }

    @Override
    public GroupedRangeParamState copyState() {
        double[] values = Arrays.stream(children)
            .mapToDouble(RangeParam::getValue)
            .toArray();

        return new GroupedRangeParamState(values);
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        GroupedRangeParamState grState = (GroupedRangeParamState) state;
        double[] values = grState.values;
        for (int i = 0; i < values.length; i++) {
            double value = values[i];
            if (updateGUI) {
                children[i].setValueNoTrigger(value);
            } else {
                children[i].setValueNoGUI(value);
            }
        }
    }

    @Override
    public void loadStateFrom(String savedValue) {
        StringTokenizer st = new StringTokenizer(savedValue, ",");
        for (RangeParam child : children) {
            String s = st.nextToken();
            child.setValueNoTrigger(Double.parseDouble(s));
        }
    }

    public void setDecimalPlaces(int dp) {
        for (RangeParam child : children) {
            child.setDecimalPlaces(dp);
        }
    }

    public GroupedRangeParam withDecimalPlaces(int dp) {
        setDecimalPlaces(dp);
        return this;
    }

    @Override
    public Object getParamValue() {
        List<Object> childValues = Stream.of(children)
            .map(FilterParam::getParamValue)
            .collect(toList());
        return childValues;
    }

    private record GroupedRangeParamState(double[] values) implements ParamState<GroupedRangeParamState> {
        @Override
        public GroupedRangeParamState interpolate(GroupedRangeParamState endState, double progress) {
            double[] interpolatedValues = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                interpolatedValues[i] = ImageMath.lerp(
                    progress, values[i], endState.values[i]);
            }

            return new GroupedRangeParamState(interpolatedValues);
        }

        @Override
        public String toSaveString() {
            return DoubleStream.of(values)
                .mapToObj("%.2f"::formatted)
                .collect(joining(","));
        }
    }
}

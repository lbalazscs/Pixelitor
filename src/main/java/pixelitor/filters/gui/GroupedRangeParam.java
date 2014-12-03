/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters.gui;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Rectangle;

/**
 * Two or more range params that are grouped and can be linked.
 */
public class GroupedRangeParam extends AbstractGUIParam implements RangeBasedOnImageSize {
    private RangeParam[] rangeParams;
    private final JToggleButton.ToggleButtonModel checkBoxModel;
    private final boolean linkedByDefault;
    private boolean linkable = true; // whether a "Linked" checkbox appears

    /**
     * 2 linked params: "Horizontal" and "Vertical", linked by default
     */
    public GroupedRangeParam(String name, int minValue, int maxValue, int defaultValue) {
        this(name, minValue, maxValue, defaultValue, true);
    }

    /**
     * 2 linked params: "Horizontal" and "Vertical"
     */
    public GroupedRangeParam(String name, int minValue, int maxValue, int defaultValue, boolean linked) {
            this(name, "Horizontal:", "Vertical:", minValue, maxValue, defaultValue, linked);
    }

    /**
     * 2 linked params
     */
    public GroupedRangeParam(String name, String firstRangeName, String secondRangeName, int minValue, int maxValue, int defaultValue, boolean linked) {
        this(name, new String[] {firstRangeName, secondRangeName}, minValue, maxValue, defaultValue, linked);
    }

    /**
     * Any number of linked params
     */
    public GroupedRangeParam(String name, String[] rangeNames, int minValue, int maxValue, int defaultValue, boolean linked) {
        super(name);

        rangeParams = new RangeParam[rangeNames.length];
        for (int i = 0; i < rangeNames.length; i++) {
            String rangeName = rangeNames[i];
            rangeParams[i] = new RangeParam(rangeName, minValue, maxValue, defaultValue);
        }

        checkBoxModel = new JToggleButton.ToggleButtonModel();

        this.linkedByDefault = linked;
        setLinked(linkedByDefault);

        for (final RangeParam param : rangeParams) {
            param.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    if (isLinked()) {
                        // set the value of every other param to the value of the current param
                        for (RangeParam otherParam : rangeParams) {
                            if (otherParam != param) {
                                otherParam.setDontTrigger(true);
                                otherParam.setValue(param.getValue());
                                otherParam.setDontTrigger(false);
                            }
                        }
                    }
                }
            });
        }
    }

    public JToggleButton.ToggleButtonModel getCheckBoxModel() {
        return checkBoxModel;
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        for (RangeParam param : rangeParams) {
            param.setAdjustmentListener(listener);
        }
    }

    @Override
    public JComponent createGUI() {
        return new GroupedRangeSelector(this);
    }

    public int getValue(int index) {
        return rangeParams[index].getValue();
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
    public void setDontTrigger(boolean b) {
        for (RangeParam param : rangeParams) {
            param.setDontTrigger(b);
        }
    }

    @Override
    public boolean isSetToDefault() {
        if(isLinked() == linkedByDefault) {
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
            param.reset(triggerAction);
        }
        // TODO previous code was this - only one was given the triggerAction parameter
        // possibly it is enough or desirable to set triggerAction to true
        // only once because the rest happens automatically
        // TODO also an interesting question is whether the setLinked should be called before this
//        rangeParam1.reset(false);
//        rangeParam2.reset(triggerAction);

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
    public GroupedRangeParam adjustRangeAccordingToImage(double ratio) {
        for (RangeParam param : rangeParams) {
            param.adjustRangeAccordingToImage(ratio);
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
        // TODO
        return null;
    }

    @Override
    public void setState(ParamState state) {
        // TODO
    }
}

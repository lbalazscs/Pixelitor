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
 * Two range params that can be coupled.
 */
public class CoupledRangeParam extends AbstractGUIParam implements RangeBasedOnImageSize {
    private final RangeParam rangeParam1;
    private final RangeParam rangeParam2;
    //    private boolean coupled = true;
    private final JToggleButton.ToggleButtonModel checkBoxModel;
    private final boolean coupledByDefault;
//    private boolean adjustMaxAccordingToImage = false;

    public CoupledRangeParam(String name, int minValue, int maxValue, int defaultValue) {
        this(name, minValue, maxValue, defaultValue, true);
    }

    public CoupledRangeParam(String name, int minValue, int maxValue, int defaultValue, boolean coupled) {
        this(name, "Horizontal:", "Vertical:", minValue, maxValue, defaultValue, coupled);
    }

    public CoupledRangeParam(String name, String firstRangeName, String secondRangeName, int minValue, int maxValue, int defaultValue, boolean coupled) {
        super(name);
        rangeParam1 = new RangeParam(firstRangeName, minValue, maxValue, defaultValue);
        rangeParam2 = new RangeParam(secondRangeName, minValue, maxValue, defaultValue);
        checkBoxModel = new JToggleButton.ToggleButtonModel();

        this.coupledByDefault = coupled;
        setCoupled(coupledByDefault);

        rangeParam1.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (isCoupled()) {
                    rangeParam2.setDontTrigger(true);
                    rangeParam2.setValue(rangeParam1.getValue());
                    rangeParam2.setDontTrigger(false);
                }
            }
        });
        rangeParam2.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (isCoupled()) {
                    rangeParam1.setDontTrigger(true);
                    rangeParam1.setValue(rangeParam2.getValue());
                    rangeParam1.setDontTrigger(false);
                }
            }
        });
    }

    public JToggleButton.ToggleButtonModel getCheckBoxModel() {
        return checkBoxModel;
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        rangeParam1.setAdjustmentListener(listener);
        rangeParam2.setAdjustmentListener(listener);
    }

    @Override
    public JComponent createGUI() {
        return new CoupledRangeSelector(this);
    }

    public int getFirstValue() {
        return rangeParam1.getValue();
    }

    public int getSecondValue() {
        return rangeParam2.getValue();
    }

    public void setFirstValue(int newValue) {
        rangeParam1.setValue(newValue);
        // TODO not necessary
//        if(coupled) {
//            rangeParam2.setValue(newValue);
//        }
    }

    public void setSecondValue(int newValue) {
        rangeParam2.setValue(newValue);
        // TODO not necessary
//        if(coupled) {
//            rangeParam1.setValue(newValue);
//        }
    }

    public boolean isCoupled() {
        return checkBoxModel.isSelected();
    }

    public void setCoupled(boolean coupled) {
        checkBoxModel.setSelected(coupled);
    }

    @Override
    public int getNrOfGridBagCols() {
        return 1;
    }

    @Override
    public void randomize() {
        if (isCoupled()) {
            rangeParam1.randomize();
        } else {
            rangeParam1.randomize();
            rangeParam2.randomize();
        }
    }

    @Override
    public void setDontTrigger(boolean b) {
        rangeParam1.setDontTrigger(b);
        rangeParam2.setDontTrigger(b);
    }

    @Override
    public boolean isSetToDefault() {
        // TODO check the checkbox state?
        return rangeParam1.isSetToDefault() && rangeParam2.isSetToDefault();
    }

    @Override
    public void reset(boolean triggerAction) {
        rangeParam1.reset(false);
        rangeParam2.reset(triggerAction);

        setCoupled(coupledByDefault);
    }

    public RangeParam getFirstRangeParam() {
        return rangeParam1;
    }

    public RangeParam getSecondRangeParam() {
        return rangeParam2;
    }

    @Override
    public void considerImageSize(Rectangle bounds) {
        rangeParam1.considerImageSize(bounds);
        rangeParam2.considerImageSize(bounds);
    }

    public CoupledRangeParam adjustRangeAccordingToImage(double ratio) {
        rangeParam1.adjustRangeAccordingToImage(ratio);
        rangeParam2.adjustRangeAccordingToImage(ratio);
        return this;
    }

    public float getFirstValueAsPercentage() {
        return rangeParam1.getValueAsPercentage();
    }

    public float getSecondValueAsPercentage() {
        return rangeParam2.getValueAsPercentage();
    }

}

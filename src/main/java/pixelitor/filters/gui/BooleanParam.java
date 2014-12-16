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
import java.awt.Rectangle;

/**
 * A GUIParam for selecting a boolean value
 */
public class BooleanParam extends AbstractGUIParam {
    private final boolean defaultValue;
    private boolean currentValue;
    private final boolean ignoreRandomize;
    private ParamGUI paramGUI;
    private boolean finalAnimationSettingMode;

    // show original should ignore the final animation setting
    private boolean ignoreFinalAnimationSettingMode = false;

    public BooleanParam(String name, boolean defaultValue) {
        this(name, defaultValue, false);
    }

    public BooleanParam(String name, boolean defaultValue, boolean ignoreRandomize) {
        super(name);
        this.defaultValue = defaultValue;
        currentValue = defaultValue;
        this.ignoreRandomize = ignoreRandomize;
    }

    public static BooleanParam createParamForHPSharpening() {
        return new BooleanParam("High-Pass Sharpening", false, true);
    }

    public static BooleanParam createParamForShowOriginal() {
        return new BooleanParam("Show Original", false, true);
    }


    @Override
    public boolean isSetToDefault() {
        return (defaultValue == currentValue);
    }

    @Override
    public JComponent createGUI() {
        BooleanSelector selector = new BooleanSelector(this);
        paramGUI = selector;

        if(finalAnimationSettingMode && !ignoreFinalAnimationSettingMode) {
            selector.setEnabled(false);
        }

        return selector;
    }

    @Override
    public void reset(boolean triggerAction) {
        if (!triggerAction) {
            dontTrigger = true;
        }
        setValue(defaultValue, true);
        dontTrigger = false;
    }

    @Override
    public int getNrOfGridBagCols() {
        return 2;
    }

    @Override
    public void randomize() {
        if(finalAnimationSettingMode) {
            assert !canBeAnimated();
            return;
        }
        if (!ignoreRandomize) {
            dontTrigger = true;
            setValue(Math.random() > 0.5, true);
            dontTrigger = false;
        }
    }

    public boolean getValue() {
        return currentValue;
    }

    public void setValue(boolean newValue, boolean updateGUI) {
        if (currentValue != newValue) {
            currentValue = newValue;
            if (!dontTrigger) {
                adjustmentListener.paramAdjusted();
            }
        }
        if (updateGUI && (paramGUI != null)) {
            paramGUI.updateGUI();
        }
    }

    @Override
    public void considerImageSize(Rectangle bounds) {
    }

    @Override
    public boolean canBeAnimated() {
        return false;
    }

    @Override
    public ParamState copyState() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setState(ParamState state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEnabledLogically(boolean b) {
        // TODO
    }

    @Override
    public void setFinalAnimationSettingMode(boolean b) {
        finalAnimationSettingMode = b;
    }

    public void setIgnoreFinalAnimationSettingMode(boolean ignoreFinalAnimationSettingMode) {
        this.ignoreFinalAnimationSettingMode = ignoreFinalAnimationSettingMode;
    }
}

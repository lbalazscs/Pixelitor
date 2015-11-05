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

import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.Rectangle;

import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;

/**
 * A filter parameter for text input
 */
public class TextParam extends AbstractFilterParam {
    private final String defaultValue;

    private boolean trigger = true; // whether the running of the filter should be triggered
    private final TextParamGUI gui;

    public TextParam(String name, String defaultValue) {
        super(name, ALLOW_RANDOMIZE);
        this.defaultValue = defaultValue;
        gui = new TextParamGUI(this, defaultValue, adjustmentListener);
    }

    @Override
    public JComponent createGUI() {
        paramGUI = gui;
        setParamGUIEnabledState();
        return gui;
    }

    @Override
    public boolean isSetToDefault() {
        return defaultValue.equals(getValue());
    }

    @Override
    public void reset(boolean triggerAction) {
        if (triggerAction) {
            setValue(defaultValue);
        } else {
            trigger = false;
            setValue(defaultValue);
            trigger = true;
        }
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        this.adjustmentListener = listener;
    }

    @Override
    public int getNrOfGridBagCols() {
        return 1;
    }

    @Override
    public void randomize() {
        trigger = false;
        setValue(Utils.getRandomString(15));
        trigger = true;
    }

    public String getValue() {
        return gui.getText();
    }

    public void setValue(String s) {
        String old = gui.getText();
        if (!old.equals(s)) {
            boolean triggerWasTrue = trigger;
            trigger = false;
            gui.setText(s);
            trigger = true;
            if (triggerWasTrue) { // handle the case when this is called from randomize
                adjustmentListener.paramAdjusted();
            }
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
    public String toString() {
        return String.format("%s[name = '%s', text = '%s']",
                getClass().getSimpleName(), getName(), gui == null ? "null" : gui.getText());
    }

    public boolean isTrigger() {
        return trigger;
    }
}

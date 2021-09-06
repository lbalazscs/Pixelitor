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

import pixelitor.utils.Rnd;

import javax.swing.*;

import static java.lang.String.format;
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
        afterGUICreation();
        return gui;
    }

    @Override
    public boolean isSetToDefault() {
        return defaultValue.equals(getValue());
    }

    @Override
    public void reset(boolean trigger) {
        if (trigger) {
            setValue(defaultValue);
        } else {
            this.trigger = false;
            setValue(defaultValue);
            this.trigger = true;
        }
    }

    @Override
    protected void doRandomize() {
        trigger = false;
        setValue(Rnd.createRandomString(15));
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
    public boolean canBeAnimated() {
        return false;
    }

    @Override
    public ParamState<?> copyState() {
        return new TextParamState(gui.getText());
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        gui.setText(((TextParamState) state).value());
    }

    @Override
    public void loadStateFrom(String savedValue) {
        gui.setText(savedValue);
    }

    public boolean isTrigger() {
        return trigger;
    }

    @Override
    public Object getParamValue() {
        return getValue();
    }

    @Override
    public String toString() {
        return format("%s[name = '%s', text = '%s']",
            getClass().getSimpleName(), getName(), gui == null ? "null" : gui.getText());
    }

    private record TextParamState(String value) implements ParamState<TextParamState> {
        @Override
        public TextParamState interpolate(TextParamState endState, double progress) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toSaveString() {
            return value;
        }
    }
}

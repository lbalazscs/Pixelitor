/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import java.io.Serial;
import java.util.List;

import static java.lang.String.format;
import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;

/**
 * A filter parameter for text input.
 */
public class TextParam extends AbstractFilterParam {
    private final String defaultValue;

    // If true, the filter is not executed after every edit.
    // Instead, there is a "Run" button in the GUI for that purpose.
    private final boolean command;

    private String value;
    private TextParamGUI gui;

    // in the case of commands the random values have to be listed explicitly
    private List<String> randomCommands;

    public TextParam(String name, String defaultValue, boolean command) {
        super(name, ALLOW_RANDOMIZE);
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.command = command;
    }

    @Override
    public JComponent createGUI() {
        gui = new TextParamGUI(this, value, adjustmentListener);
        paramGUI = gui;
        guiCreated();
        return gui;
    }

    @Override
    public boolean hasDefault() {
        return defaultValue.equals(getValue());
    }

    @Override
    public void reset(boolean trigger) {
        setValue(defaultValue, trigger);
    }

    @Override
    protected void doRandomize() {
        String randomValue;
        if (command && randomCommands != null) {
            randomValue = Rnd.chooseFrom(randomCommands);
        } else {
            randomValue = Rnd.createRandomString(15);
        }
        setValue(randomValue, false);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String s, boolean trigger) {
        if (!value.equals(s)) {
            value = s;
            if (paramGUI != null) {
                paramGUI.updateGUI();
            }
            if (trigger && adjustmentListener != null) {
                adjustmentListener.paramAdjusted();
            }
        }
    }

    public void setRandomCommands(List<String> randomCommands) {
        assert command;
        this.randomCommands = randomCommands;
    }

    @Override
    public boolean isAnimatable() {
        return false;
    }

    @Override
    public TextParamState copyState() {
        return new TextParamState(value);
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        String newValue = ((TextParamState) state).value();
        value = newValue;
        if (updateGUI) {
            gui.setText(newValue);
        }
    }

    @Override
    public void loadStateFrom(String savedValue) {
        setValue(savedValue, false);
    }

    @Override
    public String getParamValue() {
        return getValue();
    }

    public boolean isCommand() {
        return command;
    }

    @Override
    public boolean isComplex() {
        return true;
    }

    @Override
    public String toString() {
        return format("%s[name = '%s', text = '%s']",
            getClass().getSimpleName(), getName(), value);
    }

    public record TextParamState(String value) implements ParamState<TextParamState> {
        @Serial
        private static final long serialVersionUID = 1L;

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

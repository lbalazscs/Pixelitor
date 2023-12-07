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

import pixelitor.colors.Colors;
import pixelitor.utils.Rnd;

import javax.swing.*;
import java.awt.Color;
import java.util.Arrays;

/**
 * Represents two or more color selectors that are grouped together in the GUI.
 */
public class GroupedColorsParam extends AbstractFilterParam {
    private Color[] values;
    private final Color[] defaultValues;
    private final String[] names;

    public GroupedColorsParam(String name,
                              String firstName, Color firstColor,
                              String secondName, Color secondColor) {
        super(name, RandomizePolicy.ALLOW_RANDOMIZE);
        this.names = new String[]{firstName, secondName};
        values = new Color[]{firstColor, secondColor};
        defaultValues = new Color[]{firstColor, secondColor};
    }

    @Override
    public JComponent createGUI() {
        var gui = new GroupedColorsParamGUI(this);
        paramGUI = gui;
        guiCreated();

        return gui;
    }

    public int getNumColors() {
        return values.length;
    }

    public Color getColor(int index) {
        return values[index];
    }

    public String getColorStr(int index) {
        return Colors.formatGMIC(values[index]);
    }

    public String getName(int index) {
        return names[index];
    }

    // sets a single color
    public void setValue(Color newColor, int changedIndex, boolean trigger) {
        Color[] newColors = new Color[values.length];
        for (int i = 0; i < newColors.length; i++) {
            if (i == changedIndex) {
                newColors[i] = newColor;
            } else {
                newColors[i] = values[i]; // not changed
            }
        }
        setValue(newColors, trigger);
    }

    public void setValue(Color[] newColors, boolean trigger) {
        if (Arrays.equals(newColors, values)) {
            return;
        }
        values = newColors;

        if (paramGUI != null) {
            paramGUI.updateGUI();
        }

        if (trigger && adjustmentListener != null) {
            adjustmentListener.paramAdjusted();
        }
    }

    @Override
    public ParamState<?> copyState() {
        return new GroupedColorsParamState(values);
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        Color[] newColors = ((GroupedColorsParamState) state).colors();
        if (updateGUI) {
            setValue(newColors, false);
        } else {
            values = newColors;
        }
    }

    @Override
    public void loadStateFrom(String savedValue) {

    }

    @Override
    public boolean isAnimatable() {
        return true;
    }

    @Override
    public Object getParamValue() {
        return values;
    }

    @Override
    public boolean hasDefault() {
        return Arrays.equals(values, defaultValues);
    }

    @Override
    protected void doRandomize() {
        Color newColor1 = Rnd.createRandomColor(true);
        Color newColor2 = Rnd.createRandomColor(true);
        setValue(new Color[]{newColor1, newColor2}, false);
    }

    @Override
    public void reset(boolean trigger) {
        setValue(defaultValues, trigger);
    }

    public record GroupedColorsParamState(Color[] colors) implements ParamState<GroupedColorsParamState> {

        @Override
        public GroupedColorsParamState interpolate(GroupedColorsParamState endState, double progress) {
            int numColors = colors.length;
            Color[] interpolatedColors = new Color[numColors];
            for (int i = 0; i < numColors; i++) {
                interpolatedColors[i] = Colors.interpolateRGB(
                    colors[i], endState.colors[i], progress);
            }
            return new GroupedColorsParamState(interpolatedColors);
        }

        @Override
        public String toSaveString() {
            return null;
        }
    }
}

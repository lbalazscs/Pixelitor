/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.colors.ColorHistory;
import pixelitor.colors.Colors;
import pixelitor.utils.Rnd;

import javax.swing.*;
import java.awt.Color;
import java.io.Serial;
import java.util.Arrays;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

/**
 * A filter parameter for selecting a list of colors
 */
public class ColorListParam extends AbstractFilterParam {
    private Color[] colors;
    private final int minNumColors;
    private final Color[] candidateColors;
    private final Color[] defaultColors;

    public ColorListParam(String name, int defaultNumColors, int minNumColors, Color... candidateColors) {
        super(name, RandomizePolicy.ALLOW_RANDOMIZE);
        this.minNumColors = minNumColors;
        this.candidateColors = candidateColors;
        this.colors = Arrays.copyOf(candidateColors, defaultNumColors);
        this.defaultColors = Arrays.copyOf(candidateColors, defaultNumColors);
    }

    @Override
    public JComponent createGUI() {
        var gui = new ColorListParamGUI(this, candidateColors, minNumColors);
        paramGUI = gui;
        guiCreated();

        return gui;
    }

    public Color[] getColors() {
        return colors;
    }

    public Color getColor(int index) {
        return colors[index];
    }

    public void setColor(int index, Color newColor, boolean trigger) {
        Color oldColor = colors[index];
        if (oldColor.equals(newColor)) {
            return;
        }
        colors[index] = newColor;

        ColorHistory.remember(newColor);

        updateGUI(trigger);
    }

    public void setColors(Color[] newColors, boolean trigger) {
        boolean change = !Arrays.equals(colors, newColors);
        colors = newColors;
        if (change) {
            updateGUI(trigger);
        }
    }

    private void updateGUI(boolean trigger) {
        if (paramGUI != null) {
            paramGUI.updateGUI();
        }

        if (trigger) {
            if (adjustmentListener != null) {  // when called from randomize, this is null
                adjustmentListener.paramAdjusted();
            }
        }
    }

    @Override
    protected void doRandomize() {
        Color[] newColors = new Color[colors.length];
        for (int i = 0; i < colors.length; i++) {
            newColors[i] = Rnd.createRandomColor();
        }
        setColors(newColors, false);
    }

    @Override
    public ParamState<?> copyState() {
        return new ColorListParamState(colors);
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        Color[] newColors = ((ColorListParamState) state).colors();
        setColors(newColors, false);
    }

    @Override
    public void loadStateFrom(String savedValue) {
        String[] colorStrings = savedValue.split(",");
        Color[] newColors = new Color[colorStrings.length];
        for (int i = 0; i < newColors.length; i++) {
            newColors[i] = Colors.fromHTMLHex(colorStrings[i]);
        }
        setColors(newColors, false);
    }

    @Override
    public boolean canBeAnimated() {
        return false;
    }

    @Override
    public Object getParamValue() {
        return colors;
    }

    @Override
    public boolean isSetToDefault() {
        return Arrays.equals(colors, defaultColors);
    }

    @Override
    public void reset(boolean trigger) {
        Color[] defCopy = Arrays.copyOf(defaultColors, defaultColors.length);
        setColors(defCopy, trigger);
    }

    private record ColorListParamState(Color[] colors) implements ParamState<ColorListParamState> {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public ColorListParamState interpolate(ColorListParamState endState, double progress) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toSaveString() {
            return Stream.of(colors)
                .map(c -> Colors.toHTMLHex(c, true))
                .collect(joining(","));
        }
    }
}

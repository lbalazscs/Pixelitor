/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

/**
 * A filter parameter for selecting a list of colors.
 */
public class ColorListParam extends AbstractFilterParam {
    private Color[] colors;
    private final int minNumColors;
    private final Color[] candidateColors;
    private final Color[] defaultColors;

    // candidateColors supplies the initial colors, the colors for newly revealed swatches, and the maximum count
    public ColorListParam(String name, int defaultNumColors, int minNumColors, Color... candidateColors) {
        super(name, RandomizeMode.ALLOW);
        assert minNumColors <= defaultNumColors && defaultNumColors <= candidateColors.length;

        this.minNumColors = minNumColors;
        this.candidateColors = candidateColors;
        this.colors = Arrays.copyOf(candidateColors, defaultNumColors);
        this.defaultColors = Arrays.copyOf(this.colors, defaultNumColors);
    }

    @Override
    public JComponent createGUI() {
        return installGUI(new ColorListParamGUI(this));
    }

    /**
     * Returns the current list of colors.
     */
    public Color[] getColors() {
        return colors;
    }

    public int[] getColorsAsArgb() {
        return Arrays.stream(colors)
            .mapToInt(Color::getRGB)
            .toArray();
    }

    /**
     * Returns the color at the given index.
     */
    public Color getColor(int index) {
        return colors[index];
    }

    public int getMinNumColors() {
        return minNumColors;
    }

    public int getMaxNumColors() {
        return candidateColors.length;
    }

    public Color getCandidateColor(int index) {
        return candidateColors[index];
    }

    /**
     * Sets the color at the given index.
     */
    public void setColor(int index, Color newColor, boolean trigger) {
        if (colors[index].equals(newColor)) {
            return;
        }
        Color[] newColors = colors.clone();
        newColors[index] = newColor;
        colors = newColors;

        ColorHistory.remember(newColor);

        updateGUI();

        if (trigger) {
            triggerFilter();
        }
    }

    /**
     * Sets the list of colors.
     */
    public void setColors(Color[] newColors, boolean trigger) {
        if (newColors.length < minNumColors || newColors.length > candidateColors.length) {
            throw new IllegalArgumentException("Expected %d-%d colors, got %d"
                .formatted(minNumColors, candidateColors.length, newColors.length));
        }

        if (Arrays.equals(colors, newColors)) {
            return;
        }
        colors = newColors.clone();
        updateGUI();

        if (trigger) {
            triggerFilter();
        }
    }

    @Override
    protected void doRandomize() {
        int numNewColors = Rnd.intInRange(minNumColors, candidateColors.length);
        Color[] newColors = new Color[numNewColors];
        Arrays.setAll(newColors, i -> Rnd.createRandomColor());
        setColors(newColors, false);
    }

    @Override
    public ColorListParamState copyState() {
        return new ColorListParamState(colors.clone());
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        Color[] newColors = ((ColorListParamState) state).colors();
        setColors(newColors, false);
    }

    @Override
    public void loadStateFrom(String savedValue) {
        try {
            Color[] newColors = Colors.parseHtmlHexList(savedValue);
            setColors(newColors, false);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Could not parse " + savedValue, e);
        }
    }

    @Override
    public boolean isAnimatable() {
        return false;
    }

    @Override
    public String getValueAsString() {
        return Colors.formatForDebugging(colors);
    }

    @Override
    public boolean isAtDefault() {
        return Arrays.equals(colors, defaultColors);
    }

    @Override
    public void reset(boolean trigger) {
        setColors(defaultColors, trigger);
    }

    /**
     * The state of a {@link ColorListParam}.
     */
    public record ColorListParamState(Color[] colors) implements ParamState<ColorListParamState> {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public ColorListParamState interpolate(ColorListParamState endState, double progress) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toPresetString() {
            return Colors.toHtmlHexList(colors);
        }
    }
}

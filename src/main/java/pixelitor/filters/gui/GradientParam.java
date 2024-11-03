/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.Colormap;
import com.jhlabs.image.ImageMath;
import pixelitor.colors.Colors;
import pixelitor.utils.Rnd;

import javax.swing.*;
import java.awt.Color;
import java.io.Serial;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.awt.Color.BLACK;
import static java.awt.Color.GRAY;
import static java.awt.Color.WHITE;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;

/**
 * Represents a gradient.
 */
public class GradientParam extends AbstractFilterParam {
    private GradientParamGUI gui;
    private final float[] defaultThumbPositions;
    private final Color[] defaultColors;
    private float[] thumbPositions;
    private Color[] colors;

    public GradientParam(String name, Color startColor, Color endColor) {
        this(name, new float[]{0.0f, 0.5f, 1.0f},
            new Color[]{
                startColor,
                Colors.averageRGB(startColor, endColor),
                endColor});
    }

    public GradientParam(String name, float[] defaultThumbPositions,
                         Color[] defaultColors) {
        this(name, defaultThumbPositions, defaultColors, ALLOW_RANDOMIZE);
    }

    public GradientParam(String name, float[] defaultThumbPositions,
                         Color[] defaultColors, RandomizePolicy randomizePolicy) {
        super(name, randomizePolicy);
        this.defaultThumbPositions = defaultThumbPositions;
        this.defaultColors = defaultColors;

        thumbPositions = defaultThumbPositions;
        colors = defaultColors;
    }

    public static GradientParam createBlackToWhite(String name) {
        return new GradientParam(name,
            new float[]{0.0f, 0.5f, 1.0f},
            new Color[]{BLACK, GRAY, WHITE});
    }

    @Override
    public JComponent createGUI() {
        gui = new GradientParamGUI(this);
        paramGUI = gui;
        guiCreated();
        return gui;
    }

    public void setValues(float[] thumbPositions, Color[] colors, boolean trigger) {
        if (Arrays.equals(this.thumbPositions, thumbPositions)
            && Arrays.equals(this.colors, colors)) {

            return;
        }

        this.thumbPositions = thumbPositions;
        this.colors = colors;

        if (paramGUI != null) {
            paramGUI.updateGUI();
        }
        if (trigger && adjustmentListener != null) {
            adjustmentListener.paramAdjusted();
        }
    }

    public Colormap getColorMap() {
        return this::interpolatedColorAt;
    }

    private int interpolatedColorAt(float pos) {
        // Interpolate here, replicating the getValue(pos) logic in
        // GradientSlider, because this code might be called in cases
        // when there is no GUI instantiated (testing, smart filters).
        for (int i = 0; i < thumbPositions.length - 1; i++) {
            if (thumbPositions[i] <= pos && pos <= thumbPositions[i + 1]) {
                float t = (pos - thumbPositions[i]) / (thumbPositions[i + 1] - thumbPositions[i]);
                int left = (colors[i]).getRGB();
                int right = (colors[i + 1]).getRGB();
                return ImageMath.mixColors(t, left, right);
            }
        }
        if (pos < thumbPositions[0]) {
            return colors[0].getRGB(); // first color
        }
        if (pos > thumbPositions[thumbPositions.length - 1]) {
            return colors[colors.length - 1].getRGB(); // last color
        }

        // should never get here
        throw new IllegalStateException("pos = " + pos);
    }

    @Override
    protected void doRandomize() {
        Color[] randomColors = new Color[defaultThumbPositions.length];
        for (int i = 0; i < randomColors.length; i++) {
            randomColors[i] = Rnd.createRandomColor();
        }

        setValues(defaultThumbPositions, randomColors, false);
    }

    @Override
    public boolean hasDefault() {
        return !areThumbPositionsChanged() && !areColorsChanged();
    }

    private boolean areThumbPositionsChanged() {
        if (thumbPositions.length != defaultThumbPositions.length) {
            return true;
        }
        for (int i = 0; i < thumbPositions.length; i++) {
            if (thumbPositions[i] != defaultThumbPositions[i]) {
                return true;
            }
        }
        return false;
    }

    private boolean areColorsChanged() {
        if (colors.length != defaultColors.length) {
            return true;
        }

        for (int i = 0; i < defaultColors.length; i++) {
            if (!defaultColors[i].equals(colors[i])) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void reset(boolean trigger) {
        setValues(defaultThumbPositions, defaultColors, trigger);
    }

    @Override
    public boolean isAnimatable() {
        return true;
    }

    @Override
    public boolean isComplex() {
        return true;
    }

    @Override
    public GradientParamState copyState() {
        return new GradientParamState(thumbPositions, colors);
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        GradientParamState gr = (GradientParamState) state;

        setValues(gr.thumbPositions, gr.colors, false);
    }

    @Override
    public void loadStateFrom(String savedValue) {
        // the expected argument format is like: 0.00,0.50,1.00|91707B,1E165B,BF7512
        int pipeIndex = savedValue.indexOf('|');
        if (pipeIndex == -1) {
            throw new IllegalArgumentException("savedValue = " + savedValue);
        }

        String[] thumbStrings = savedValue.substring(0, pipeIndex).split(",");
        String[] colorStrings = savedValue.substring(pipeIndex + 1).split(",");
        if (thumbStrings.length != colorStrings.length) {
            throw new IllegalArgumentException("savedValue = " + savedValue);
        }

        float[] newThumbPositions = new float[thumbStrings.length];
        Color[] newColors = new Color[colorStrings.length];
        for (int i = 0; i < thumbStrings.length; i++) {
            newThumbPositions[i] = Float.parseFloat(thumbStrings[i]);
            newColors[i] = Colors.fromHTMLHex(colorStrings[i]);
        }

        setValues(newThumbPositions, newColors, false);
    }

    @Override
    public List<Object> getParamValue() {
        return List.of(colors);
    }

    public float[] getThumbPositions() {
        return thumbPositions;
    }

    public Color[] getColors() {
        return colors;
    }

    @Override
    public String toString() {
        return format("%s[name = '%s']", getClass().getSimpleName(), getName());
    }

    public record GradientParamState(float[] thumbPositions,
                                      Color[] colors) implements ParamState<GradientParamState> {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public GradientParamState interpolate(GradientParamState endState, double progress) {
            // This will not work if the number of thumbs changes
            float[] interpolatedPositions = interpolatePositions((float) progress, endState);
            Color[] interpolatedColors = interpolateColors((float) progress, endState);

            return new GradientParamState(interpolatedPositions, interpolatedColors);
        }

        private float[] interpolatePositions(float progress, GradientParamState endState) {
            float[] interpolatedPositions = new float[thumbPositions.length];
            for (int i = 0; i < thumbPositions.length; i++) {
                float initial = thumbPositions[i];
                float end = endState.thumbPositions[i];
                interpolatedPositions[i] = ImageMath.lerp(progress, initial, end);
            }
            return interpolatedPositions;
        }

        private Color[] interpolateColors(float progress, GradientParamState endState) {
            Color[] interpolatedColors = new Color[colors.length];
            for (int i = 0; i < colors.length; i++) {
                Color initial = colors[i];
                Color end = endState.colors[i];
                interpolatedColors[i] = Colors.interpolateRGB(initial, end, progress);
            }
            return interpolatedColors;
        }

        @Override
        public String toSaveString() {
            String thumbsString = IntStream.range(0, thumbPositions.length)
                .mapToDouble(i -> thumbPositions[i])
                .mapToObj("%.2f"::formatted)
                .collect(joining(",", "", "|"));

            String colorsString = Stream.of(colors)
                .map(c -> Colors.toHTMLHex(c, true))
                .collect(joining(","));

            return thumbsString + colorsString;
        }
    }
}

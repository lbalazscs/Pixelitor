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

import com.jhlabs.image.Colormap;
import com.jhlabs.image.ImageMath;
import pixelitor.colors.Colors;
import pixelitor.utils.Rnd;

import javax.swing.*;
import java.awt.Color;
import java.io.Serial;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.awt.Color.*;
import static java.util.stream.Collectors.joining;

/**
 * Represents a gradient filter parameter.
 */
public class GradientParam extends AbstractFilterParam {
    public static final String CUSTOM_PRESET_NAME = "Custom";

    private final List<GradientPreset> presets;
    private final float[] defaultThumbPositions;
    private final Color[] defaultColors;
    private float[] thumbPositions;
    private Color[] colors;
    private String selectedPresetName;

    public GradientParam(String name, List<GradientPreset> presets) {
        this(name, presets, RandomizeMode.ALLOW);
    }

    public GradientParam(String name, List<GradientPreset> presets, RandomizeMode randomizeMode) {
        super(name, randomizeMode);
        assert presets != null && !presets.isEmpty();

        this.presets = List.copyOf(presets);
        GradientPreset firstPreset = this.presets.getFirst();
        this.defaultThumbPositions = firstPreset.thumbPositions();
        this.defaultColors = firstPreset.colors();

        this.thumbPositions = this.defaultThumbPositions.clone();
        this.colors = this.defaultColors.clone();
        this.selectedPresetName = firstPreset.name();
    }

    // constructors without presets
    public GradientParam(String name, Color startColor, Color endColor) {
        this(name, new float[]{0.0f, 0.5f, 1.0f},
            new Color[]{
                startColor,
                Colors.averageRgb(startColor, endColor),
                endColor});
    }

    public GradientParam(String name, float[] defaultThumbPositions,
                         Color[] defaultColors) {
        this(name, defaultThumbPositions, defaultColors, RandomizeMode.ALLOW);
    }

    public GradientParam(String name, float[] defaultThumbPositions,
                         Color[] defaultColors, RandomizeMode randomizeMode) {
        super(name, randomizeMode);
        this.presets = List.of();
        this.defaultThumbPositions = defaultThumbPositions;
        this.defaultColors = defaultColors;

        this.thumbPositions = defaultThumbPositions;
        this.colors = defaultColors;
        this.selectedPresetName = CUSTOM_PRESET_NAME;
    }

    public static GradientParam createBlackToWhite(String name) {
        return new GradientParam(name,
            new float[]{0.0f, 0.5f, 1.0f},
            new Color[]{BLACK, GRAY, WHITE});
    }

    public static GradientParam createWhiteToBlack(String name) {
        return new GradientParam(name,
            new float[]{0.0f, 0.5f, 1.0f},
            new Color[]{WHITE, GRAY, BLACK});
    }

    public static GradientParam createUniformWhite() {
        return new GradientParam("Colors",
            new float[]{0.0f, 0.5f, 1.0f},
            new Color[]{WHITE, WHITE, WHITE});
    }

    public boolean hasPresets() {
        return !presets.isEmpty();
    }

    public List<GradientPreset> getPresets() {
        return presets;
    }

    public String getSelectedPresetName() {
        return selectedPresetName;
    }

    /**
     * Returns all predefined preset names in registered order with "Custom" appended.
     */
    public String[] getSelectablePresetNames() {
        String[] names = new String[presets.size() + 1];
        for (int i = 0; i < presets.size(); i++) {
            names[i] = presets.get(i).name();
        }
        names[presets.size()] = CUSTOM_PRESET_NAME;
        return names;
    }

    /**
     * Selects a predefined preset by name. "Custom" is a state indicator and not actionable.
     */
    public void selectPreset(String name) {
        Objects.requireNonNull(name, "name");
        if (CUSTOM_PRESET_NAME.equals(name)) {
            if (paramGUI != null) {
                paramGUI.updateGUI();
            }
            return;
        }

        for (GradientPreset preset : presets) {
            if (preset.name().equals(name)) {
                if (Arrays.equals(this.thumbPositions, preset.thumbPositions())
                    && Arrays.equals(this.colors, preset.colors())) {
                    this.selectedPresetName = preset.name();
                    if (paramGUI != null) {
                        paramGUI.updateGUI();
                    }
                    return;
                }
                setValues(preset.thumbPositions(), preset.colors(), true);
                return;
            }
        }
        throw new IllegalArgumentException("Unknown preset: " + name);
    }

    private void updateSelectedPresetName() {
        for (GradientPreset preset : presets) {
            if (Arrays.equals(thumbPositions, preset.thumbPositions())
                && Arrays.equals(colors, preset.colors())) {
                selectedPresetName = preset.name();
                return;
            }
        }
        selectedPresetName = CUSTOM_PRESET_NAME;
    }

    @Override
    public JComponent createGUI() {
        GradientParamGUI gui = new GradientParamGUI(this);
        paramGUI = gui;
        syncWithGui();
        return gui;
    }

    public void setValues(float[] thumbPositions, Color[] colors, boolean trigger) {
        if (Arrays.equals(this.thumbPositions, thumbPositions)
            && Arrays.equals(this.colors, colors)) {
            return;
        }

        this.thumbPositions = thumbPositions.clone();
        this.colors = colors.clone();
        updateSelectedPresetName();

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
        // interpolate here, replicating the getValue(pos) logic in
        // GradientSlider, because this code might be called in cases
        // when there is no GUI instantiated (testing, smart filters)
        for (int i = 0; i < thumbPositions.length - 1; i++) {
            if (thumbPositions[i] <= pos && pos <= thumbPositions[i + 1]) {
                float t = (pos - thumbPositions[i]) / (thumbPositions[i + 1] - thumbPositions[i]);
                int left = (colors[i]).getRGB();
                int right = (colors[i + 1]).getRGB();
                return ImageMath.mixColors(t, left, right);
            }
        }
        if (pos < thumbPositions[0]) {
            return colors[0].getRGB();
        }
        if (pos > thumbPositions[thumbPositions.length - 1]) {
            return colors[colors.length - 1].getRGB();
        }

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
    public boolean isAtDefault() {
        return Arrays.equals(thumbPositions, defaultThumbPositions)
            && Arrays.equals(colors, defaultColors);
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

        setValues(gr.thumbPositions(), gr.colors(), false);
        if (updateGUI && paramGUI != null) {
            paramGUI.updateGUI();
        }
    }

    @Override
    public void loadStateFrom(String savedValue) {
        Objects.requireNonNull(savedValue, "savedValue");
        int pipeIndex = savedValue.indexOf('|');
        if (pipeIndex == -1) {
            throw new IllegalArgumentException("savedValue missing '|': " + savedValue);
        }

        String thumbPart = savedValue.substring(0, pipeIndex);
        String colorPart = savedValue.substring(pipeIndex + 1);

        String[] thumbStrings = thumbPart.split(",");
        String[] colorStrings = colorPart.split(",");
        if (thumbStrings.length != colorStrings.length) {
            throw new IllegalArgumentException(
                "savedValue length mismatch: thumbs=" + thumbStrings.length + ", colors=" + colorStrings.length);
        }

        float[] newThumbPositions = new float[thumbStrings.length];
        Color[] newColors = new Color[colorStrings.length];
        for (int i = 0; i < thumbStrings.length; i++) {
            newThumbPositions[i] = Float.parseFloat(thumbStrings[i].trim());
            if (newThumbPositions[i] < 0.0f || newThumbPositions[i] > 1.0f) {
                throw new IllegalArgumentException("Thumb position out of range [0, 1]: " + newThumbPositions[i]);
            }
            if (i > 0 && newThumbPositions[i] < newThumbPositions[i - 1]) {
                throw new IllegalArgumentException("Thumb positions not ascending: " + Arrays.toString(newThumbPositions));
            }

            String colorStr = colorStrings[i].trim();
            Color c;
            if (colorStr.length() == 8) {
                c = Colors.fromHtmlHexRgba(colorStr);
            } else if (colorStr.length() == 6) {
                c = Colors.fromHtmlHexRgb(colorStr);
            } else {
                throw new IllegalArgumentException("Invalid color string length: " + colorStr);
            }
            if (c == null) {
                throw new IllegalArgumentException("Could not parse color: " + colorStr);
            }
            newColors[i] = c;
        }

        setValues(newThumbPositions, newColors, false);
        if (paramGUI != null) {
            paramGUI.updateGUI();
        }
    }

    @Override
    public String getValueAsString() {
        return copyState().toPresetString();
    }

    public float[] getThumbPositions() {
        return thumbPositions;
    }

    public Color[] getColors() {
        return colors;
    }

    @Override
    public String toString() {
        return String.format("%s[name = '%s']", getClass().getSimpleName(), getName());
    }

    public record GradientParamState(float[] thumbPositions,
                                     Color[] colors) implements ParamState<GradientParamState> {
        @Serial
        private static final long serialVersionUID = 1L;

        public GradientParamState {
            thumbPositions = thumbPositions.clone();
            colors = colors.clone();
        }

        @Override
        public float[] thumbPositions() {
            return thumbPositions.clone();
        }

        @Override
        public Color[] colors() {
            return colors.clone();
        }

        @Override
        public GradientParamState interpolate(GradientParamState endState, double progress) {
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
                interpolatedColors[i] = Colors.interpolateRgb(initial, end, progress);
            }
            return interpolatedColors;
        }

        @Override
        public String toPresetString() {
            String thumbsString = IntStream.range(0, thumbPositions.length)
                .mapToDouble(i -> thumbPositions[i])
                .mapToObj(d -> String.format(Locale.ROOT, "%.2f", d))
                .collect(joining(",", "", "|"));

            String colorsString = Stream.of(colors)
                .map(c -> Colors.toHtmlHex(c, true))
                .collect(joining(","));

            return thumbsString + colorsString;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof GradientParamState other)) {
                return false;
            }
            return Arrays.equals(thumbPositions, other.thumbPositions)
                && Arrays.equals(colors, other.colors);
        }

        @Override
        public int hashCode() {
            return 31 * Arrays.hashCode(thumbPositions) + Arrays.hashCode(colors);
        }
    }
}

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

import com.bric.swing.GradientSlider;
import com.jhlabs.image.Colormap;
import com.jhlabs.image.ImageMath;
import pixelitor.colors.Colors;
import pixelitor.utils.Rnd;

import javax.swing.*;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.io.Serial;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.bric.swing.MultiThumbSlider.HORIZONTAL;
import static java.awt.Color.*;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;

/**
 * Represents a gradient.
 * Unlike other filter parameter implementations, this is not
 * a GUI-free model, the actual value is stored inside the GradientSlider.
 */
public class GradientParam extends AbstractFilterParam {
    private static final String USE_BEVEL = "GradientSlider.useBevel";
    private GradientSlider gradientSlider;
    private GUI gui;
    private final float[] defaultThumbPositions;
    private final Color[] defaultColors;

    // whether the running of the filter should be triggered
    private boolean trigger = true;

    public GradientParam(String name, Color firstColor, Color secondColor) {
        this(name, new float[]{0.0f, 0.5f, 1.0f},
            new Color[]{
                firstColor,
                Colors.rgbAverage(firstColor, secondColor),
                secondColor});
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

        // has to be created in the constructor because getValue() can be called early
        createGradientSlider(defaultThumbPositions, defaultColors);
    }

    public static GradientParam createBlackToWhite(String name) {
        return new GradientParam(name,
            new float[]{0.0f, 0.5f, 1.0f},
            new Color[]{BLACK, GRAY, WHITE});
    }

    private void createGradientSlider(float[] defaultThumbPositions, Color[] defaultColors) {
        gradientSlider = new GradientSlider(HORIZONTAL,
            defaultThumbPositions, defaultColors);
        gradientSlider.addPropertyChangeListener(this::sliderPropertyChanged);
        gradientSlider.putClientProperty(USE_BEVEL, "true");
        gradientSlider.setPreferredSize(new Dimension(250, 30));
    }

    private void setValuesNoTrigger(float[] thumbPositions, Color[] defaultColors) {
        this.trigger = false;
        gradientSlider.setValues(thumbPositions, defaultColors);
        this.trigger = true;
    }

    private void sliderPropertyChanged(PropertyChangeEvent evt) {
        if (shouldStartFilter(evt)) {
            if (gui != null) {
                gui.updateResetButtonIcon();
            }
            adjustmentListener.paramAdjusted();
        }
    }

    private void debugParents() {
        Container parent = gradientSlider.getParent();
        while (parent != null) {
            System.out.println("GradientParam::debugParents: parent = " + (parent == null ? "null" :
                (parent + ", class = " + parent.getClass().getName())));
            parent = parent.getParent();
        }
    }

    private boolean shouldStartFilter(PropertyChangeEvent evt) {
//        if (evt.getPropertyName().equals("UI")) {
//            debugParents();
//        }

        if (trigger && !gradientSlider.isValueAdjusting() && adjustmentListener != null) {
            return switch (evt.getPropertyName()) {
                case "ancestor", "selected thumb", "enabled",
                    "graphicsConfiguration", "UI", USE_BEVEL -> false;
                default -> true;
            };
        }
        return false;
    }

    @Override
    public JComponent createGUI() {
        gui = new GUI(gradientSlider, this);
        return gui;
    }

    public Colormap getValue() {
        return this::rgbIntFromValue;
    }

    private int rgbIntFromValue(float v) {
        Color c = (Color) gradientSlider.getValue(v);
        if (c == null) {
            throw new IllegalStateException("null color for v = " + v);
        }
        return c.getRGB();
    }

    @Override
    protected void doRandomize() {
        Color[] randomColors = new Color[defaultThumbPositions.length];
        for (int i = 0; i < randomColors.length; i++) {
            randomColors[i] = Rnd.createRandomColor();
        }

        setValuesNoTrigger(defaultThumbPositions, randomColors);
        if (gui != null) {
            gui.updateResetButtonIcon();
        }
    }

    @Override
    public boolean isSetToDefault() {
        return !areThumbPositionsChanged() && !areColorsChanged();
    }

    private boolean areThumbPositionsChanged() {
        float[] thumbPositions = gradientSlider.getThumbPositions();
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
        Object[] values = gradientSlider.getValues();
        if (values.length != defaultColors.length) {
            return true;
        }

        for (int i = 0; i < defaultColors.length; i++) {
            if (!defaultColors[i].equals(values[i])) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void reset(boolean trigger) {
        if (trigger) {
            gradientSlider.setValues(defaultThumbPositions, defaultColors);
        } else {
            setValuesNoTrigger(defaultThumbPositions, defaultColors);
        }
        if (gui != null) {
            gui.updateResetButtonIcon();
        }
    }

    @Override
    protected void setEnabledState() {
        if (gradientSlider != null) {
            gradientSlider.setEnabled(isEnabled());
        }
    }

    @Override
    public boolean canBeAnimated() {
        return true;
    }

    @Override
    public GradientParamState copyState() {
        return new GradientParamState(gradientSlider.getThumbPositions(),
            gradientSlider.getColors());
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        GradientParamState gr = (GradientParamState) state;

        setValuesNoTrigger(gr.thumbPositions, gr.colors);
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

        float[] thumbPositions = new float[thumbStrings.length];
        Color[] colors = new Color[colorStrings.length];
        for (int i = 0; i < thumbStrings.length; i++) {
            thumbPositions[i] = Float.parseFloat(thumbStrings[i]);
            colors[i] = Colors.fromHTMLHex(colorStrings[i]);
        }

        setValuesNoTrigger(thumbPositions, colors);
    }

    @Override
    public Object getParamValue() {
        return List.of(gradientSlider.getValues());
    }

    @Override
    public String toString() {
        return format("%s[name = '%s']", getClass().getSimpleName(), getName());
    }

    private record GradientParamState(float[] thumbPositions,
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

        private float[] interpolatePositions(float progress, GradientParamState grEndState) {
            float[] interpolatedPositions = new float[thumbPositions.length];
            for (int i = 0; i < thumbPositions.length; i++) {
                float initial = thumbPositions[i];
                float end = grEndState.thumbPositions[i];
                interpolatedPositions[i] = ImageMath.lerp(progress, initial, end);
            }
            return interpolatedPositions;
        }

        private Color[] interpolateColors(float progress, GradientParamState grEndState) {
            Color[] interpolatedColors = new Color[colors.length];
            for (int i = 0; i < colors.length; i++) {
                Color initial = colors[i];
                Color end = grEndState.colors[i];
                interpolatedColors[i] = Colors.rgbInterpolate(initial, end, progress);
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

    static class GUI extends JPanel implements ParamGUI {
        private final GradientSlider slider;
        private final ResetButton resetButton;

        public GUI(GradientSlider slider, GradientParam gradientParam) {
            super(new FlowLayout());
            this.slider = slider;
            add(slider);
            resetButton = new ResetButton(gradientParam);
            add(resetButton);
        }

        @Override
        public void updateGUI() {

        }

        @Override
        public void setEnabled(boolean enabled) {
            slider.setEnabled(enabled);
        }

        @Override
        public void setToolTip(String tip) {
            slider.setToolTipText(tip);
        }

        @Override
        public boolean isEnabled() {
            return slider.isEnabled();
        }

        public void updateResetButtonIcon() {
            resetButton.updateIcon();
        }

        @Override
        public int getNumLayoutColumns() {
            return 2;
        }
    }
}

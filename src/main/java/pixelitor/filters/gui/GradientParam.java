/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.colors.ColorUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;

import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;

/**
 * Represents a gradient. (Note that unlike other filter parameter implementations,
 * this is not really a model for the GradientSlider GUI component,
 * the actual value is stored only inside the GradientSlider)
 */
public class GradientParam extends AbstractFilterParam {
    private static final String GRADIENT_SLIDER_USE_BEVEL = "GradientSlider.useBevel";
    private GradientSlider gradientSlider;
    private final float[] defaultThumbPositions;
    private final Color[] defaultColors;
    private boolean trigger = true; // whether the running of the filter should be triggered

    public GradientParam(String name, Color firstColor, Color secondColor) {
        this(name, new float[]{0.0f, 1.0f}, new Color[]{firstColor, secondColor});
    }

    public GradientParam(String name, float[] defaultThumbPositions, Color[] defaultColors) {
        this(name, defaultThumbPositions, defaultColors, ALLOW_RANDOMIZE);
    }

    public GradientParam(String name, float[] defaultThumbPositions, Color[] defaultColors, RandomizePolicy randomizePolicy) {
        super(name, randomizePolicy);
        this.defaultThumbPositions = defaultThumbPositions;
        this.defaultColors = defaultColors;

        // has to be created in the constructor because getValue() can be called early
        createGradientSlider(defaultThumbPositions, defaultColors);
    }

    private void createGradientSlider(float[] defaultThumbPositions, Color[] defaultColors) {
        gradientSlider = new GradientSlider(GradientSlider.HORIZONTAL, defaultThumbPositions, defaultColors);
        gradientSlider.addPropertyChangeListener(evt -> {
            if(shouldStartFilter(evt)) {
                adjustmentListener.paramAdjusted();
            }
        });
        gradientSlider.putClientProperty(GRADIENT_SLIDER_USE_BEVEL, "true");

        // if there other controls in the dialog, they will determine the horizontal size
        gradientSlider.setPreferredSize(new Dimension(250, 30));
    }

    private boolean shouldStartFilter(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(GRADIENT_SLIDER_USE_BEVEL)) {
            return false;
        }
        if (evt.getPropertyName().equals("enabled")) {
            return false;
        }

        if (trigger && !gradientSlider.isValueAdjusting() && adjustmentListener != null) {
            String propertyName = evt.getPropertyName();
            if (!"ancestor".equals(propertyName)) {
                if (!"selected thumb".equals(propertyName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public JComponent createGUI() {
        return gradientSlider;
    }

    public Colormap getValue() {
        return v -> {
            Color c = (Color) gradientSlider.getValue(v);
            if (c == null) {
                throw new IllegalStateException("null color for v = " + v);
            }
            return c.getRGB();
        };
    }

    @Override
    public int getNrOfGridBagCols() {
        return 2;
    }

    @Override
    public void randomize() {
        if (randomizePolicy.allowRandomize()) {
            Color[] randomColors = new Color[defaultThumbPositions.length];
            for (int i = 0; i < randomColors.length; i++) {
                randomColors[i] = ColorUtils.getRandomColor(false);
            }

            trigger = false;
            gradientSlider.setValues(defaultThumbPositions, randomColors);
            trigger = true;
        }
    }

    @Override
    public boolean isSetToDefault() {
        if (areThumbPositionsChanged()) {
            return false;
        }

        if (areColorsChanged()) {
            return false;
        }

        return true;
    }

    private boolean areThumbPositionsChanged() {
        float[] thumbPositions = gradientSlider.getThumbPositions();
        if(thumbPositions.length != defaultThumbPositions.length) {
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
    public void reset(boolean triggerAction) {
        if (triggerAction) {
            gradientSlider.setValues(defaultThumbPositions, defaultColors);
        } else {
            trigger = false;
            gradientSlider.setValues(defaultThumbPositions, defaultColors);
            trigger = true;
        }
    }

    @Override
    void setEnabled(boolean b) {
        if (gradientSlider != null) {
            gradientSlider.setEnabled(b);
        }
    }

    @Override
    public void considerImageSize(Rectangle bounds) {
    }

    @Override
    public boolean canBeAnimated() {
        return true;
    }

    @Override
    public ParamState copyState() {
        return new GState(gradientSlider.getThumbPositions(), gradientSlider.getColors());
    }

    @Override
    public void setState(ParamState state) {
        GState gr = (GState) state;

        trigger = false;
        createGradientSlider(gr.thumbPositions, gr.colors);
        trigger = true;
    }

    private static class GState implements ParamState {
        final float[] thumbPositions;
        final Color[] colors;

        public GState(float[] thumbPositions, Color[] colors) {
            this.thumbPositions = thumbPositions;
            this.colors = colors;
        }

        @Override
        public ParamState interpolate(ParamState endState, double progress) {
            // This will not work if the number of thumbs changes
            GState grEndState = (GState) endState;

            float[] interpolatedPositions = getInterpolatedPositions((float) progress, grEndState);

            Color[] interpolatedColors = getInterpolatedColors((float) progress, grEndState);

            return new GState(interpolatedPositions, interpolatedColors);
        }

        private float[] getInterpolatedPositions(float progress, GState grEndState) {
            float[] interpolatedPositions = new float[thumbPositions.length];
            for (int i = 0; i < thumbPositions.length; i++) {
                float initial = thumbPositions[i];
                float end = grEndState.thumbPositions[i];
                float interpolated = ImageMath.lerp(progress, initial, end);
                interpolatedPositions[i] = interpolated;
            }
            return interpolatedPositions;
        }

        private Color[] getInterpolatedColors(float progress, GState grEndState) {
            Color[] interpolatedColors = new Color[colors.length];
            for (int i = 0; i < colors.length; i++) {
                Color initial = colors[i];
                Color end = grEndState.colors[i];
                Color interpolated = ColorUtils.interpolateColor(initial, end, progress);
                interpolatedColors[i] = interpolated;
            }
            return interpolatedColors;
        }
    }

    @Override
    public String toString() {
        return String.format("%s[name = '%s']",
                getClass().getSimpleName(), getName());
    }
}

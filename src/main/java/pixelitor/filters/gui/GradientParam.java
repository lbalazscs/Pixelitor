/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters.gui;

import com.bric.swing.GradientSlider;
import com.jhlabs.image.Colormap;
import com.jhlabs.image.ImageMath;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Represents a gradient. (Note that unlike other GUIParam implementations,
 * this is not really a model for the GradientSlider GUI component,
 * the actual value is stored only inside the GradientSlider)
 */
public class GradientParam extends AbstractGUIParam {
    public static final String GRADIENT_SLIDER_USE_BEVEL = "GradientSlider.useBevel";
    private GradientSlider gradientSlider;
    private final float[] defaultThumbPositions;
    private final Color[] defaultColors;

    public GradientParam(String name, Color firstColor, Color secondColor) {
        this(name, new float[]{0.0f, 1.0f}, new Color[]{firstColor, secondColor});
    }

    public GradientParam(String name, float[] defaultThumbPositions, Color[] defaultColors) {
        super(name);
        this.defaultThumbPositions = defaultThumbPositions;
        this.defaultColors = defaultColors;

        // has to be created in the constructor because getValue() can be called early
        createGradientSlider(defaultThumbPositions, defaultColors);

//        gradientSlider.addChangeListener(new ChangeListener() {
//            @Override
//            public void stateChanged(ChangeEvent e) {
//            }
//        });
    }

    public void createGradientSlider(float[] defaultThumbPositions, Color[] defaultColors) {
        gradientSlider = new GradientSlider(GradientSlider.HORIZONTAL, defaultThumbPositions, defaultColors);
        gradientSlider.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(GRADIENT_SLIDER_USE_BEVEL)) {
                    return;
                }
                if (!dontTrigger) {
                    if (!gradientSlider.isValueAdjusting()) {
                        if (adjustmentListener != null) {
                            String propertyName = evt.getPropertyName();
                            if (!"ancestor".equals(propertyName)) {
                                if (!"selected thumb".equals(propertyName)) {
                                    adjustmentListener.paramAdjusted();
                                }
                            }
                        }
                    }
                }
            }
        });
        gradientSlider.putClientProperty(GRADIENT_SLIDER_USE_BEVEL, "true");

        // if there other controls in the dialog, they will determine the horizontal size
        gradientSlider.setPreferredSize(new Dimension(250, 30));
    }

    @Override
    public JComponent createGUI() {
        return gradientSlider;
    }

    public Colormap getValue() {
        return new Colormap() {
            @Override
            public int getColor(float v) {
                Color c = (Color) gradientSlider.getValue(v);
                if (c == null) {
                    throw new IllegalStateException("null color for v = " + v);
                }
                return c.getRGB();
            }
        };
    }

    @Override
    public int getNrOfGridBagCols() {
        return 2;
    }

    @Override
    public void randomize() {
        Color[] randomColors = new Color[defaultThumbPositions.length];
        for (int i = 0; i < randomColors.length; i++) {
            randomColors[i] = ImageUtils.getRandomColor(false);
        }

        dontTrigger = true;
        gradientSlider.setValues(defaultThumbPositions, randomColors);
        dontTrigger = false;
    }

    @Override
    public boolean isSetToDefault() {
        Color[] colors = (Color[]) gradientSlider.getValues();
        if (colors.length != defaultColors.length) {
            return false;
        }

        float[] thumbPositions = gradientSlider.getThumbPositions();
        for (int i = 0; i < thumbPositions.length; i++) {
            float thumbPosition = thumbPositions[i];
            float defaultThumbPosition = defaultThumbPositions[i];
            if (thumbPosition != defaultThumbPosition) {
                return false;
            }
        }

        for (int i = 0; i < defaultColors.length; i++) {
            Color defaultColor = defaultColors[i];
            Color actualColor = colors[i];
            if (!defaultColor.equals(actualColor)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void reset(boolean triggerAction) {
        if (!triggerAction) {
            dontTrigger = true;
        }
        gradientSlider.setValues(defaultThumbPositions, defaultColors);
        dontTrigger = false;
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
        dontTrigger = true;
        createGradientSlider(gr.thumbPositions, gr.colors);
        dontTrigger = false;
    }

    private static class GState implements ParamState {
        float[] thumbPositions;
        Color[] colors;

        public GState(float[] thumbPositions, Color[] colors) {
            this.thumbPositions = thumbPositions;
            this.colors = colors;
        }

        @Override
        public ParamState interpolate(ParamState endState, double progress) {
            // This will not work if the number of thumbs changes
            GState grEndState = (GState) endState;

            float[] interpolatedPositions = new float[thumbPositions.length];
            for (int i = 0; i < thumbPositions.length; i++) {
                float initial = thumbPositions[i];
                float end = grEndState.thumbPositions[i];
                float interpolated = ImageMath.lerp((float)progress, initial, end);
                interpolatedPositions[i] = interpolated;
            }

            Color[] interpolatedColors = new Color[colors.length];
            for (int i = 0; i < colors.length; i++) {
                Color initial = colors[i];
                Color end = grEndState.colors[i];
                // TODO interpolate in HSB space?
                Color interpolated = new Color(ImageMath.mixColors((float)progress, initial.getRGB(), end.getRGB()));
                interpolatedColors[i] = interpolated;
            }

            return new GState(interpolatedPositions, interpolatedColors);
        }
    }

    @Override
    public void setEnabledLogically(boolean b) {
        // TODO
    }

    @Override
    public void setFinalAnimationSettingMode(boolean b) {
        // ignored because this GUIParam can be animated
    }

    @Override
    public String toString() {
        return String.format("%s[name = '%s']",  // TODO add values
                getClass().getSimpleName(), getName());
    }
}

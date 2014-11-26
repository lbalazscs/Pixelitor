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
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 *
 */
public class GradientParam extends AbstractGUIParam {
    private final GradientSlider gradientSlider;

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
        gradientSlider = new GradientSlider(GradientSlider.HORIZONTAL, defaultThumbPositions, defaultColors);
        gradientSlider.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (!dontTrigger) {
                    if (!gradientSlider.isValueAdjusting()) {
                        if (adjustmentListener != null) {
                            String propertyName = evt.getPropertyName();
//                        System.out.println("GradientParam.propertyChange propertyName = \"" + propertyName + "\"");
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

//        gradientSlider.addChangeListener(new ChangeListener() {
//            @Override
//            public void stateChanged(ChangeEvent e) {
//            }
//        });
        gradientSlider.putClientProperty("GradientSlider.useBevel", "true");

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
}

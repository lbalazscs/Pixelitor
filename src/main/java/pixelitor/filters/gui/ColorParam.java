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

import com.jhlabs.image.ImageMath;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Rectangle;

/**
 * A GUIParam for selecting a color
 */
public class ColorParam extends AbstractGUIParam {
    private final Color defaultColor;
    private Color color;

    private ParamGUI paramGUI;
    private boolean allowOpacity = false;
    private final boolean allowOpacityAtRandomize;

    public ColorParam(String name, Color defaultColor, boolean allowOpacity, boolean allowOpacityAtRandomize) {
        super(name);

        if (allowOpacityAtRandomize && !allowOpacity) {
            throw new IllegalArgumentException();
        }

        this.defaultColor = defaultColor;
        this.color = defaultColor;
        this.allowOpacity = allowOpacity;
        this.allowOpacityAtRandomize = allowOpacityAtRandomize;
    }

    @Override
    public boolean isSetToDefault() {
        return color.equals(defaultColor);
    }

    @Override
    public JComponent createGUI() {
        ColorSelector gui = new ColorSelector(this);
        paramGUI = gui;
        return gui;
    }

    @Override
    public void reset(boolean triggerAction) {
        if (!triggerAction) {
            dontTrigger = true;
        }
        setColor(defaultColor);
        dontTrigger = false;
    }

    @Override
    public int getNrOfGridBagCols() {
        return 2;
    }

    @Override
    public void randomize() {
        Color c = ImageUtils.getRandomColor(allowOpacityAtRandomize);
        dontTrigger = true;
        setColor(c);
        dontTrigger = false;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color newColor) {
        if (newColor == null) {
            throw new IllegalArgumentException("newColor is null");
        }
        if (!color.equals(newColor)) {
            this.color = newColor;
            if (paramGUI != null) {
                paramGUI.updateGUI();
            }

            if (!dontTrigger) {
                if (adjustmentListener != null) {  // when called from randomize, this is null
                    adjustmentListener.paramAdjusted();
                }
            }
        }
    }

    public boolean allowOpacity() {
        return allowOpacity;
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
        return new CState(color);
    }

    @Override
    public void setState(ParamState state) {
        this.color = ((CState)state).color;
    }

    private static class CState implements ParamState {
        private Color color;

        public CState(Color color) {
            this.color = color;
        }

        @Override
        public ParamState interpolate(ParamState endState, double progress) {
            // TODO - interpolating in HSB space would be better?

            int initialRGB = color.getRGB();
            int finalRGB = ((CState)endState).color.getRGB();
            int interpolatedRGB = ImageMath.mixColors((float) progress, initialRGB, finalRGB);
            return new CState(new Color(interpolatedRGB));
        }
    }

}

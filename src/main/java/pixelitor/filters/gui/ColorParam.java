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

import pixelitor.colors.ColorHistory;
import pixelitor.colors.ColorUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Rectangle;

import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;

/**
 * A filter parameter for selecting a color
 */
public class ColorParam extends AbstractFilterParam {
    private final Color defaultColor;
    private Color color;

    private final OpacitySetting opacitySetting;

    public ColorParam(String name, Color defaultColor, OpacitySetting opacitySetting) {
        super(name, ALLOW_RANDOMIZE);

        this.defaultColor = defaultColor;
        this.color = defaultColor;
        this.opacitySetting = opacitySetting;

        ColorHistory.FILTER.add(defaultColor);
    }

    @Override
    public JComponent createGUI() {
        ColorSelector gui = new ColorSelector(this);
        paramGUI = gui;
        setParamGUIEnabledState();

        return gui;
    }

    @Override
    public boolean isSetToDefault() {
        return color.equals(defaultColor);
    }

    @Override
    public void reset(boolean triggerAction) {
        setColor(defaultColor, triggerAction);
    }

    @Override
    public int getNrOfGridBagCols() {
        return 2;
    }

    @Override
    public void randomize() {
        Color c = ColorUtils.getRandomColor(opacitySetting.allowOpacityAtRandomize);
        setColor(c, false);
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color newColor, boolean trigger) {
        assert newColor != null;
        if (!color.equals(newColor)) {
            this.color = newColor;

            ColorHistory.FILTER.add(newColor);

            if (paramGUI != null) {
                paramGUI.updateGUI();
            }

            if (trigger) {
                if (adjustmentListener != null) {  // when called from randomize, this is null
                    adjustmentListener.paramAdjusted();
                }
            }
        }
    }

    public boolean allowOpacity() {
        return opacitySetting.allowOpacity;
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
        private final Color color;

        public CState(Color color) {
            this.color = color;
        }

        @Override
        public ParamState interpolate(ParamState endState, double progress) {
            Color endColor = ((CState) endState).color;
            return new CState(ColorUtils.interpolateColor(color, endColor, (float) progress));
        }
    }

    @Override
    public String toString() {
        return String.format("%s[name = '%s', color = '%s']",
                getClass().getSimpleName(), getName(), color.toString());
    }

    public enum OpacitySetting {
        NO_OPACITY(false, false),
        USER_ONLY_OPACITY(true, false),
        FREE_OPACITY(true, true);

        private final boolean allowOpacity;
        private final boolean allowOpacityAtRandomize;

        OpacitySetting(boolean allowOpacity, boolean allowOpacityAtRandomize) {
            this.allowOpacity = allowOpacity;
            this.allowOpacityAtRandomize = allowOpacityAtRandomize;
        }
    }
}

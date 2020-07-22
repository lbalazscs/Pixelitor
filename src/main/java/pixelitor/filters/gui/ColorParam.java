/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import java.util.Objects;

import static java.lang.String.format;
import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;

/**
 * A filter parameter for selecting a color
 */
public class ColorParam extends AbstractFilterParam {
    private final Color defaultColor;
    private Color color;

    private final TransparencyPolicy transparencyPolicy;

    public ColorParam(String name, Color defaultColor, TransparencyPolicy transparencyPolicy) {
        super(name, ALLOW_RANDOMIZE);

        this.defaultColor = defaultColor;
        color = defaultColor;
        this.transparencyPolicy = transparencyPolicy;

        ColorHistory.FILTER.add(defaultColor);
    }

    @Override
    public JComponent createGUI() {
        var gui = new ColorParamGUI(this, true);
        paramGUI = gui;
        setGUIEnabledState();

        return gui;
    }

    @Override
    public boolean isSetToDefault() {
        return Objects.equals(color, defaultColor);
    }

    @Override
    public void reset(boolean trigger) {
        setColor(defaultColor, trigger);
    }

    @Override
    protected void doRandomize() {
        setColor(Rnd.createRandomColor(
                transparencyPolicy.allowTransparencyWhenRandomized), false);
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color newColor, boolean trigger) {
        assert newColor != null;
        if (Objects.equals(color, newColor)) {
            return;
        }
        color = newColor;

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

    public boolean allowTransparency() {
        return transparencyPolicy.allowTransparency;
    }

    @Override
    public boolean canBeAnimated() {
        return true;
    }

    @Override
    public ColorParamState copyState() {
        return new ColorParamState(color);
    }

    @Override
    public void setState(ParamState<?> state) {
        color = ((ColorParamState) state).color;
    }

    @Override
    public Object getParamValue() {
        return color;
    }

    @Override
    public String toString() {
        return format("%s[name = '%s', color = '%s']",
                getClass().getSimpleName(), getName(), color);
    }

    private static class ColorParamState implements ParamState<ColorParamState> {
        private final Color color;

        public ColorParamState(Color color) {
            this.color = color;
        }

        @Override
        public ColorParamState interpolate(ColorParamState endState, double progress) {
            return new ColorParamState(Colors.interpolateInRGB(
                color, endState.color, (float) progress));
        }

        @Override
        public String toString() {
            return format("%s[color=%s]",
                    getClass().getSimpleName(), color);
        }
    }

    public enum TransparencyPolicy {
        /**
         * Transparent colors can't be selected
         */
        NO_TRANSPARENCY(false, false),

        /**
         * The user can select an alpha value, but randomizing will
         * always use opaque colors
         */
        USER_ONLY_TRANSPARENCY(true, false),

        /**
         * The user can select an alpha value, and randomizing will
         * with randomize the alpha
         */
        FREE_TRANSPARENCY(true, true);

        private final boolean allowTransparency;
        private final boolean allowTransparencyWhenRandomized;

        TransparencyPolicy(boolean allowTransparency, boolean allowTransparencyWhenRandomized) {
            this.allowTransparency = allowTransparency;
            this.allowTransparencyWhenRandomized = allowTransparencyWhenRandomized;
        }
    }
}

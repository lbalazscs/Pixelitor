/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.Icons;
import pixelitor.utils.Rnd;

import javax.swing.*;
import java.awt.Color;
import java.io.Serial;
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

    public ColorParam(String name, Color defaultColor,
                      TransparencyPolicy transparencyPolicy) {
        super(name, ALLOW_RANDOMIZE);

        this.defaultColor = defaultColor;
        color = defaultColor;
        this.transparencyPolicy = transparencyPolicy;

        ColorHistory.remember(defaultColor);
    }

    @Override
    public JComponent createGUI() {
        var gui = new ColorParamGUI(this, action, true);
        paramGUI = gui;
        guiCreated();

        return gui;
    }

    public ColorParam withRandomizeAction() {
        String toolTip = "<html>Randomize the color of <b>" + getName() + "</b>";
        action = new FilterButtonModel("",
            this::randomize, Icons.getDiceIcon(),
            toolTip, null);

        return this;
    }

    @Override
    public boolean hasDefault() {
        return Objects.equals(color, defaultColor);
    }

    @Override
    public void reset(boolean trigger) {
        setColor(defaultColor, trigger);
    }

    @Override
    protected void doRandomize() {
        setColor(Rnd.createRandomColor(
            transparencyPolicy.randomizeTransparency), false);
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

        ColorHistory.remember(newColor);

        if (paramGUI != null) {
            paramGUI.updateGUI();
        }

        if (trigger) {
            if (adjustmentListener != null) {  // when called from randomize, this is null
                adjustmentListener.paramAdjusted();
            }
        }
    }

    public void darker() {
        setColor(color.darker(), false);
    }

    public void brighter() {
        setColor(color.brighter(), false);
    }

    public boolean allowTransparency() {
        return transparencyPolicy.allowTransparency;
    }

    @Override
    public boolean isAnimatable() {
        return true;
    }

    @Override
    public ColorParamState copyState() {
        return new ColorParamState(color);
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        Color newColor = ((ColorParamState) state).color;
        if (updateGUI) {
            setColor(newColor, false);
        } else {
            color = newColor;
        }
    }

    @Override
    public void loadStateFrom(String savedValue) {
        try {
            Color newColor = Colors.fromHTMLHex(savedValue);
            setColor(newColor, false);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Could not parse " + savedValue);
        }
    }

    @Override
    public Color getParamValue() {
        return color;
    }

    @Override
    public String toString() {
        return format("%s[name = '%s', color = '%s']",
            getClass().getSimpleName(), getName(), color);
    }

    public record ColorParamState(Color color) implements ParamState<ColorParamState> {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public ColorParamState interpolate(ColorParamState endState, double progress) {
            return new ColorParamState(Colors.interpolateRGB(
                color, endState.color, progress));
        }

        @Override
        public String toSaveString() {
            return Colors.toHTMLHex(color, true);
        }
    }

    public enum TransparencyPolicy {
        /**
         * Transparent colors can't be selected
         */
        NO_TRANSPARENCY(false, false),

        /**
         * The user can select an alpha value, but randomizing will
         * always create opaque colors
         */
        USER_ONLY_TRANSPARENCY(true, false),

        /**
         * The user can select an alpha value, and randomizing will
         * with randomize the alpha
         */
        FREE_TRANSPARENCY(true, true);

        private final boolean allowTransparency;
        private final boolean randomizeTransparency;

        TransparencyPolicy(boolean allowTransparency, boolean randomizeTransparency) {
            this.allowTransparency = allowTransparency;
            this.randomizeTransparency = randomizeTransparency;
        }
    }
}

/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.ImageMath;

import javax.swing.*;
import java.awt.Rectangle;

import static java.lang.String.format;
import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;

/**
 * A filter parameter for selecting an image coordinate (relative to the image size)
 */
public class ImagePositionParam extends AbstractFilterParam {
    private float relativeX = 0.5f;
    private float relativeY = 0.5f;

    private float defaultRelativeX = 0.5f;
    private float defaultRelativeY = 0.5f;

    public ImagePositionParam(String name) {
        super(name, ALLOW_RANDOMIZE);
    }

    public ImagePositionParam(String name, float relX, float relY) {
        super(name, ALLOW_RANDOMIZE);
        this.relativeX = relX;
        this.relativeY = relY;
        defaultRelativeX = relX;
        defaultRelativeY = relY;
    }

    @Override
    public JComponent createGUI() {
        int defaultX = (int) (100 * defaultRelativeX);
        int defaultY = (int) (100 * defaultRelativeX);

        ImagePositionParamGUI gui = new ImagePositionParamGUI(this, defaultX, defaultY);
        paramGUI = gui;
        setParamGUIEnabledState();
        paramGUI.updateGUI();
        return gui;
    }

    @Override
    public boolean isSetToDefault() {
        return relativeX == defaultRelativeX && relativeY == defaultRelativeY;
    }

    @Override
    public void reset(boolean trigger) {
        setRelativeValues(defaultRelativeX, defaultRelativeY, true, false, trigger);
    }

    @Override
    public int getNumGridBagCols() {
        return 1;
    }

    @Override
    public void randomize() {
        float rx = (float) Math.random();
        float ry = (float) Math.random();

        setRelativeValues(rx, ry, true, false, false);
    }

    public float getRelativeX() {
        return relativeX;
    }

    public float getRelativeY() {
        return relativeY;
    }

    public void setRelativeValues(float relX, float relY, boolean updateGUI, boolean isAdjusting, boolean trigger) {
        this.relativeX = relX;
        this.relativeY = relY;
        if (updateGUI && (paramGUI != null)) {
            paramGUI.updateGUI();
        }
        if (trigger && !isAdjusting) {
            adjustmentListener.paramAdjusted();
        }
    }

    public void setRelativeX(float x, boolean isAdjusting) {
        setRelativeValues(x, relativeY, false, isAdjusting, true);
    }

    public void setRelativeY(float y, boolean isAdjusting) {
        setRelativeValues(relativeX, y, false, isAdjusting, true);
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
        return new IPPState(relativeX, relativeY);
    }

    @Override
    public void setState(ParamState state) {
        IPPState s = (IPPState) state;
        relativeX = (float) s.relativeX;
        relativeY = (float) s.relativeY;
    }

    private static class IPPState implements ParamState {
        private final double relativeX;
        private final double relativeY;

        public IPPState(double relX, double relY) {
            this.relativeX = relX;
            this.relativeY = relY;
        }

        @Override
        public ParamState interpolate(ParamState endState, double progress) {
            IPPState ippEndState = (IPPState) endState;
            double interpolatedX = ImageMath.lerp(progress, relativeX, ippEndState.relativeX);
            double interpolatedY = ImageMath.lerp(progress, relativeY, ippEndState.relativeY);
            return new IPPState(interpolatedX, interpolatedY);
        }
    }

    @Override
    public String toString() {
        return format("%s[name = '%s', relativeX= %.2f, relativeY= %.2f]",
                getClass().getSimpleName(), getName(), relativeX, relativeY);
    }
}

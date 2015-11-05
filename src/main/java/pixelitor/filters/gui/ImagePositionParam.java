/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
import pixelitor.utils.UpdateGUI;

import javax.swing.*;
import java.awt.Rectangle;

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

    public ImagePositionParam(String name, float relativeX, float relativeY) {
        super(name, ALLOW_RANDOMIZE);
        this.relativeX = relativeX;
        this.relativeY = relativeY;
        defaultRelativeX  = relativeX;
        defaultRelativeY = relativeY;
    }

    @Override
    public JComponent createGUI() {
        int defaultX = (int) (100 * defaultRelativeX);
        int defaultY = (int) (100 * defaultRelativeX);

        ImagePositionPanel selector = new ImagePositionPanel(this, defaultX, defaultY);
        paramGUI = selector;
        setParamGUIEnabledState();
        paramGUI.updateGUI();
        return selector;
    }

    @Override
    public boolean isSetToDefault() {
        return relativeX == defaultRelativeX && relativeY == defaultRelativeY;
    }

    @Override
    public void reset(boolean triggerAction) {
        setRelativeValues(defaultRelativeX, defaultRelativeY, UpdateGUI.YES, false, triggerAction);
    }

    @Override
    public int getNrOfGridBagCols() {
        return 1;
    }

    @Override
    public void randomize() {
        float rx = (float) Math.random();
        float ry = (float) Math.random();

        setRelativeValues(rx, ry, UpdateGUI.YES, false, false);
    }

    public float getRelativeX() {
        return relativeX;
    }

    public float getRelativeY() {
        return relativeY;
    }

    public void setRelativeValues(float relativeX, float relativeY, UpdateGUI updateGUI, boolean isAdjusting, boolean trigger) {
        this.relativeX = relativeX;
        this.relativeY = relativeY;
        if (updateGUI.isYes() && (paramGUI != null)) {
            paramGUI.updateGUI();
        }
        if (trigger && !isAdjusting) {
            adjustmentListener.paramAdjusted();
        }
    }

    public void setRelativeX(float newRelativeX, boolean isAdjusting) {
        setRelativeValues(newRelativeX, relativeY, UpdateGUI.NO, isAdjusting, true);
    }

    public void setRelativeY(float newRelativeY, boolean isAdjusting) {
        setRelativeValues(relativeX, newRelativeY, UpdateGUI.NO, isAdjusting, true);
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

        public IPPState(double relativeX, double relativeY) {
            this.relativeX = relativeX;
            this.relativeY = relativeY;
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
        return String.format("%s[name = '%s', relativeX= %.2f, relativeY= %.2f]",
                getClass().getSimpleName(), getName(), relativeX, relativeY);
    }
}

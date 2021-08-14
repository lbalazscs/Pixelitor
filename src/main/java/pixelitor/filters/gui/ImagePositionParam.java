/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import java.awt.geom.Point2D;

import static java.lang.String.format;
import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;

/**
 * A filter parameter for selecting an image coordinate (relative to the image size)
 */
public class ImagePositionParam extends AbstractFilterParam {
    private float relativeX = 0.5f;
    private float relativeY = 0.5f;
    private int decimalPlaces = 1;

    private float defaultRelativeX = 0.5f;
    private float defaultRelativeY = 0.5f;

    public ImagePositionParam(String name) {
        super(name, ALLOW_RANDOMIZE);
    }

    public ImagePositionParam(String name, float relX, float relY) {
        super(name, ALLOW_RANDOMIZE);
        relativeX = relX;
        relativeY = relY;
        defaultRelativeX = relX;
        defaultRelativeY = relY;
    }

    @Override
    public JComponent createGUI() {
        double defaultX = 100 * defaultRelativeX;
        double defaultY = 100 * defaultRelativeY;

        var gui = new ImagePositionParamGUI(this, defaultX, defaultY);
        paramGUI = gui;
        afterGUICreation();
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
    protected void doRandomize() {
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
        relativeX = relX;
        relativeY = relY;
        if (updateGUI && paramGUI != null) {
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
    public boolean canBeAnimated() {
        return true;
    }

    public ImagePositionParam withDecimalPlaces(int dp) {
        decimalPlaces = dp;
        return this;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    @Override
    public ImagePositionParamState copyState() {
        return new ImagePositionParamState(relativeX, relativeY);
    }

    @Override
    public void loadStateFrom(ParamState<?> state, boolean updateGUI) {
        ImagePositionParamState s = (ImagePositionParamState) state;
        float newRelX = (float) s.relativeX;
        float newRelY = (float) s.relativeY;

        if (updateGUI) {
            setRelativeValues(newRelX, newRelY, true, false, false);
        } else {
            this.relativeX = newRelX;
            this.relativeY = newRelY;
        }
    }

    @Override
    public void loadStateFrom(String savedValue) {
        int commaIndex = savedValue.indexOf(',');
        float newRelX = Float.parseFloat(savedValue.substring(0, commaIndex));
        float newRelY = Float.parseFloat(savedValue.substring(commaIndex + 1));
        setRelativeValues(newRelX, newRelY, true, false, false);
    }

    @Override
    public Object getParamValue() {
        return new Point2D.Float(getRelativeX(), getRelativeY());
    }

    @Override
    public String toString() {
        return format("%s[name = '%s', relativeX= %.2f, relativeY= %.2f]",
            getClass().getSimpleName(), getName(), relativeX, relativeY);
    }

    private record ImagePositionParamState(double relativeX,
                                           double relativeY) implements ParamState<ImagePositionParamState> {
        @Override
        public ImagePositionParamState interpolate(ImagePositionParamState endState, double progress) {
            double interpolatedX = ImageMath.lerp(progress, relativeX, endState.relativeX);
            double interpolatedY = ImageMath.lerp(progress, relativeY, endState.relativeY);
            return new ImagePositionParamState(interpolatedX, interpolatedY);
        }

        @Override
        public String toSaveString() {
            // mandelbrot has 2 decimal places
            return "%.4f,%.4f".formatted(relativeX, relativeY);
        }
    }
}

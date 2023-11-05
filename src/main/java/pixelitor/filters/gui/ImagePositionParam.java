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

import com.jhlabs.image.ImageMath;

import javax.swing.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.Serial;

import static java.lang.String.format;
import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;

/**
 * A filter parameter for selecting an image coordinate (relative to the image size).
 */
public class ImagePositionParam extends AbstractFilterParam {
    private double relativeX = 0.5;
    private double relativeY = 0.5;
    private int decimalPlaces = 1;

    private double defaultRelativeX = 0.5;
    private double defaultRelativeY = 0.5;

    public ImagePositionParam(String name) {
        super(name, ALLOW_RANDOMIZE);
    }

    public ImagePositionParam(String name, double relX, double relY) {
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
        guiCreated();
        paramGUI.updateGUI();
        return gui;
    }

    @Override
    public boolean hasDefault() {
        return relativeX == defaultRelativeX && relativeY == defaultRelativeY;
    }

    @Override
    public void reset(boolean trigger) {
        setRelativeValues(defaultRelativeX, defaultRelativeY, true, false, trigger);
    }

    @Override
    protected void doRandomize() {
        double rx = Math.random();
        double ry = Math.random();

        setRelativeValues(rx, ry, true, false, false);
    }

    public double getRelativeX() {
        return relativeX;
    }

    public double getRelativeY() {
        return relativeY;
    }

    public Point2D getRelativePoint() {
        return new Point2D.Double(relativeX, relativeY);
    }

    public Point2D getAbsolutePoint(BufferedImage src) {
        return new Point2D.Double(relativeX * src.getWidth(), relativeY * src.getHeight());
    }

    public void setRelativeValues(double relX, double relY, boolean updateGUI, boolean isAdjusting, boolean trigger) {
        relativeX = relX;
        relativeY = relY;
        if (updateGUI && paramGUI != null) {
            paramGUI.updateGUI();
        }
        if (trigger && !isAdjusting) {
            adjustmentListener.paramAdjusted();
        }
    }

    public void setRelativeX(double x, boolean isAdjusting) {
        setRelativeValues(x, relativeY, false, isAdjusting, true);
    }

    public void setRelativeY(double y, boolean isAdjusting) {
        setRelativeValues(relativeX, y, false, isAdjusting, true);
    }

    @Override
    public boolean isAnimatable() {
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
        double newRelX = s.relativeX;
        double newRelY = s.relativeY;

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
        double newRelX = Double.parseDouble(savedValue.substring(0, commaIndex));
        double newRelY = Double.parseDouble(savedValue.substring(commaIndex + 1));
        setRelativeValues(newRelX, newRelY, true, false, false);
    }

    @Override
    public Point2D getParamValue() {
        return new Point2D.Double(getRelativeX(), getRelativeY());
    }

    @Override
    public String toString() {
        return format("%s[name = '%s', relativeX= %.2f, relativeY= %.2f]",
            getClass().getSimpleName(), getName(), relativeX, relativeY);
    }

    public record ImagePositionParamState(double relativeX,
                                           double relativeY) implements ParamState<ImagePositionParamState> {
        @Serial
        private static final long serialVersionUID = 1L;

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

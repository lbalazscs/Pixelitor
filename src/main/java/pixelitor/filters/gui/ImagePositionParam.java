/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import static pixelitor.filters.gui.RandomizeMode.ALLOW_RANDOMIZE;

/**
 * A {@link FilterParam} that allows selecting a point within an image using
 * relative coordinates (0.0 to 1.0) that are independent of the actual image size.
 */
public class ImagePositionParam extends AbstractFilterParam {
    private double relativeX;
    private double relativeY;
    private int decimalPlaces = 1;

    private final double defaultRelativeX;
    private final double defaultRelativeY;

    public ImagePositionParam(String name) {
        this(name, 0.5, 0.5); // center position
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
        var gui = new ImagePositionParamGUI(this, defaultRelativeX, defaultRelativeY);
        paramGUI = gui;
        syncWithGui();
        paramGUI.updateGUI();
        return gui;
    }

    @Override
    protected void doRandomize() {
        setRelativePosition(Math.random(), Math.random(), true, false, false);
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

    /**
     * Returns the current position as absolute pixel coordinates for a given image.
     */
    public Point2D getAbsolutePoint(BufferedImage image) {
        return new Point2D.Double(
            relativeX * image.getWidth(),
            relativeY * image.getHeight());
    }

    public void setRelativePosition(double relX, double relY,
                                    boolean updateGUI, boolean isAdjusting,
                                    boolean trigger) {
        relativeX = relX;
        relativeY = relY;
        if (updateGUI && paramGUI != null) {
            paramGUI.updateGUI();
        }
        if (trigger && !isAdjusting) {
            adjustmentListener.paramAdjusted();
        }
    }

    void setRelativeX(double x, boolean isAdjusting) {
        setRelativePosition(x, relativeY, false, isAdjusting, true);
    }

    void setRelativeY(double y, boolean isAdjusting) {
        setRelativePosition(relativeX, y, false, isAdjusting, true);
    }

    @Override
    public boolean isAtDefault() {
        return relativeX == defaultRelativeX && relativeY == defaultRelativeY;
    }

    @Override
    public void reset(boolean trigger) {
        setRelativePosition(defaultRelativeX, defaultRelativeY, true, false, trigger);
    }

    @Override
    public boolean isAnimatable() {
        return true;
    }

    public ImagePositionParam withDecimalPlaces(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
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
        setRelativePosition(s.relativeX, s.relativeY, updateGUI, false, false);
    }

    @Override
    public void loadStateFrom(String savedValue) {
        int commaIndex = savedValue.indexOf(',');
        double newRelX = Double.parseDouble(savedValue.substring(0, commaIndex));
        double newRelY = Double.parseDouble(savedValue.substring(commaIndex + 1));
        setRelativePosition(newRelX, newRelY, true, false, false);
    }

    @Override
    public String getValueAsString() {
        return new Point2D.Double(getRelativeX(), getRelativeY()).toString();
    }

    @Override
    public String toString() {
        return String.format("%s[name = '%s', relativeX= %.2f, relativeY= %.2f]",
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
            // 4 because range param has max 2 digits after the decimal point
            return "%.4f,%.4f".formatted(relativeX, relativeY);
        }
    }
}

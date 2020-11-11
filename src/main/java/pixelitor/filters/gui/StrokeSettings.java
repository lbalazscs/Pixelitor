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

import com.jhlabs.image.ImageMath;
import pixelitor.tools.shapes.BasicStrokeCap;
import pixelitor.tools.shapes.BasicStrokeJoin;
import pixelitor.tools.shapes.ShapeType;
import pixelitor.tools.shapes.StrokeType;

/**
 * Represents the configuration of a stroke.
 * It also functions as the {@link ParamState} of {@link StrokeParam}
 */
public class StrokeSettings implements ParamState<StrokeSettings> {
    private final double width;
    private final BasicStrokeCap cap;
    private final BasicStrokeJoin join;
    private final StrokeType type;
    private final ShapeType shapeType;
    private final boolean dashed;

    public StrokeSettings(double width, BasicStrokeCap cap, BasicStrokeJoin join,
                          StrokeType type, ShapeType shapeType, boolean dashed) {
        this.width = width;
        this.cap = cap;
        this.join = join;
        this.type = type;
        this.shapeType = shapeType;
        this.dashed = dashed;
    }

    @Override
    public StrokeSettings interpolate(StrokeSettings endState, double progress) {
        // the stroke width is the only thing that can be animated
        double newWidth = ImageMath.lerp(progress, width, endState.width);

        return new StrokeSettings(newWidth, cap, join, type, shapeType, dashed);
    }

    public double getWidth() {
        return width;
    }

    public BasicStrokeCap getCap() {
        return cap;
    }

    public BasicStrokeJoin getJoin() {
        return join;
    }

    public StrokeType getType() {
        return type;
    }

    public ShapeType getShapeType() {
        return shapeType;
    }

    public boolean isDashed() {
        return dashed;
    }

    @Override
    public String toSaveString() {
        throw new UnsupportedOperationException();
    }
}

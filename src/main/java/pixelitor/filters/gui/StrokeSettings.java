/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.tools.shapes.ShapeType;
import pixelitor.tools.shapes.StrokeCap;
import pixelitor.tools.shapes.StrokeJoin;
import pixelitor.tools.shapes.StrokeType;

import java.io.Serial;

import static java.lang.String.format;

/**
 * Represents the configuration of a stroke.
 * It also functions as the {@link ParamState} of {@link StrokeParam}
 */
public record StrokeSettings(double width, StrokeCap cap, StrokeJoin join,
                             StrokeType type, ShapeType shapeType, boolean dashed)
    implements ParamState<StrokeSettings> {

    @Serial
    private static final long serialVersionUID = 1L;

    public static StrokeSettings defaultsWith(StrokeType type, double width) {
        return new StrokeSettings(width, StrokeCap.ROUND, StrokeJoin.ROUND,
            type, StrokeParam.DEFAULT_SHAPE_TYPE, false);
    }

    @Override
    public StrokeSettings interpolate(StrokeSettings endState, double progress) {
        // the stroke width is the only thing that can be animated
        double newWidth = ImageMath.lerp(progress, width, endState.width);

        return new StrokeSettings(newWidth, cap, join, type, shapeType, dashed);
    }

    @Override
    public String toSaveString() {
        throw new UnsupportedOperationException();
    }

    public String toSVGString() {
        String svg = format("stroke-width=\"%.2f\" %s %s",
            width, cap.toSVG(), join.toSVG());
        if (dashed) {
            svg += format(" stroke-dasharray=\"%.2f %.2f\"", 2 * width, 2 * width);
        }
        return svg;
    }
}

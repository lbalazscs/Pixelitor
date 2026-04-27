/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.BoxBlurFilter;
import com.jhlabs.image.VariableBlurFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.*;
import pixelitor.utils.BlurredShape;
import pixelitor.utils.ImageUtils;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * Focus filter based on the JHLabs {@link VariableBlurFilter}.
 */
public class JHFocus extends ParametrizedFilter {
    public static final String NAME = "Focus";

    @Serial
    private static final long serialVersionUID = 6331340888057548063L;

    private final ImagePositionParam center = new ImagePositionParam("Focused Area Center");
    private final GroupedRangeParam radius = new GroupedRangeParam("Focused Area Radius (Pixels)", 0, 200, 1000, false);
    private final RangeParam softness = new RangeParam("Transition Softness", 0, 20, 100);
    private final GroupedRangeParam blurRadius = new GroupedRangeParam("Blur Radius", 0, 10, 48);
    private final RangeParam numIterations = new RangeParam("Blur Iterations (Quality)", 1, 3, 10);
    private final BooleanParam invert = new BooleanParam("Invert");
    private final BooleanParam hpSharpening = BooleanParam.forHPSharpening();
    private final IntChoiceParam shape = BlurredShape.getChoices();

    public JHFocus() {
        super(true);

        initParams(
            center.withDecimalPlaces(0),
            radius,
            softness,
            shape,
            blurRadius,
            numIterations,
            invert,
            hpSharpening
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int hRadius = blurRadius.getHorizontal();
        int vRadius = blurRadius.getVertical();
        if (hRadius == 0 && vRadius == 0) {
            return src;
        }
        if (radius.getHorizontal() == 0 || radius.getVertical() == 0) {
            if (invert.isChecked()) {
                return src;
            }
            return new BoxBlurFilter(getName(), hRadius, vRadius, numIterations.getValue()).filter(src, dest);
        }

        double cx = src.getWidth() * center.getRelativeX();
        double cy = src.getHeight() * center.getRelativeY();
        double radiusX = radius.getValueAsDouble(0);
        double radiusY = radius.getValueAsDouble(1);
        double softnessFactor = softness.getPercentage();
        boolean inverted = invert.isChecked();
        float blurHorRadius = blurRadius.getValueAsFloat(0);
        float blurVerRadius = blurRadius.getValueAsFloat(1);
        int iterations = numIterations.getValue();
        int shapeType = shape.getValue();

        FocusImpl filter = new FocusImpl(
            NAME,
            blurHorRadius,
            blurVerRadius,
            iterations,
            cx,
            cy,
            radiusX,
            radiusY,
            softnessFactor,
            inverted,
            shapeType
        );

        dest = filter.filter(src, dest);

        if (hpSharpening.isChecked()) {
            dest = ImageUtils.toHighPassSharpenedImage(src, dest);
        }

        return dest;
    }

    @Override
    public boolean supportsGray() {
        return !hpSharpening.isChecked();
    }

    private static class FocusImpl extends VariableBlurFilter {
        private final boolean inverted;
        private final BlurredShape shape;

        public FocusImpl(String filterName, float hRadius, float vRadius, int iterations,
                         double cx, double cy, double radiusX, double radiusY, double softness,
                         boolean inverted, int shapeType) {

            // instead of using a blur mask, we override blurRadiusAt
            super(filterName, hRadius, vRadius, iterations, null, true);

            Point2D center = new Point2D.Double(cx, cy);

            double innerRadiusX = radiusX - radiusX * softness;
            double innerRadiusY = radiusY - radiusY * softness;
            double outerRadiusX = radiusX + radiusX * softness;
            double outerRadiusY = radiusY + radiusY * softness;

            this.inverted = inverted;

            this.shape = BlurredShape.create(shapeType, center,
                innerRadiusX, innerRadiusY,
                outerRadiusX, outerRadiusY);
        }

        @Override
        protected float blurRadiusAt(int x, int y) {
            double outside = shape.isOutside(x, y);
            if (inverted) {
                return (float) (1 - outside);
            }
            return (float) outside;
        }
    }
}

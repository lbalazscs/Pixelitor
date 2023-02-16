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

package pixelitor.filters;

import net.jafama.FastMath;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;

import java.awt.geom.Path2D;

/**
 * A shape filter rendering a spiral.
 */
public class Spiral extends ShapeFilter {
    public static final String NAME = "Spiral";
    private static final double LOG_PHI = 0.1;

    private static final int NUM_STEPS_PER_SPIN = 100;
    private static final int TYPE_CIRCULAR = 0;
    private static final int TYPE_POLYGON = 1;

    private final RangeParam numSpinsParam = new RangeParam("Number of Spins",
        1, 3, 10);
    private final IntChoiceParam typeParam = new IntChoiceParam("Type", new Item[]{
        new Item("Circular", TYPE_CIRCULAR),
        new Item("Polygon", TYPE_POLYGON)
    });
    private final RangeParam sidesParam = new RangeParam("Polygon Sides", 3, 4, 10);
    private final BooleanParam symmetry = new BooleanParam("Symmetric", false);
    private final BooleanParam log = new BooleanParam("Logarithmic", false);
    private final BooleanParam scale = new BooleanParam("Scale", true);

    public Spiral() {
        addParamsToFront(
            numSpinsParam,
            typeParam,
            sidesParam,
            symmetry,
            log,
            scale
        );

        typeParam.setupEnableOtherIf(sidesParam, type ->
            type.valueIs(TYPE_POLYGON));

        helpURL = "https://en.wikipedia.org/wiki/Spiral";
    }

    @Override
    protected Path2D createShape(int width, int height) {
        Path2D shape = new Path2D.Double();

        double cx = width * center.getRelativeX();
        double cy = height * center.getRelativeY();

        int numSpins = numSpinsParam.getValue();

        double w = width / 2.0;
        double h = height / 2.0;
        if (!scale.isChecked()) {
            w = h = Math.max(w, h);
        }

        double maxAngle = 2 * Math.PI * numSpins;
        int type = typeParam.getValue();
        if (type == TYPE_POLYGON) {
            // compensate rounding errors - otherwise
            // the last polygon segment might not be drawn
            maxAngle += 0.001;
        }

        double dt = switch (type) {
            case TYPE_CIRCULAR -> maxAngle / (NUM_STEPS_PER_SPIN * numSpins);
            case TYPE_POLYGON -> (2 * Math.PI) / sidesParam.getValue();
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };

        boolean logarithmic = log.isChecked();
        double a = 1.0 / maxAngle;
        double maxCorr = 1;
        if (logarithmic) {
            maxCorr = FastMath.pow(Math.E, maxAngle * LOG_PHI);
        }

        shape.moveTo(cx, cy);
        for (double t = dt; t <= maxAngle; t += dt) {
            double x, y;
            if (logarithmic) {
                double logCorr = FastMath.pow(Math.E, t * LOG_PHI) / maxCorr;
                x = w * a * t * FastMath.cos(t) * logCorr + cx;
                y = h * a * t * FastMath.sin(t) * logCorr + cy;
            } else {
                x = w * a * t * FastMath.cos(t) + cx;
                y = h * a * t * FastMath.sin(t) + cy;
            }
            shape.lineTo(x, y);
        }
        if (symmetry.isChecked()) {
            shape.moveTo(cx, cy);
            for (double t = dt; t <= maxAngle; t += dt) {
                double x, y;
                if (logarithmic) {
                    double logCorr = FastMath.pow(Math.E, t * LOG_PHI) / maxCorr;
                    x = -w * a * t * FastMath.cos(t) * logCorr + cx;
                    y = -h * a * t * FastMath.sin(t) * logCorr + cy;
                } else {
                    x = -w * a * t * FastMath.cos(t) + cx;
                    y = -h * a * t * FastMath.sin(t) + cy;
                }
                shape.lineTo(x, y);
            }
        }

        return shape;
    }

    @Override
    protected float getGradientRadius(float cx, float cy) {
        return Math.min(cx, cy);
    }
}
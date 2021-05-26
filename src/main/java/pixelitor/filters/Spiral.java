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

package pixelitor.filters;

import net.jafama.FastMath;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.FilterSetting;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.geom.Path2D;

/**
 * A shape filter rendering a spiral.
 */
public class Spiral extends ShapeFilter {
    public static final String NAME = "Spiral";

    private static final int NUM_STEPS_PER_SPIN = 100;

    private final RangeParam numSpinsParam = new RangeParam("Number of Spins",
            1, 3, 10);
    private final IntChoiceParam typeParam = new IntChoiceParam("Type", new IntChoiceParam.Item[]{
            new IntChoiceParam.Item("Circular", 0),
            new IntChoiceParam.Item("Polygon", 1)
    });
    private final RangeParam sidesParam = new RangeParam("Sides", 3, 4, 10);
    private final BooleanParam symmetry = new BooleanParam("Symmetric", false);
    private final BooleanParam scale = new BooleanParam("Scale", true);

    public Spiral() {
        sidesParam.setEnabled(false, FilterSetting.EnabledReason.APP_LOGIC);

        addParamsToFront(
                numSpinsParam,
                typeParam,
                sidesParam,
                symmetry,
                scale
        );

        typeParam.addListDataListener(new TypeParamListener());

        helpURL = "https://en.wikipedia.org/wiki/Spiral";
    }

    @Override
    protected Path2D createShape(int width, int height) {
        return switch (typeParam.getValue()) {
            default -> createCircular(width, height);
            case 1 -> createPolygonal(width, height);
        };
    }

    private Path2D createCircular(int width, int height) {
        Path2D shape = new Path2D.Double();

        double cx = width * center.getRelativeX();
        double cy = height * center.getRelativeY();

        int numSpins = numSpinsParam.getValue();

        double w = width / 2.0;
        double h = height / 2.0;
        double maxAngle = 2 * Math.PI * numSpins;
        double dt = maxAngle / (NUM_STEPS_PER_SPIN * numSpins);
        double a = 1.0 / maxAngle;

        shape.moveTo(cx, cy);
        for (double t = dt; t < maxAngle; t += dt) {
            double x = w * a * t * FastMath.cos(t) + cx;
            double y = h * a * t * FastMath.sin(t) + cy;
            shape.lineTo(x, y);
        }
        if (symmetry.isChecked()) {
            shape.moveTo(cx, cy);
            for (double t = dt; t < maxAngle; t += dt) {
                double x = -w * a * t * FastMath.cos(t) + cx;
                double y = -h * a * t * FastMath.sin(t) + cy;
                shape.lineTo(x, y);
            }
        }

        return shape;
    }

    private Path2D createPolygonal(int width, int height) {
        Path2D shape = new Path2D.Double();

        double cx = width * center.getRelativeX();
        double cy = height * center.getRelativeY();

        int numSpins = numSpinsParam.getValue();

        double w = width / 2.0;
        double h = height / 2.0;

        if (!scale.isChecked())
            w = h = Math.max(w, h);

        double maxAngle = 2 * Math.PI * numSpins;
        double dt = 2 * Math.PI / sidesParam.getValue();
        System.out.println(sidesParam.getValue());
        System.out.println(dt / Math.PI);
        double a = 1.0 / maxAngle;

        shape.moveTo(cx, cy);
        for (double t = dt; t < maxAngle; t += dt) {
            double x = w * a * t * FastMath.cos(t) + cx;
            double y = h * a * t * FastMath.sin(t) + cy;
            shape.lineTo(x, y);
        }
        if (symmetry.isChecked()) {
            shape.moveTo(cx, cy);
            for (double t = dt; t < maxAngle; t += dt) {
                double x = -w * a * t * FastMath.cos(t) + cx;
                double y = -h * a * t * FastMath.sin(t) + cy;
                shape.lineTo(x, y);
            }
        }

        return shape;
    }

    private Path2D createRectangular(int width, int height) {
        return null;
    }

    @Override
    protected float getGradientRadius(float cx, float cy) {
        return Math.min(cx, cy);
    }


    //


    //


    class TypeParamListener implements ListDataListener {
        @Override
        public void intervalAdded(ListDataEvent e) {
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
        }

        @Override
        public void contentsChanged(ListDataEvent e) {

            // If it's circular, we don't need a parameter for sides
            if (typeParam.getValue() == 0)
                sidesParam.setEnabled(false, FilterSetting.EnabledReason.APP_LOGIC);

                // If it's polygonal, we need need a parameter to know it's sides
            else
                sidesParam.setEnabled(true, FilterSetting.EnabledReason.APP_LOGIC);

        }
    }
}
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

package pixelitor.utils;

import pixelitor.filters.gui.*;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import static pixelitor.utils.NonlinTransform.NONE;

/**
 * All the linear and nonlinear shape transform settings.
 */
public class Transform {
    private final EnumParam<NonlinTransform> distortType = NonlinTransform.asParam();
    private final RangeParam distortAmount = NonlinTransform.createAmountParam();
    protected final ImagePositionParam center = new ImagePositionParam("Center");
    private final GroupedRangeParam scale = new GroupedRangeParam("Scale (%)", 1, 100, 500);
    private final AngleParam rotate = new AngleParam("Rotate", 0);

    public Transform() {
        distortType.setupEnableOtherIf(distortAmount, NonlinTransform::hasAmount);
    }

    public DialogParam createDialogParam() {
        return new DialogParam("Transform", distortType, distortAmount, center, rotate, scale);
    }

    public Distortion createDistortion(int width, int height) {
        return new Distortion(distortType.getSelected(),
            getPivotPoint(width, height),
            distortAmount.getValueAsDouble(),
            width, height);
    }

    public AffineTransform calcAffineTransform(int width, int height) {
        double relX = center.getRelativeX();
        double relY = center.getRelativeY();
        double scaleX = scale.getPercentage(0);
        double scaleY = scale.getPercentage(1);

        boolean hasRotation = !rotate.hasDefault();
        boolean hasScaling = scaleX != 1.0 || scaleY != 1.0;

        if (!hasRotation && !hasScaling) {
            return null;
        }

        double cx = width * relX;
        double cy = height * relY;

        AffineTransform at = AffineTransform.getTranslateInstance(cx, cy);
        if (hasScaling) {
            at.scale(scaleX, scaleY);
        }
        if (hasRotation) {
            at.rotate(rotate.getValueInRadians());
        }
        at.translate(-cx, -cy);
        return at;
    }

    public double getCx(int width) {
        return width * center.getRelativeX();
    }

    public double getCy(int height) {
        return height * center.getRelativeY();
    }

    // ensures that shapes that are not drawn around a center
    // are still translated when moving the center selector
    public double getHorOffset(int width) {
        return width * (center.getRelativeX() - 0.5);
    }

    public double getVerOffset(int height) {
        return height * (center.getRelativeY() - 0.5);
    }

    private Point2D getPivotPoint(int width, int height) {
        return new Point2D.Double(getCx(width), getCy(height));
    }

    public boolean hasNonlinDistort() {
        return distortType.getSelected() != NONE;
    }
}

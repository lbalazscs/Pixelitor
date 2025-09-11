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

import java.awt.Shape;
import java.awt.geom.Point2D;

/**
 * The settings for a nonlinear shape distortion.
 */
public record Distortion(NonlinTransform nonlinTransform,
                         Point2D pivotPoint, double amount,
                         int width, int height) {
    public Shape distort(Shape input) {
        if (nonlinTransform == NonlinTransform.NONE) {
            return input;
        }
        return nonlinTransform.transform(input, pivotPoint, amount, width, height);
    }
}

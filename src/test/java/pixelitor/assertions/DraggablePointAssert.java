/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.assertions;

import org.assertj.core.api.AbstractAssert;
import pixelitor.tools.DraggablePoint;

/**
 * Custom AssertJ assertions for {@link DraggablePoint} objects.
 */
public class DraggablePointAssert extends AbstractAssert<DraggablePointAssert, DraggablePoint> {
    public DraggablePointAssert(DraggablePoint actual) {
        super(actual, DraggablePointAssert.class);
    }

    public DraggablePointAssert locIs(int x, int y) {
        isNotNull();

        if ((actual.x != x) || (actual.y != y)) {
            throw new AssertionError(String.format(
                    "found (%d, %d) instead of the expected (%d, %d)",
                    actual.x, actual.y, x, y));
        }

        return this;
    }

    public DraggablePointAssert imLocIs(double imX, double imY) {
        isNotNull();

        double dImX = Math.abs(actual.imX - imX);
        double dImY = Math.abs(actual.imY - imY);
        if (dImX > 2 || dImY > 2) {
            throw new AssertionError(String.format(
                    "found image coords (%.2f, %.2f) instead of the expected (%.2f, %.2f)",
                    actual.imX, actual.imY, imX, imY));
        }

        return this;
    }

}

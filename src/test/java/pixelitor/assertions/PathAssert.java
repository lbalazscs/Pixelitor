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
import pixelitor.tools.pen.Path;

/**
 * Custom AssertJ assertions for {@link Path} objects.
 */
public class PathAssert extends AbstractAssert<PathAssert, Path> {
    public PathAssert(Path actual) {
        super(actual, PathAssert.class);
    }

    public PathAssert isClosed() {
        isNotNull();

        if (!actual.isClosed()) {
            throw new AssertionError("not closed");
        }

        return this;
    }

    public PathAssert isNotClosed() {
        isNotNull();

        if (actual.isClosed()) {
            throw new AssertionError("closed");
        }

        return this;
    }

    public PathAssert firstIsActive() {
        isNotNull();

        if (!actual.getFirst().isActive()) {
            throw new AssertionError("first is not active");
        }

        return this;
    }

    public PathAssert firstIsNotActive() {
        isNotNull();

        if (actual.getFirst().isActive()) {
            throw new AssertionError("first is active");
        }

        return this;
    }

    public PathAssert numPointsIs(int expected) {
        isNotNull();

        int numPoints = actual.getNumPoints();
        if (numPoints != expected) {
            throw new AssertionError("numPoints is " + numPoints + ", expecting " + expected);
        }

        return this;
    }
}

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
import pixelitor.tools.pen.SubPath;

/**
 * Custom AssertJ assertions for {@link Path} objects.
 */
public class PathAssert extends AbstractAssert<PathAssert, Path> {
    public PathAssert(Path actual) {
        super(actual, PathAssert.class);
    }

    public PathAssert numSubPathsIs(int n) {
        isNotNull();

        int numSubpaths = actual.getNumSubpaths();
        if (numSubpaths != n) {
            throw new AssertionError(String.format(
                    "Expected %d subpaths, found %d", n, numSubpaths));
        }

        return this;
    }

    public PathAssert activeSubPathIs(SubPath sp) {
        isNotNull();

        SubPath activeSubpath = actual.getActiveSubpath();
        if (activeSubpath != sp) {
            throw new AssertionError(String.format(
                    "Expected %s, found %s", sp, activeSubpath));
        }

        return this;
    }

    public PathAssert isConsistent() {
        isNotNull();

        actual.checkConsistency();

        return this;
    }

    public PathAssert numAnchorsIs(int expected) {
        isNotNull();

        int numSubpaths = actual.getNumSubpaths();
        int numPoints = 0;
        for (int i = 0; i < numSubpaths; i++) {
            SubPath subPath = actual.getSubPath(i);
            numPoints += subPath.getNumAnchors();
        }
        if (numPoints != expected) {
            throw new AssertionError("Expected " + expected + ", found " + numPoints);
        }

        return this;
    }
}

/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.tools.pen.Path;
import pixelitor.tools.pen.PenTool;
import pixelitor.tools.pen.PenToolMode;

/**
 * Custom AssertJ assertions for {@link PenTool} objects.
 */
public class PenToolAssert extends ToolAssert<PenToolAssert, PenTool> {
    public PenToolAssert(PenTool actual) {
        super(actual, PenToolAssert.class);
    }

    public PenToolAssert hasPath() {
        isNotNull();

        if (PenTool.path == null) {
            throw new AssertionError("has no path");
        }

        return this;
    }

    public PenToolAssert hasNoPath() {
        isNotNull();

        if (PenTool.hasPath()) {
            throw new AssertionError("has path");
        }

        return this;
    }

    public PenToolAssert pathIs(Path path) {
        isNotNull();

        if (path == null) {
            throw new AssertionError("Null path, consider hasNoPath() instead");
        }
        if (PenTool.path != path) {
            throw new AssertionError("Expected " + path + ", found " + PenTool.path);
        }

        return this;
    }

    public PenToolAssert modeIs(PenToolMode expected) {
        isNotNull();

        if (actual.modeIsNot(expected)) {
            throw new AssertionError(
                "Expected " + expected + ", found " + actual.getMode());
        }

        return this;
    }

    public PenToolAssert pathActionAreEnabled() {
        isNotNull();

        if (!actual.arePathActionsEnabled()) {
            throw new AssertionError("not enabled");
        }

        return this;
    }

    public PenToolAssert pathActionAreNotEnabled() {
        isNotNull();

        if (actual.arePathActionsEnabled()) {
            throw new AssertionError("enabled");
        }

        return this;
    }

    public PenToolAssert isConsistent() {
        isNotNull();

        assert PenTool.checkPathConsistency();

        return this;
    }
}

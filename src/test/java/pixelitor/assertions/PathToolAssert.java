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

package pixelitor.assertions;

import pixelitor.tools.pen.PathActions;
import pixelitor.tools.pen.PathTool;

/**
 * Custom AssertJ assertions for {@link PathTool} objects.
 */
public class PathToolAssert extends ToolAssert<PathToolAssert, PathTool> {
    public PathToolAssert(PathTool actual) {
        super(actual, PathToolAssert.class);
    }

    public PathToolAssert pathActionAreEnabled() {
        isNotNull();

        if (!PathActions.isEnabled()) {
            throw new AssertionError("not enabled");
        }

        return this;
    }

    public PathToolAssert pathActionAreNotEnabled() {
        isNotNull();

        if (PathActions.isEnabled()) {
            throw new AssertionError("enabled");
        }

        return this;
    }
}

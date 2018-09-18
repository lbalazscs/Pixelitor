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

package pixelitor;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import pixelitor.tools.ToolTest;
import pixelitor.tools.pen.PathBuilderTest;
import pixelitor.tools.pen.PathTest;
import pixelitor.tools.pen.PenToolTest;

/**
 * All the tests that are related to paths
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        PathBuilderTest.class,
        PathTest.class,
        PenToolTest.class,
        ToolTest.class,
})
public class PathTestsSuite {
    // empty
}

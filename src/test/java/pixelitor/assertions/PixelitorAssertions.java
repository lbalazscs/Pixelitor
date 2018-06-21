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

import org.assertj.core.api.Assertions;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.filters.RandomFilterSource;
import pixelitor.selection.Selection;
import pixelitor.tools.DraggablePoint;
import pixelitor.tools.pen.Path;

@SuppressWarnings("ExtendsUtilityClass")
public class PixelitorAssertions extends Assertions {
    @org.assertj.core.util.CheckReturnValue
    public static CompositionAssert assertThat(Composition actual) {
        return new CompositionAssert(actual);
    }

    @org.assertj.core.util.CheckReturnValue
    public static CanvasAssert assertThat(Canvas actual) {
        return new CanvasAssert(actual);
    }

    @org.assertj.core.util.CheckReturnValue
    public static SelectionAssert assertThat(Selection actual) {
        return new SelectionAssert(actual);
    }

    @org.assertj.core.util.CheckReturnValue
    public static RandomFilterSourceAssert assertThat(RandomFilterSource actual) {
        return new RandomFilterSourceAssert(actual);
    }

    @org.assertj.core.util.CheckReturnValue
    public static DraggablePointAssert assertThat(DraggablePoint actual) {
        return new DraggablePointAssert(actual);
    }

    @org.assertj.core.util.CheckReturnValue
    public static PathAssert assertThat(Path actual) {
        return new PathAssert(actual);
    }
}

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

package pixelitor.filters.transitions;

import com.bric.image.transition.BlindsTransition2D;
import com.bric.image.transition.CheckerboardTransition2D;
import com.bric.image.transition.Transition;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;

public class CheckerboardTransition extends AbstractTransition {
    public static final String NAME = "Checkerboard Transition";

    private final IntChoiceParam type = new IntChoiceParam("Type", new Item[]{
        new Item("Left", BlindsTransition2D.LEFT),
        new Item("Right", BlindsTransition2D.RIGHT),
        new Item("Up", BlindsTransition2D.UP),
        new Item("Down", BlindsTransition2D.DOWN),
    });
    private final GroupedRangeParam numDiv = new GroupedRangeParam(
        "Number of Divisions", "Rows", "Columns",
        2, 8, 100, true);

    public CheckerboardTransition() {
        addParamsToFront(type, numDiv, invert);
    }

    @Override
    Transition createTransition() {
        int numRows = numDiv.getValue(0);
        int numCols = numDiv.getValue(1);
        return new CheckerboardTransition2D(type.getValue(), numRows, numCols);
    }
}

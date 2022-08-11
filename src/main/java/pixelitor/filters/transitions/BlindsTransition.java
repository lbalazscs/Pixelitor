/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import com.bric.image.transition.Transition;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;

public class BlindsTransition extends AbstractTransition {
    public static final String NAME = "Blinds Transition";

    private final IntChoiceParam type = new IntChoiceParam("Type", new Item[]{
        new Item("Left", BlindsTransition2D.LEFT),
        new Item("Right", BlindsTransition2D.RIGHT),
        new Item("Up", BlindsTransition2D.UP),
        new Item("Down", BlindsTransition2D.DOWN),
    });
    private final RangeParam numBlinds = new RangeParam("Number of Blinds", 1, 10, 50);

    public BlindsTransition() {
        addParamsToFront(type, numBlinds);
    }

    @Override
    Transition createTransition() {
        return new BlindsTransition2D(type.getValue(), numBlinds.getValue());
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}

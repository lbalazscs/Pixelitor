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

import com.bric.image.transition.DiamondsTransition2D;
import com.bric.image.transition.Transition;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;

public class ShapesGridTransition extends AbstractTransition {
    public static final String NAME = "Shapes Grid Transition";

    private final IntChoiceParam type = new IntChoiceParam("Type", new Item[]{
        new Item("Diamonds", DiamondsTransition2D.TYPE_DIAMOND),
        new Item("Circles", DiamondsTransition2D.TYPE_CIRCLE),
        new Item("Squares", DiamondsTransition2D.TYPE_SQUARE),
        new Item("Triangles", DiamondsTransition2D.TYPE_TRIANGLE),
    });
    private final RangeParam size = new RangeParam("Grid Size", 10, 50, 500);
    private final AngleParam angle = new AngleParam("Angle", 0);

    public ShapesGridTransition() {
        addParamsToFront(type, size, angle);
    }

    @Override
    Transition createTransition() {
        return new DiamondsTransition2D(size.getValue(), type.getValue(), angle.getValueInRadians());
    }
}

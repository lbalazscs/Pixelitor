/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import com.bric.image.transition.GooTransition2D;
import com.bric.image.transition.Transition;
import pixelitor.filters.gui.RangeParam;

public class GooTransition extends AbstractTransition {
    public static final String NAME = "Goo Transition";

    private final RangeParam columns = new RangeParam("Columns", 2, 20, 50);
    private final RangeParam strokeWidth = new RangeParam("Stroke Width", 0, 1, 10);

    private GooTransition2D transition;
    private int lastNumColumns = -1;
    private long seed;
    private long lastSeed;

    public GooTransition() {
        addParamsToFront(columns, strokeWidth);
        paramSet.withAction(paramSet.createReseedAction(this::reseed));
        reseed(paramSet.getLastSeed());
        lastSeed = seed;
    }

    @Override
    Transition createTransition() {
        int numColumns = columns.getValue();
        if (transition == null || numColumns != lastNumColumns || seed != lastSeed) {
            transition = new GooTransition2D(numColumns, seed);
        }
        transition.setStrokeWidth(strokeWidth.getValue());
        lastNumColumns = numColumns;
        lastSeed = seed;
        return transition;
    }

    private void reseed(long newSeed) {
        seed = newSeed;
    }
}

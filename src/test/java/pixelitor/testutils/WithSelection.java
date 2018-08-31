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

package pixelitor.testutils;

import pixelitor.Composition;
import pixelitor.TestHelper;

import java.awt.Rectangle;

/**
 * Whether there is a selection present when a test runs
 */
public enum WithSelection {
    YES {
        @Override
        public void setupFor(Composition comp) {
            TestHelper.setStandardTestSelection(comp);
            Rectangle selectionShape = TestHelper.getStandardTestSelectionShape();
            TestHelper.checkSelectionBounds(comp, selectionShape);
        }
    }, NO {
        @Override
        public void setupFor(Composition comp) {
            // do nothing
        }
    };

    public abstract void setupFor(Composition comp);

    public boolean isYes() {
        return this == YES;
    }
}

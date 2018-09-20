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

import pixelitor.selection.SelectionInteraction;
import pixelitor.selection.SelectionType;
import pixelitor.tools.SelectionTool;

/**
 * Custom AssertJ assertions for {@link SelectionTool} objects.
 */
public class SelectionToolAssert extends ToolAssert<SelectionToolAssert, SelectionTool> {
    public SelectionToolAssert(SelectionTool actual) {
        super(actual, SelectionToolAssert.class);
    }

    public SelectionToolAssert selectionTypeIs(SelectionType expected) {
        isNotNull();

        SelectionType selectionType = actual.getSelectionType();
        if (selectionType != expected) {
            throw new AssertionError("Expected " + expected + ", found " + selectionType);
        }

        return this;
    }

    public SelectionToolAssert interactionIs(SelectionInteraction expected) {
        isNotNull();

        SelectionInteraction interaction = actual.getCurrentInteraction();
        if (interaction != expected) {
            throw new AssertionError("Expected " + expected + ", found " + interaction);
        }

        return this;
    }
}

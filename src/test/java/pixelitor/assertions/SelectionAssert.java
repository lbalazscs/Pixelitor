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

import org.assertj.core.api.AbstractObjectAssert;
import pixelitor.selection.Selection;

import java.awt.geom.Rectangle2D;
import java.util.Objects;

/**
 * Custom AssertJ assertions for {@link Selection} objects.
 */
public class SelectionAssert extends AbstractObjectAssert<SelectionAssert, Selection> {
    public SelectionAssert(Selection actual) {
        super(actual, SelectionAssert.class);
    }

    public SelectionAssert isAlive() {
        isNotNull();

        if (!actual.isAlive()) {
            failWithMessage("Not alive");
        }

        return this;
    }

    public SelectionAssert isNotAlive() {
        isNotNull();

        if (actual.isAlive()) {
            failWithMessage("Alive");
        }

        return this;
    }

    public SelectionAssert isFrozen() {
        isNotNull();

        if (!actual.isFrozen()) {
            failWithMessage("Not frozen");
        }

        return this;
    }

    public SelectionAssert isNotFrozen() {
        isNotNull();

        if (actual.isFrozen()) {
            failWithMessage("Frozen");
        }

        return this;
    }

    public SelectionAssert isHidden() {
        isNotNull();

        if (!actual.isHidden()) {
            failWithMessage("Not hidden");
        }

        return this;
    }

    public SelectionAssert isNotHidden() {
        isNotNull();

        if (actual.isHidden()) {
            failWithMessage("Hidden");
        }

        return this;
    }

    public SelectionAssert isMarching() {
        isNotNull();

        if (!actual.isMarching()) {
            failWithMessage("Not marching");
        }

        return this;
    }

    public SelectionAssert isNotMarching() {
        isNotNull();

        if (actual.isMarching()) {
            failWithMessage("Marching");
        }

        return this;
    }

    public SelectionAssert hasShapeBounds(Rectangle2D shapeBounds) {
        isNotNull();

        Rectangle2D actualShapeBounds = actual.getShapeBounds2D();
        if (!Objects.equals(actualShapeBounds, shapeBounds)) {
            failWithMessage("""
                    
                Expecting shapeBounds of:
                  <%s>
                to be:
                  <%s>
                but was:
                  <%s>""", actual, shapeBounds, actualShapeBounds);
        }

        return this;
    }
}

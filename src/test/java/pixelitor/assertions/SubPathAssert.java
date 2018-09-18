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

import org.assertj.core.api.AbstractAssert;
import pixelitor.tools.pen.AnchorPoint;
import pixelitor.tools.pen.SubPath;
import pixelitor.tools.util.DraggablePoint;

import static pixelitor.assertions.PixelitorAssertions.assertThat;

/**
 * Custom AssertJ assertions for {@link SubPath} objects.
 */
public class SubPathAssert extends AbstractAssert<SubPathAssert, SubPath> {
    public SubPathAssert(SubPath actual) {
        super(actual, SubPathAssert.class);
    }

    public SubPathAssert isClosed() {
        isNotNull();

        if (!actual.isClosed()) {
            throw new AssertionError("not closed");
        }

        return this;
    }

    public SubPathAssert isNotClosed() {
        isNotNull();

        if (actual.isClosed()) {
            throw new AssertionError("closed");
        }

        return this;
    }

    public SubPathAssert isFinished() {
        isNotNull();

        if (!actual.isFinished()) {
            throw new AssertionError("not finished");
        }

        return this;
    }

    public SubPathAssert isNotFinished() {
        isNotNull();

        if (actual.isFinished()) {
            throw new AssertionError("finished");
        }

        return this;
    }

    public SubPathAssert firstIsActive() {
        isNotNull();

        if (!actual.getFirst().isActive()) {
            throw new AssertionError("first is not active");
        }

        return this;
    }

    public SubPathAssert firstIsNotActive() {
        isNotNull();

        if (actual.getFirst().isActive()) {
            throw new AssertionError("first is active");
        }

        return this;
    }

    public SubPathAssert numAnchorsIs(int expected) {
        isNotNull();

        int numPoints = actual.getNumAnchors();
        if (numPoints != expected) {
            throw new AssertionError("numPoints is " + numPoints + ", expecting " + expected);
        }

        return this;
    }

    public SubPathAssert isConsistent() {
        isNotNull();

        actual.checkConsistency();

        return this;
    }

    public SubPathAssert firstAnchorIsAt(double x, double y) {
        isNotNull();

        AnchorPoint anchor = actual.getAnchor(0);
        assertThat(anchor).isAt(x, y);

        return this;
    }

    public SubPathAssert hasMoving() {
        isNotNull();

        if (!actual.hasMovingPoint()) {
            throw new AssertionError("has no moving");
        }

        return this;
    }

    public SubPathAssert hasNoMoving() {
        isNotNull();

        if (actual.hasMovingPoint()) {
            throw new AssertionError("has moving");
        }

        return this;
    }

    public SubPathAssert movingIsAt(double x, double y) {
        isNotNull();

        DraggablePoint moving = actual.getMoving();
        assertThat(moving).isAt(x, y);

        return this;
    }
}

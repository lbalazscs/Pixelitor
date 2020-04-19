/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.layers.ContentLayer;

/**
 * Whether there is a translation present when a test runs
 */
public enum WithTranslation {
    NO(0, 0) {
        @Override
        public void setupFor(Composition comp) {
            // do nothing
        }

        @Override
        public void moveLayer(Composition comp) {
            // do nothing
        }

        @Override
        public void setupFor(ContentLayer layer) {
            // do nothing
        }
    }, YES(-4, -4) {
        @Override
        public void setupFor(Composition comp) {
            comp.forEachContentLayer(contentLayer ->
                    TestHelper.setTranslation(comp, contentLayer, this));
        }

        @Override
        public void moveLayer(Composition comp) {
            TestHelper.moveLayer(comp, false, 2, 2);
            TestHelper.moveLayer(comp, false, -4, -4);
        }

        @Override
        public void setupFor(ContentLayer layer) {
            layer.startMovement();
            layer.moveWhileDragging(-2, -2);
            layer.endMovement();
        }
    };

    private final int expectedTX;
    private final int expectedTY;

    WithTranslation(int expectedTX, int expectedTY) {
        this.expectedTX = expectedTX;
        this.expectedTY = expectedTY;
    }

    public abstract void setupFor(Composition comp);

    public abstract void setupFor(ContentLayer layer);

    public abstract void moveLayer(Composition comp);

    public int getExpectedTX() {
        return expectedTX;
    }

    public int getExpectedTY() {
        return expectedTY;
    }

    public boolean isTrue() {
        return expectedTX != 0 || expectedTY != 0;
    }
}

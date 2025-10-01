/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import java.awt.Point;

/**
 * Whether and how translatios should be applied to content layers during tests.
 */
public enum WithTranslation {
    NO(0, 0) {
        @Override
        public void configure(Composition comp) {
            // do nothing
        }

        @Override
        public void move(Composition comp) {
            // do nothing
        }

        @Override
        public void configure(ContentLayer layer) {
            // do nothing
        }
    }, YES(-4, -4) {
        @Override
        public void configure(Composition comp) {
            comp.forEachNestedLayerOfType(ContentLayer.class, contentLayer ->
                TestHelper.setTranslation(comp, contentLayer, this));
        }

        @Override
        public void move(Composition comp) {
            TestHelper.move(comp, 2, 2, false);
            TestHelper.move(comp, -4, -4, false);
        }

        @Override
        public void configure(ContentLayer layer) {
            layer.prepareMovement();
            layer.moveWhileDragging(-2, -2);
            layer.finalizeMovement();
        }
    };

    private final int expectedTX;
    private final int expectedTY;

    WithTranslation(int expectedTX, int expectedTY) {
        this.expectedTX = expectedTX;
        this.expectedTY = expectedTY;
    }

    public abstract void configure(Composition comp);

    public abstract void configure(ContentLayer layer);

    public abstract void move(Composition comp);

    public int getExpectedTX() {
        return expectedTX;
    }

    public int getExpectedTY() {
        return expectedTY;
    }

    public Point getExpectedValue() {
        return new Point(expectedTX, expectedTY);
    }

    public boolean isTrue() {
        return expectedTX != 0 || expectedTY != 0;
    }
}

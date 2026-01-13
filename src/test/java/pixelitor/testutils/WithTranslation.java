/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

    private final int expectedTx;
    private final int expectedTy;

    WithTranslation(int expectedTx, int expectedTy) {
        this.expectedTx = expectedTx;
        this.expectedTy = expectedTy;
    }

    public abstract void configure(Composition comp);

    public abstract void configure(ContentLayer layer);

    public abstract void move(Composition comp);

    public int getExpectedTx() {
        return expectedTx;
    }

    public int getExpectedTy() {
        return expectedTy;
    }

    public Point getExpectedValue() {
        return new Point(expectedTx, expectedTy);
    }

    public boolean isTrue() {
        return expectedTx != 0 || expectedTy != 0;
    }
}

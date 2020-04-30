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

/**
 * The number of layers which is present when a test runs
 */
public enum NumLayers {
    ONE() {
        @Override
        public void setupFor(Composition comp) {
            // delete one layer so that we have one left
            comp.deleteLayer(comp.getActiveLayer(), true);
        }
    }, MORE() {
        @Override
        public void setupFor(Composition comp) {

        }
    };

    public abstract void setupFor(Composition comp);
}

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

/**
 * The number of layers required for different test scenarios in a composition.
 */
public enum LayerCount {
    ONE() {
        @Override
        public void configure(Composition comp) {
            // delete a layer to ensure only one layer remains
            comp.deleteLayer(comp.getActiveLayer(), true);
        }
    }, TWO() {
        @Override
        public void configure(Composition comp) {
            // default composition starts with two layers, so no action needed
        }
    };

    /**
     * Configures the composition to have the required number of layers.
     */
    public abstract void configure(Composition comp);
}

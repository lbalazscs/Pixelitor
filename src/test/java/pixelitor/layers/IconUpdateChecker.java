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

package pixelitor.layers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Utility class for verifying the number of updates to layer and mask icons.
 */
public class IconUpdateChecker {
    private final TestLayerUI ui;
    private final LayerMask mask;

    private final int initialLayerIconUpdates;
    private final int initialMaskIconUpdates;

    public IconUpdateChecker(Layer layer) {
        if (!(layer.getUI() instanceof TestLayerUI layerUI)) {
            throw new IllegalArgumentException();
        }
        this.ui = layerUI;
        this.mask = layer.getMask();

        initialLayerIconUpdates = ui.getLayerIconUpdateCount();
        initialMaskIconUpdates = mask == null ? 0 : ui.getMaskIconUpdateCount();

        // sanity check
        verifyUpdateCounts(0, 0);
    }

    public void verifyUpdateCounts(int expectedLayerUpdates, int expectedMaskUpdates) {
        verifyLayerUpdates(expectedLayerUpdates);
        verifyMaskUpdates(expectedMaskUpdates);
    }

    private void verifyLayerUpdates(int expectedUpdates) {
        assertThat(ui.getLayerIconUpdateCount())
            .as("layer icon updates")
            .isEqualTo(initialLayerIconUpdates + expectedUpdates);
    }

    private void verifyMaskUpdates(int expectedUpdates) {
        if (mask == null) {
            // this was a test without a mask
            return;
        }
        assertThat(ui.getMaskIconUpdateCount())
            .as("mask icon updates")
            .isEqualTo(initialMaskIconUpdates + expectedUpdates);
    }
}

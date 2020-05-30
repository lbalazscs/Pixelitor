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

package pixelitor.guitest;

import org.assertj.swing.core.Robot;
import pixelitor.layers.LayersContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Helper class to test the {@link LayersContainer} instance in
 * AssertJ Swing tests
 */
public class LayersContainerFixture {
    private final Robot robot;
    private final LayersContainer layersContainer;

    public LayersContainerFixture(Robot robot) {
        this.robot = robot;
        layersContainer = LayersContainer.get();
    }

    public LayersContainerFixture requireNumLayerButtons(int expected) {
        robot.waitForIdle();

        int numLayers = EDT.call(layersContainer::getNumLayerButtons);
        if (expected != numLayers) {
            throw new AssertionError("Expected " + expected + ", found " + numLayers);
        }

        return this;
    }

    public LayersContainerFixture requireLayerNames(String... expected) {
        robot.waitForIdle();

        List<String> actualNames = EDT.call(layersContainer::getLayerNames);
        assertThat(actualNames).containsExactly(expected);

        return this;
    }
}

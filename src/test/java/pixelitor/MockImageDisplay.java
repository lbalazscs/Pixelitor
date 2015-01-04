/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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

package pixelitor;

import pixelitor.layers.Layer;
import pixelitor.layers.LayerButton;

public class MockImageDisplay implements ImageDisplay {
    @Override
    public double getViewScale() {
        return 1.0;
    }

    @Override
    public void deleteLayerButton(LayerButton button) {
    }

    @Override
    public Composition getComp() {
        return null;
    }

    @Override
    public void changeLayerOrderInTheGUI(int oldIndex, int newIndex) {
    }

    @Override
    public void updateRegion(int startX, int startY, int endX, int endY, int thickness) {
    }

    @Override
    public void canvasSizeChanged() {
    }

    @Override
    public void repaint() {
    }

    @Override
    public void updateTitle() {
    }

    @Override
    public void addLayerToGUI(Layer newLayer, int newLayerIndex) {
    }
}

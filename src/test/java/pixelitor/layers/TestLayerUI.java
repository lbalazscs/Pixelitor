/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.RunContext;

/**
 * The {@link LayerUI} implementation used in unit tests
 */
public class TestLayerUI implements LayerUI {
    private Layer layer;
    private String name;
    private boolean showVisibility = true;
    private boolean hasMaskIconLabel = false;

    private int numLayerIconUpdates;
    private int numMaskIconUpdates;

    public TestLayerUI(Layer layer) {
        this.layer = layer;
        assert RunContext.isUnitTesting();
    }

    @Override
    public void setLayerName(String newName) {
        name = newName;
    }

    @Override
    public boolean hasMaskIcon() {
        return hasMaskIconLabel;
    }

    @Override
    public String getLayerName() {
        assert layer.getName().equals(name);
        return name;
    }

    @Override
    public void setOpenEye(boolean newVisibility) {
        showVisibility = newVisibility;
    }

    @Override
    public boolean isEyeOpen() {
        assert layer.isVisible() == showVisibility;
        return showVisibility;
    }

    @Override
    public void addMaskIcon() {
        hasMaskIconLabel = true;
    }

    @Override
    public void removeMaskIcon() {
        if (!hasMaskIconLabel) {
            throw new IllegalStateException();
        }
        hasMaskIconLabel = false;
    }

    @Override
    public void updateLayerIconImageAsync(ImageLayer imageLayer) {
        if (imageLayer instanceof LayerMask) {
            numMaskIconUpdates++;
        } else {
            numLayerIconUpdates++;
        }
    }

    public int getNumLayerIconUpdates() {
        return numLayerIconUpdates;
    }

    public int getNumMaskIconUpdates() {
        return numMaskIconUpdates;
    }

    @Override
    public void updateSelectionState() {

    }

    @Override
    public void setSelected(boolean b) {

    }

    @Override
    public void changeLayer(Layer newLayer) {
        this.layer = newLayer;
    }
}

/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Build;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The {@link LayerUI} implementation used in unit tests
 */
public class TestLayerUI implements LayerUI {
    private String name;
    private boolean showVisibility = true;
    private boolean hasMaskIconLabel = false;
    private final Map<ImageLayer, Integer> iconImageUpdateCounter = new IdentityHashMap<>();

    public TestLayerUI() {
        assert Build.isUnitTesting();
    }

    @Override
    public void setLayerName(String newName) {
        this.name = newName;
    }

    @Override
    public String getLayerName() {
        return name;
    }

    @Override
    public void setOpenEye(boolean newVisibility) {
        this.showVisibility = newVisibility;
    }

    @Override
    public boolean isVisibilityChecked() {
        return showVisibility;
    }

    @Override
    public void addMaskIconLabel() {
        hasMaskIconLabel = true;
    }

    @Override
    public void deleteMaskIconLabel() {
        if (!hasMaskIconLabel) {
            throw new IllegalStateException();
        }
        hasMaskIconLabel = false;
    }

    @Override
    public void updateLayerIconImage(ImageLayer imageLayer) {
        iconImageUpdateCounter.merge(imageLayer, 1, Integer::sum);
    }

    public int getNumIconImageUpdates(ImageLayer imageLayer) {
        return iconImageUpdateCounter.getOrDefault(imageLayer, 0);
    }

    @Override
    public void configureBorders(boolean b) {

    }

    @Override
    public void setSelected(boolean b) {

    }
}

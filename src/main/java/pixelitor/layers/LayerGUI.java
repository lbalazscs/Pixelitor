/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

public class LayerGUI {
    private final LayerButton layerButton;

    public LayerGUI(Layer layer) {
        this.layerButton = new LayerButton(layer);
    }

    public void setOpenEye(boolean newVisibility) {
        layerButton.setOpenEye(newVisibility);
    }

    public Layer getLayer() {
        return layerButton.getLayer();
    }

    public String getLayerName() {
        return layerButton.getLayerName();
    }

    public boolean isVisibilityChecked() {
        return layerButton.isVisibilityChecked();
    }

    public void changeNameProgrammatically(String newName) {
        layerButton.changeNameProgrammatically(newName);
    }

    public void updateLayerIconImage(ImageLayer layer) {
        // the argument can refer to either an image layer or a mask
        layerButton.updateLayerIconImage(layer);
    }

    public void addMaskIconLabel() {
        layerButton.addMaskIconLabel();
    }

    public void deleteMaskIconLabel() {
        layerButton.deleteMaskIconLabel();
    }

    public void setSelected(boolean b) {
        layerButton.setSelected(b);
    }

    public LayerButton getLayerButton() {
        return layerButton;
    }

    public void setOpacityFromModel(float newOpacity) {
        LayerBlendingModePanel.INSTANCE.setOpacityFromModel(newOpacity);
    }

    public void setMaskEditing(boolean b) {
        layerButton.configureBorders(b);
    }
}

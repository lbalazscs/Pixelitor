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

public interface LayerUI {
    void setOpenEye(boolean newVisibility);

    Layer getLayer();

    String getLayerName();

    boolean isVisibilityChecked();

    void changeNameProgrammatically(String newName);

    // the argument can refer to either an image layer or a mask
    void updateLayerIconImage(ImageLayer layer);

    void addMaskIconLabel();

    void deleteMaskIconLabel();

    void setSelected(boolean b);

    LayerButton getLayerButton();

    void setOpacityFromModel(float newOpacity);

    void setMaskEditing(boolean b);
}

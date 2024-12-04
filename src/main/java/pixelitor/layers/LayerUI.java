/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.utils.debug.Debuggable;

/**
 * The user interface of a {@link Layer}
 */
public interface LayerUI extends Debuggable {
    void updateName();

    boolean hasMaskIcon();

    String getLayerName();

    Layer getLayer();

    /**
     * Sets the layer visibility at the UI level.
     */
    void setOpenEye(boolean newVisibility);

    boolean isEyeOpen();

    void addMaskIcon();

    void removeMaskIcon();

    void updateLayerIconImageAsync(Layer layer);

    /**
     * Sets the border around the icon according to the selection state
     */
    void updateSelectionState();

    void updateChildrenPanel();

    void setSelected(boolean b);

    void changeLayer(Layer newLayer);

    int getId();

    void repaint();

    void setParentUI(LayerUI parentUI);

    void detach();

    boolean checkInvariants();
}

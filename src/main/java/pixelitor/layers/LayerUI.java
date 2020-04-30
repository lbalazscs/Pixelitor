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

package pixelitor.layers;

/**
 * The user interface of a {@link Layer}
 */
public interface LayerUI {
    void setLayerName(String newName);

    boolean hasMaskIcon();

    String getLayerName();

    void setOpenEye(boolean newVisibility);

    boolean isVisibilityChecked();

    void addMaskIcon();

    void removeMaskIcon();

    void updateLayerIconImageAsync(ImageLayer imageLayer);

    void updateBorders();

    void setSelected(boolean b);
}

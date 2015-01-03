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
import pixelitor.menus.view.ZoomLevel;

import java.awt.image.BufferedImage;

public interface ImageDisplay {
    void addBaseLayer(BufferedImage baseLayerImage);

    void setInternalFrame(InternalImageFrame internalFrame);

    InternalImageFrame getInternalFrame();

    void close();

    double getViewScale();

    ZoomLevel getZoomLevel();

    void addLayerButton(LayerButton layerButton, int newLayerIndex);

    void deleteLayerButton(LayerButton button);

    Composition getComp();

    void changeLayerOrderInTheGUI(int oldIndex, int newIndex);

    void updateRegion(int startX, int startY, int endX, int endY, int thickness);

    void setLayerMaskEditing(boolean layerMaskEditing);

    void canvasSizeChanged();

    boolean setZoom(ZoomLevel newZoomLevel, boolean settingTheInitialSize);

    void increaseZoom(int mouseX, int mouseY);

    void decreaseZoom(int mouseX, int mouseY);

    void repaint();

    void updateTitle();

    void addLayerToGUI(Layer newLayer, int newLayerIndex);
}

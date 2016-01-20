/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * Separates the GUI code from the non-GUI code.
 * Contains the ImageComponent methods that are visible from non-GUI code.
 */
public interface ImageDisplay {
    double getViewScale();

    ZoomLevel getZoomLevel();

    void deleteLayerButton(LayerButton button);

    Composition getComp();

    void changeLayerOrderInTheGUI(int oldIndex, int newIndex);

    void updateRegion(double startX, double startY, double endX, double endY, int thickness);

    void canvasSizeChanged();

    void repaint();

    void updateTitle();

    void addLayerToGUI(Layer newLayer, int newLayerIndex);

    // the following methods are needed by the tools

    double componentXToImageSpace(int mouseX);

    double componentYToImageSpace(int mouseY);

    Rectangle fromImageToComponentSpace(Rectangle2D input);

    Rectangle2D fromComponentToImageSpace(Rectangle input);

    Cursor getCursor();

    void setCursor(Cursor cursor);

    // must return a JViewport, because it will be casted
    Container getParent();

    Rectangle getViewRect();

    void increaseZoom(int mouseX, int mouseY);

    void decreaseZoom(int mouseX, int mouseY);

    boolean isMaskShowing();

    boolean activeIsImageLayer();

    void setShowLayerMask(boolean b);

    boolean setZoom(ZoomLevel zoomLevel, boolean settingTheInitialSize);

    void close();
}

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

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Rectangle;

/**
 * Separates the GUI code from the non-GUI code.
 * Contains the ImageComponent methods that are visible from non-GUI code.
 */
public interface ImageDisplay {
    double getViewScale();

    void deleteLayerButton(LayerButton button);

    Composition getComp();

    void changeLayerOrderInTheGUI(int oldIndex, int newIndex);

    void updateRegion(int startX, int startY, int endX, int endY, int thickness);

    void canvasSizeChanged();

    void repaint();

    void updateTitle();

    void addLayerToGUI(Layer newLayer, int newLayerIndex);

    // the following methods are needed by the tools

    int componentXToImageSpace(int mouseX);

    int componentYToImageSpace(int mouseY);

    Rectangle fromImageToComponentSpace(Rectangle input);

    Rectangle fromComponentToImageSpace(Rectangle input);

    Cursor getCursor();

    void setCursor(Cursor cursor);

    // must return a JViewport, because it will be casted
    Container getParent();

    Rectangle getViewRectangle();

    void increaseZoom(int mouseX, int mouseY);

    void decreaseZoom(int mouseX, int mouseY);
}

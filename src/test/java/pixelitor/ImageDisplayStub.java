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

import javax.swing.*;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Rectangle;

public class ImageDisplayStub implements ImageDisplay {
    private Cursor cursor = Cursor.getDefaultCursor();
    private final JViewport parent = new JViewport();
    private Composition comp;

    @Override
    public double getViewScale() {
        return 1.0;
    }

    @Override
    public void deleteLayerButton(LayerButton button) {
    }

    @Override
    public Composition getComp() {
        return comp;
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

    @Override
    public int componentXToImageSpace(int mouseX) {
        return mouseX;
    }

    @Override
    public int componentYToImageSpace(int mouseY) {
        return mouseY;
    }

    @Override
    public Rectangle fromImageToComponentSpace(Rectangle input) {
        return input;
    }

    @Override
    public Rectangle fromComponentToImageSpace(Rectangle input) {
        return input;
    }

    @Override
    public Cursor getCursor() {
        return cursor;
    }

    @Override
    public void setCursor(Cursor cursor) {
        this.cursor = cursor;
    }

    @Override
    public Container getParent() {
        return parent;
    }

    @Override
    public Rectangle getViewRectangle() {
        return comp.getCanvasBounds();
    }

    @Override
    public void increaseZoom(int mouseX, int mouseY) {
    }

    @Override
    public void decreaseZoom(int mouseX, int mouseY) {
    }

    public void setComp(Composition comp) {
        this.comp = comp;
    }
}

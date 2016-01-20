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

package pixelitor.history;

import pixelitor.Canvas;
import pixelitor.Composition;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A PixelitorEdit that changes the canvas size,
 * such as resize, crop, enlarge layer, or rotation.
 * Always part of a MultiLayerEdit.
 */
public class CanvasChangeEdit extends PixelitorEdit {
    private int backupCanvasWidth;
    private int backupCanvasHeight;

    /**
     * This constructor must be called before the change.
     */
    public CanvasChangeEdit(Composition comp, String name) {
        super(comp, name);
        embedded = true;

        backupCanvasWidth = comp.getCanvasWidth();
        backupCanvasHeight = comp.getCanvasHeight();
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        swapCanvasDimensions();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        swapCanvasDimensions();
    }

    private void swapCanvasDimensions() {
        Canvas canvas = comp.getCanvas();

        int tmpCanvasWidth = canvas.getWidth();
        int tmpCanvasHeight = canvas.getHeight();

        canvas.updateSize(backupCanvasWidth, backupCanvasHeight);

        backupCanvasWidth = tmpCanvasWidth;
        backupCanvasHeight = tmpCanvasHeight;

        if (!embedded) {
            comp.updateAllIconImages();
            History.notifyMenus(this);
        }
    }
}
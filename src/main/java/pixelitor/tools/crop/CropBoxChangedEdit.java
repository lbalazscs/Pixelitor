/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.crop;

import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.history.PixelitorEdit;
import pixelitor.tools.Tools;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.geom.Rectangle2D;

/**
 * Represents a change to the {@link CropBox} in the {@link CropTool}.
 */
public class CropBoxChangedEdit extends PixelitorEdit {
    // the crop box rectangle in image space before and after the change
    private final Rectangle2D rectBefore; // null if box creation
    private final Rectangle2D rectAfter;  // null if box dismissal

    private final boolean allowGrowingBefore;
    private final boolean allowGrowingAfter;

    public CropBoxChangedEdit(String name, Composition comp,
                              Rectangle2D rectBefore, Rectangle2D rectAfter,
                              boolean allowGrowingBefore, boolean allowGrowingAfter) {
        super(name, comp);
        this.rectBefore = rectBefore;
        this.rectAfter = rectAfter;
        this.allowGrowingBefore = allowGrowingBefore;
        this.allowGrowingAfter = allowGrowingAfter;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        restoreState(rectBefore, allowGrowingBefore);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        restoreState(rectAfter, allowGrowingAfter);
    }

    private void restoreState(Rectangle2D rectToRestore, boolean allowGrowingSetting) {
        View view = comp.getView();

        Tools.CROP.setAllowGrowingUndoRedo(allowGrowingSetting);

        if (rectToRestore == null) {
            Tools.CROP.clearBoxForUndoRedo();
        } else {
            Tools.CROP.setBoxForUndoRedo(rectToRestore, view);
        }

        Tools.CROP.updateUIFromState(view);
        view.repaint();
    }
}
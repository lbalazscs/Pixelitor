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

package pixelitor.tools.shapes.history;

import pixelitor.Composition;
import pixelitor.history.PixelitorEdit;
import pixelitor.layers.ShapesLayer;
import pixelitor.tools.Tools;
import pixelitor.tools.shapes.StyledShape;
import pixelitor.tools.transform.TransformBox;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * The creation of a {@link StyledShape} and its {@link TransformBox}.
 * The shape is not finalized yet.
 */
public class CreateBoxedShapeEdit extends PixelitorEdit {
    private final StyledShape shape;
    private final TransformBox box;
    private final ShapesLayer targetLayer;

    public CreateBoxedShapeEdit(Composition comp,
                                StyledShape shape,
                                TransformBox box,
                                ShapesLayer targetLayer) {
        super("Create Shape", comp);
        this.shape = shape.clone();
        this.box = box;
        this.targetLayer = targetLayer;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        // if the shape was on a ShapesLayer, remove it from the layer
        if (targetLayer != null) {
            targetLayer.setStyledShape(null);
            targetLayer.setTransformBox(null);
            targetLayer.updateIconImage();
        }

        Tools.SHAPES.reset();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        // they were not connected, because of the
        // cloning in the constructor
        box.setTarget(shape);

        // if re-creating on a ShapesLayer, restore the shape to the layer
        if (targetLayer != null) {
            targetLayer.setStyledShape(shape);
            targetLayer.setTransformBox(box);
            targetLayer.updateIconImage();
        }

        Tools.SHAPES.restoreBox(shape, box);
    }
}

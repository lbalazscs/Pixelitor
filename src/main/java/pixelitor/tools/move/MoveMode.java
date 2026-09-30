/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.move;

import pixelitor.Composition;
import pixelitor.history.ContentLayerMoveEdit;
import pixelitor.history.History;
import pixelitor.history.MultiEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.Layer;
import pixelitor.selection.Selection;
import pixelitor.utils.Shapes;

import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * The type of moves available in the Move Tool.
 */
public enum MoveMode {
    MOVE_BOTH("Layer and Selection", "Move",
        true, true) {
    }, MOVE_SELECTION_ONLY("Selection Only", "Move Selection",
        true, false) {
    }, MOVE_LAYER_ONLY("Layer Only", ContentLayerMoveEdit.NAME,
        false, true) {
    };

    public static final String PRESET_KEY = "Move Mode";
    private final String displayName;
    private final String editName;
    private final boolean moveSelection;
    private final boolean moveLayer;

    MoveMode(String displayName, String editName, boolean moveSelection, boolean moveLayer) {
        this.displayName = displayName;
        this.editName = editName;
        this.moveSelection = moveSelection;
        this.moveLayer = moveLayer;
    }

    public boolean movesSelection() {
        return moveSelection;
    }

    public boolean movesLayer() {
        return moveLayer;
    }

    public String getEditName() {
        return editName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Prepares for moving the active layer/mask and/or the selection.
     */
    public void prepareMovement(Composition comp, boolean duplicateLayer) {
        if (movesLayer()) {
            if (duplicateLayer) {
                comp.duplicateActiveLayer();
            }

            comp.getActiveMoveTarget().prepareMovement();
        }
        if (movesSelection()) {
            Selection sel = comp.getSelection();
            if (sel != null) {
                sel.prepareForTransform();
            }
        }
    }

    /**
     * Updates the position of a content layer/selection during a drag operation (Move Tool).
     */
    public void moveActiveContent(Composition comp, double imDx, double imDy) {
        if (movesLayer()) {
            Layer target = comp.getActiveMoveTarget();
            target.moveWhileDragging(imDx, imDy);
            target.getHolder().invalidateImageCache();
        }
        if (movesSelection()) {
            Selection sel = comp.getSelection();
            if (sel != null) {
                sel.moveWhileDragging(imDx, imDy);
            }
        }
        comp.update();
    }

    /**
     * Finalizes a movement operation, adding history if changes occurred.
     */
    public void finalizeMovement(Composition comp) {
        PixelitorEdit layerEdit = null;
        if (movesLayer()) {
            Layer target = comp.getActiveMoveTarget();

            // will be null if a non-content layer without mask was moved
            layerEdit = target.finalizeMovement();
        }

        PixelitorEdit selectionEdit = null;
        if (movesSelection()) {
            Selection sel = comp.getSelection();
            if (sel != null) {
                selectionEdit = sel.finalizeTransform();
            }
        }

        var combinedEdit = MultiEdit.combine(
            layerEdit, selectionEdit, MOVE_BOTH.getEditName());
        if (combinedEdit != null) {
            History.add(combinedEdit);
            comp.update();
        }
    }

    /**
     * Draws visual feedback (the bounding box) for the content being moved.
     */
    public void drawMovementContours(Graphics2D g, Composition comp) {
        if (!movesLayer()) {
            return;
        }
        Layer target = comp.getActiveMoveTarget();
        if (target instanceof ContentLayer contentLayer) {
            Rectangle imBounds = contentLayer.getContentBounds();
            if (imBounds != null) {
                Shapes.drawVisibly(g, comp.getView().imageToComponentSpace(imBounds));
            }
        }
    }
}

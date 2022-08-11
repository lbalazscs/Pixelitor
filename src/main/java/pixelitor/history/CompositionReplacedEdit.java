/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.layers.MaskViewMode;
import pixelitor.tools.Tools;
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.lang.ref.SoftReference;

/**
 * Used when a composition is replaced either because
 * of a "file reload" or because of a multi-layer edit
 */
public class CompositionReplacedEdit extends PixelitorEdit {
    private SoftReference<Composition> backupCompRef;
    private final MaskViewMode oldMaskViewMode;
    private View view;

    private DeselectEdit oldDeselectEdit;
    private DeselectEdit newDeselectEdit;

    private final AffineTransform canvasTransform;
    private AffineTransform inverseCanvasTransform;
    private final boolean reload;

    public CompositionReplacedEdit(String name, View view,
                                   Composition oldComp, Composition newComp,
                                   AffineTransform canvasTransform, boolean reload) {
        super(name, newComp, true);
        this.reload = reload;

        assert oldComp != null;
        assert newComp != null;

        if (canvasTransform != null || reload) {
            if (!oldComp.hasSameFileAs(newComp, !reload)) {
                throw new IllegalStateException("old = " + oldComp.getFile() + ", new = " + newComp.getFile());
            }
        }

        if (oldComp.hasSelection()) {
            // saved compositions should never have a live selection,
            // only a selection shape inside the DeselectEdit
            oldDeselectEdit = oldComp.deselect(false);
        }

        backupCompRef = new SoftReference<>(oldComp);
        oldMaskViewMode = view.getMaskViewMode();
        this.view = view;
        this.canvasTransform = canvasTransform;

        assert !oldComp.hasSelection();
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        Composition oldComp = backupCompRef.get();
        if (oldComp == null) {
            throw new CannotUndoException();
        }

        if (comp.hasSelection()) {
            if (newDeselectEdit != null) { // undo after a redo
                newDeselectEdit.redo();
            } else { // first undo
                newDeselectEdit = comp.deselect(false);
            }
        }

        view.replaceComp(oldComp, oldMaskViewMode, reload);

        if (oldDeselectEdit != null) {
            oldDeselectEdit.undo();
        }

        // after an undo the new comp is stored in the soft
        // reference and the old comp in the edit
        Composition newComp = comp;
        comp = oldComp;
        backupCompRef = new SoftReference<>(newComp);

        assert !newComp.hasSelection();

        if (canvasTransform != null) { // there was a transform
            if (inverseCanvasTransform == null) { // first undo
                try {
                    inverseCanvasTransform = canvasTransform.createInverse();
                } catch (NoninvertibleTransformException e) {
                    inverseCanvasTransform = null;
                }
            }
            if (inverseCanvasTransform != null) { // successful inversion
                // the path of the composition was restored together with
                // the old comp, but the tool widgets need updating
                Tools.imCoordsChanged(comp, inverseCanvasTransform);
            }
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        Composition newComp = backupCompRef.get();
        if (newComp == null) {
            throw new CannotRedoException();
        }

        if (oldDeselectEdit != null) {
            oldDeselectEdit.redo();
        }

        view.replaceComp(newComp, MaskViewMode.NORMAL, reload);

        if (newDeselectEdit != null) {
            newDeselectEdit.undo();
        }

        // after a redo the old comp is stored again in the soft
        // reference and the new comp in the edit
        Composition oldComp = comp;
        comp = newComp;
        backupCompRef = new SoftReference<>(oldComp);

        assert !oldComp.hasSelection();

        if (canvasTransform != null) { // there was a transform
            Tools.imCoordsChanged(comp, canvasTransform);
        }
    }

    @Override
    public boolean makesDirty() {
        // reloading should not result in a dirty comp
        return !reload;
    }

    @Override
    public void die() {
        super.die();

        backupCompRef = null;
        view = null;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);
        Composition backupComp = backupCompRef.get();
        if (backupComp != null) {
            node.add(backupComp.createDebugNode("backup comp"));
        }
        return node;
    }
}

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

package pixelitor.history;

import pixelitor.Composition;
import pixelitor.compactions.CompAction;
import pixelitor.gui.CompositionReplacedContext;
import pixelitor.gui.View;
import pixelitor.layers.MaskViewMode;
import pixelitor.tools.Tools;
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.io.File;
import java.lang.ref.SoftReference;
import java.util.Objects;

/**
 * Used when a {@link Composition} is replaced in its {@link View},
 * for example during a file reload or a {@link CompAction} edit.
 */
public class CompositionReplacedEdit extends PixelitorEdit {
    private SoftReference<Composition> otherCompRef;
    private final MaskViewMode oldMaskViewMode;
    private final View view;

    private DeselectEdit oldDeselectEdit;
    private DeselectEdit newDeselectEdit;

    private final AffineTransform imTransform;
    private AffineTransform inverseImTransform;

    private final CompositionReplacedContext context;

    public CompositionReplacedEdit(String name, View view,
                                   Composition oldComp, Composition newComp,
                                   CompositionReplacedContext context) {
        assert context != CompositionReplacedContext.TRANSFORM;
        this(name, view, oldComp, newComp, null, context);
    }

    public CompositionReplacedEdit(String name, View view,
                                   Composition oldComp, Composition newComp,
                                   AffineTransform imTransform, CompositionReplacedContext context) {
        super(name, newComp, true);
        this.context = context;

        assert oldComp != null;
        assert oldComp != newComp;

        boolean isReload = context == CompositionReplacedContext.RELOAD;
        if (imTransform != null || isReload) {
            boolean allowBothNull = !isReload;
            if (!compsHaveSameFile(oldComp, newComp, allowBothNull)) {
                throw new IllegalStateException("old = " + oldComp.getFile() + ", new = " + newComp.getFile());
            }
        }

        if (oldComp.hasSelection()) {
            // saved compositions should never have a live selection,
            // only a selection shape inside the DeselectEdit
            oldDeselectEdit = oldComp.deselect(false);
        }

        otherCompRef = new SoftReference<>(oldComp);

        // saving this is needed for example when reloading a composition
        oldMaskViewMode = view.getMaskViewMode();

        this.view = view;
        this.imTransform = imTransform;

        assert !oldComp.hasSelection();
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        Composition oldComp = otherCompRef.get();
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

        view.replaceComp(oldComp, oldMaskViewMode, context);

        if (oldDeselectEdit != null) {
            oldDeselectEdit.undo();
        }

        // after an undo the new comp is stored in the soft
        // reference and the old comp in the edit
        Composition newComp = comp;
        comp = oldComp;
        otherCompRef = new SoftReference<>(newComp);

        assert !newComp.hasSelection();

        if (imTransform != null) { // there was a transform
            if (inverseImTransform == null) { // first undo
                try {
                    inverseImTransform = imTransform.createInverse();
                } catch (NoninvertibleTransformException e) {
                    inverseImTransform = null;
                }
            }
            if (inverseImTransform != null) { // successful inversion
                // the path of the composition was restored together with
                // the old comp, but the tool widgets need updating
                Tools.imCoordsChanged(inverseImTransform, view);
            }
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        Composition newComp = otherCompRef.get();
        if (newComp == null) {
            throw new CannotRedoException();
        }

        if (oldDeselectEdit != null) {
            oldDeselectEdit.redo();
        }

        view.replaceComp(newComp, MaskViewMode.NORMAL, context);

        if (newDeselectEdit != null) {
            newDeselectEdit.undo();
        }

        // after a redo the old comp is stored again in the soft
        // reference and the new comp in the edit
        Composition oldComp = comp;
        comp = newComp;
        otherCompRef = new SoftReference<>(oldComp);

        assert !oldComp.hasSelection();

        if (imTransform != null) { // there was a transform
            Tools.imCoordsChanged(imTransform, view);
        }
    }

    @Override
    public boolean makesDirty() {
        // reloading should not result in a dirty comp
        return context != CompositionReplacedContext.RELOAD;
    }

    private static boolean compsHaveSameFile(Composition comp1, Composition comp2, boolean allowBothNull) {
        File file1 = comp1.getFile();
        File file2 = comp2.getFile();
        if (file1 == null && file2 == null) {
            return allowBothNull;
        }
        return Objects.equals(file1, file2);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);
        node.addNullableDebuggable("other comp", otherCompRef.get());
        return node;
    }
}

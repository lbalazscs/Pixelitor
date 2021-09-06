/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.layers.ContentLayer;
import pixelitor.layers.ImageLayer;
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.image.BufferedImage;

/**
 * A PixelitorEdit representing the movement of a content layer.
 * (Move Tool)
 */
public class ContentLayerMoveEdit extends PixelitorEdit {
    public static final String NAME = "Move Layer";

    // can be null, if no image enlargement is taking place
    private ImageEdit imageEdit;

    private ContentLayer layer;
    private final TranslationEdit translationEdit;

    public ContentLayerMoveEdit(ContentLayer layer, BufferedImage backupImage, int oldTx, int oldTy) {
        super(NAME, layer.getComp());

        this.layer = layer;

        if (backupImage != null) {
            imageEdit = new ImageEdit("", comp, (ImageLayer) layer,
                backupImage, true);
            imageEdit.setEmbedded(true);
        }

        translationEdit = new TranslationEdit(comp, layer, oldTx, oldTy, false);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        if (imageEdit != null) {
            imageEdit.undo();
        }
        translationEdit.undo();

        if (!embedded) {
            layer.getComp().update();
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        if (imageEdit != null) {
            imageEdit.redo();
        }
        translationEdit.redo();

        if (!embedded) {
            layer.getComp().update();
        }
    }

    @Override
    public void die() {
        super.die();
        translationEdit.die();
        if (imageEdit != null) {
            imageEdit.die();
        }
        layer = null;
    }

    @Override
    public DebugNode createDebugNode() {
        var node = super.createDebugNode();

        node.add(translationEdit.createDebugNode());
        if (imageEdit != null) {
            node.add(imageEdit.createDebugNode());
        } else {
            node.addString("image edit", "null");
        }
        node.add(layer.createDebugNode("layer"));

        return node;
    }
}

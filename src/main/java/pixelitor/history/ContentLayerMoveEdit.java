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

import pixelitor.layers.ContentLayer;
import pixelitor.layers.ImageLayer;
import pixelitor.selection.IgnoreSelection;
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.image.BufferedImage;

import static pixelitor.Composition.ImageChangeActions.FULL;

/**
 * A PixelitorEdit hat represents the movement of a content layer.
 * (Move Tool)
 */
public class ContentLayerMoveEdit extends PixelitorEdit {
    public static final String NAME = "Move Layer";

    // can be null, if no image enlargement is taking place
    private ImageEdit imageEdit;

    private ContentLayer layer;
    private final TranslationEdit translationEdit;

    public ContentLayerMoveEdit(ContentLayer layer, BufferedImage backupImage, int oldTX, int oldTY) {
        super(layer.getComp(), NAME);

        this.layer = layer;

        if (backupImage != null) {
            imageEdit = new ImageEdit(comp, "", (ImageLayer) layer,
                    backupImage, IgnoreSelection.YES, false);
            imageEdit.setEmbedded(true);
        }

        this.translationEdit = new TranslationEdit(comp, layer, oldTX, oldTY, false);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        if (imageEdit != null) {
            imageEdit.undo();
        }
        translationEdit.undo();

        if (!embedded) {
            layer.getComp().imageChanged(FULL);
            History.notifyMenus(this);
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
            layer.getComp().imageChanged(FULL);
            History.notifyMenus(this);
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
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        node.add(translationEdit.getDebugNode());

        return node;
    }
}

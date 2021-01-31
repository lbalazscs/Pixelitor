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

import pixelitor.Composition;
import pixelitor.filters.painters.TextSettings;
import pixelitor.layers.TextLayer;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A PixelitorEdit that represents the editing of a text layer
 */
public class TextLayerChangeEdit extends PixelitorEdit {
    private TextSettings backupSettings;
    private TextLayer layer;

    public TextLayerChangeEdit(Composition comp, TextLayer layer,
                               TextSettings backupSettings) {
        super("Edit Text Layer", comp);

        this.backupSettings = backupSettings;
        this.layer = layer;

        if (backupSettings == layer.getSettings()) {
            throw new IllegalArgumentException("same settings");
        }
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        swapTextSettings();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        swapTextSettings();
    }

    private void swapTextSettings() {
        TextSettings tmp = layer.getSettings();
        layer.setSettings(backupSettings);
        backupSettings = tmp;

        layer.updateLayerName();

        layer.getComp().imageChanged();
    }

    @Override
    public void die() {
        super.die();

        layer = null;
    }
}

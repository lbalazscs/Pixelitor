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

package pixelitor.gui.utils;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.function.Consumer;

/**
 * A DocumentListener that forwards all events
 * (insert, remove, change) to a single callback.
 */
public class SimpleDocumentListener implements DocumentListener {
    private final Consumer<DocumentEvent> callback;

    public SimpleDocumentListener(Consumer<DocumentEvent> callback) {
        this.callback = callback;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        callback.accept(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        callback.accept(e);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        callback.accept(e);
    }
}

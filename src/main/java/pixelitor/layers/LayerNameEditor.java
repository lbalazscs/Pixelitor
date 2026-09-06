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

package pixelitor.layers;

import pixelitor.utils.Keys;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * A JTextField for editing layer names that becomes editable only if double-clicked.
 */
public class LayerNameEditor extends JTextField {
    private final LayerGUI layerGUI;

    public LayerNameEditor(LayerGUI layerGUI) {
        super(layerGUI.getLayer().getName());
        this.layerGUI = layerGUI;

        setToolTipText("Double-click to rename this layer.");
        disableEditing();

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                finishEditing();
            }
        });

        addActionListener(_ -> finishEditing());

        // cancel editing on Esc key
        getInputMap(WHEN_FOCUSED).put(Keys.ESC, "cancelEditing");
        getActionMap().put("cancelEditing", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelEditing();
            }
        });
    }

    private void finishEditing() {
        if (!isEditable()) {
            return; // prevent duplicate invocation caused by focus lost during disableEditing()
        }
        disableEditing();

        String newName = getText().trim();
        if (!newName.isEmpty()) {
            layerGUI.getLayer().setName(newName, true);
        } else {
            revert();
        }
    }

    public void cancelEditing() {
        if (!isEditable()) {
            return;
        }
        revert();
        disableEditing();
    }

    private void revert() {
        setText(layerGUI.getLayer().getName());
    }

    public void enableEditing() {
        setEnabled(true);
        setEditable(true);
        requestFocusInWindow();
        selectAll();
        getCaret().setVisible(true);
    }

    public void disableEditing() {
        setEnabled(false);
        setEditable(false);
    }
}

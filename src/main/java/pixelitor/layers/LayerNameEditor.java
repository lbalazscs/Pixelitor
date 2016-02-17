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

package pixelitor.layers;

import pixelitor.history.AddToHistory;

import javax.swing.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * A JTextField for layer names that becomes editable if double-clicked
 */
public class LayerNameEditor extends JTextField {
    private final LayerButton layerButton;

    public LayerNameEditor(LayerButton layerButton, Layer layer) {
        super(layer.getName());

//        setToolTipText("Double-click to rename this layer.");

        this.layerButton = layerButton;
        disableEditing();

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                disableEditing();
                layer.setName(getText(), AddToHistory.YES);
            }
        });

        // disable if enter pressed
        addActionListener(e -> {
            disableEditing();
            layer.setName(getText(), AddToHistory.YES);
        });

    }

    public void enableEditing() {
        setEnabled(true);
        setEditable(true);
        requestFocus();
        selectAll();
        getCaret().setVisible(true);
    }

    public void disableEditing() {
        setEnabled(false);
        setEditable(false);
    }

    public LayerButton getLayerButton() {
        return layerButton;
    }
}

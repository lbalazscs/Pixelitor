/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import javax.swing.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * A JTextField for layer names that becomes editable only if double-clicked
 */
public class LayerNameEditor extends JTextField {
    private final LayerButton layerButton;

    public LayerNameEditor(LayerButton layerButton) {
        super(layerButton.getLayer().getName());

        // TODO setting up a tool tip would show an
        // annoying GRAY "disabled tooltip"
        // One solution would be to put
        // UIManager.put("ToolTip[Disabled].backgroundPainter", UIManager.get("ToolTip[Enabled].backgroundPainter"));
        // at the beginning (not ideal), another would be
        // to create a custom tooltip specifically
        // for this component

        // setToolTipText("Double-click to rename this layer.");

        this.layerButton = layerButton;
        disableEditing();

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                finishEditing();
            }
        });

        // disable if enter pressed
        addActionListener(e -> finishEditing());
    }

    private void finishEditing() {
        disableEditing();
        layerButton.getLayer().setName(getText(), true);
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

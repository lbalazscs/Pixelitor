/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.gui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

import static java.awt.FlowLayout.LEFT;

/**
 * The GUI for a {@link TextParam}.
 */
public class TextParamGUI extends JPanel implements ParamGUI {
    private final TextParam model;
    private final JTextComponent textComponent;

    public TextParamGUI(TextParam model, String defaultValue, ParamAdjustmentListener adjustmentListener) {
        this.model = model;

        JLabel nameLabel = new JLabel(model.getName() + ": ");

        if (model.isCommand()) {
            setLayout(new BorderLayout());
            add(nameLabel, BorderLayout.WEST);

            textComponent = new JTextArea(defaultValue, 10, 25);
            JScrollPane scrollPane = new JScrollPane(textComponent);
            add(scrollPane, BorderLayout.CENTER);

            JButton runButton = new JButton("Run");
            runButton.addActionListener(e ->
                model.setValue(getText(), true));

            Box verticalBox = Box.createVerticalBox();
            verticalBox.add(Box.createVerticalGlue());
            verticalBox.add(runButton);
            verticalBox.add(Box.createVerticalGlue());
            add(verticalBox, BorderLayout.EAST);
        } else {
            setLayout(new FlowLayout(LEFT));
            add(nameLabel);

            textComponent = new JTextField(defaultValue, 25);
            add(textComponent);

            if (adjustmentListener != null) {
                addDocumentListener(adjustmentListener);
            }
        }
    }

    private void addDocumentListener(ParamAdjustmentListener adjustmentListener) {
        textComponent.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                adjustmentListener.paramAdjusted();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                adjustmentListener.paramAdjusted();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                adjustmentListener.paramAdjusted();
            }
        });
    }

    @Override
    public void updateGUI() {
        textComponent.setText(model.getValue());
    }

    @Override
    public boolean isEnabled() {
        return textComponent.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        textComponent.setEnabled(enabled);
    }

    public String getText() {
        return textComponent.getText().trim();
    }

    @Override
    public void setToolTip(String tip) {
        textComponent.setToolTipText(tip);
    }

    @Override
    public int getNumLayoutColumns() {
        return 1;
    }
}

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

import pixelitor.utils.Icons;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.SOUTH;

/**
 * The history panel.
 */
public class HistoryPanel extends JPanel {
    private final JButton undoButton;
    private final JButton redoButton;
    private final PixelitorUndoManager pum;

    public HistoryPanel(PixelitorUndoManager pum, JList<PixelitorEdit> historyList) {
        this.pum = pum;
        setLayout(new BorderLayout());
        add(new JScrollPane(historyList), CENTER);
        JPanel buttonsPanel = new JPanel(new FlowLayout());

        undoButton = createButton(Icons.getUndoIcon(), "undo",
            "AbstractUndoableEdit.undoText", History.UNDO_ACTION);
        redoButton = createButton(Icons.getRedoIcon(), "redo",
            "AbstractUndoableEdit.redoText", History.REDO_ACTION);

        History.addUndoableEditListener(e -> updateButtonsEnabledState());
        updateButtonsEnabledState();

        buttonsPanel.add(undoButton);
        buttonsPanel.add(redoButton);
        add(buttonsPanel, SOUTH);
    }

    private static JButton createButton(Icon icon, String name,
                                        String tooltipResource,
                                        ActionListener actionListener) {
        JButton b = new JButton(icon);
        b.setName(name);
        b.setToolTipText(UIManager.getString(tooltipResource));
        b.addActionListener(actionListener);
        return b;
    }

    private void updateButtonsEnabledState() {
        undoButton.setEnabled(pum.canUndo());
        redoButton.setEnabled(pum.canRedo());
    }
}

/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.AppContext;
import pixelitor.utils.Icons;
import pixelitor.utils.debug.Debug;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

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
        super(new BorderLayout());
        this.pum = pum;
        add(new JScrollPane(historyList), CENTER);
        JPanel buttonsPanel = new JPanel(new FlowLayout());

        undoButton = createButton(Icons.getUndoIcon(), "undo",
            "AbstractUndoableEdit.undoText", UndoAction.INSTANCE);
        redoButton = createButton(Icons.getRedoIcon(), "redo",
            "AbstractUndoableEdit.redoText", RedoAction.INSTANCE);

        History.addUndoableEditListener(e -> updateHistoryButtons());
        updateHistoryButtons();

        buttonsPanel.add(undoButton);
        buttonsPanel.add(redoButton);

        if (AppContext.isDevelopment()) {
            JButton debugButton = new JButton("Debug...");
            debugButton.addActionListener(e -> {
                PixelitorEdit edit = History.getEditToBeUndone();
                Debug.showTree(edit, edit.getName());
            });
            buttonsPanel.add(debugButton);
        }

        add(buttonsPanel, SOUTH);
    }

    private static JButton createButton(Icon icon, String name,
                                        String tooltipResource,
                                        Action action) {
        JButton b = new JButton(icon);
        b.setName(name);
        b.setToolTipText(UIManager.getString(tooltipResource));

        // Uses the action as a mere action listener in order
        // to avoid changing the button's text all the time.
        // The button's tooltip is updated instead.
        b.addActionListener(action);
        return b;
    }

    private void updateHistoryButtons() {
        undoButton.setEnabled(pum.canUndo());
        redoButton.setEnabled(pum.canRedo());

        undoButton.setToolTipText(pum.getUndoPresentationName());
        redoButton.setToolTipText(pum.getRedoPresentationName());
    }
}

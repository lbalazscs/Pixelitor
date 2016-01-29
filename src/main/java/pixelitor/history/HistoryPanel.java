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

import pixelitor.utils.IconUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class HistoryPanel extends JPanel {
    private final JButton undoButton;
    private final JButton redoButton;
    private final PixelitorUndoManager pum;

    public HistoryPanel(PixelitorUndoManager pum, JList historyList) {
        this.pum = pum;
        setLayout(new BorderLayout());
        add(new JScrollPane(historyList), BorderLayout.CENTER);
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        Icon undoIcon = IconUtils.getUndoIcon();
        Icon redoIcon = IconUtils.getRedoIcon();

        undoButton = new JButton(undoIcon);
        redoButton = new JButton(redoIcon);

        undoButton.setToolTipText(UIManager.getString("AbstractUndoableEdit.undoText"));
        redoButton.setToolTipText(UIManager.getString("AbstractUndoableEdit.redoText"));

        History.addUndoableEditListener(e -> {
            updateEnabledState();
        });
        updateEnabledState();

        undoButton.addActionListener(History.UNDO_ACTION);
        redoButton.addActionListener(History.REDO_ACTION);

        buttonsPanel.add(undoButton);
        buttonsPanel.add(redoButton);
        add(buttonsPanel, BorderLayout.SOUTH);
    }

    public void updateEnabledState() {
        undoButton.setEnabled(pum.canUndo());
        redoButton.setEnabled(pum.canRedo());
    }
}

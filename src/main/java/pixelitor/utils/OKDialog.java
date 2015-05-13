/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

package pixelitor.utils;

import pixelitor.GlobalKeyboardWatch;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.KeyEvent;

/**
 * A dialog with an OK button at the bottom
 */
public class OKDialog extends JDialog {
    public OKDialog(Frame owner, String title, JComponent form) {
        super(owner, title);

        setupGUI(form, true);
    }

    public OKDialog(Frame owner, String title) {
        super(owner, title);
    }

    public void setupGUI(JComponent form, boolean setVisible) {
        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);

        JPanel p2 = new JPanel();
        JButton ok = new JButton("OK");
        ok.setName("ok");
        p2.add(ok);
        add(p2, BorderLayout.SOUTH);

        ok.addActionListener(evt -> okPressed());

        // cancel when ESC is pressed
        ((JComponent) getContentPane()).registerKeyboardAction(e -> closeDialog(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        pack();

        GUIUtils.centerOnScreen(this);

        if (setVisible) {
            setVisible(true);
        }
    }

    protected void okPressed() {
        closeDialog();
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (b) {
            GlobalKeyboardWatch.setShowHideAllForTab(false);
        } else {
            GlobalKeyboardWatch.setShowHideAllForTab(true);
        }
    }

    private void closeDialog() {
        setVisible(false);
        dispose();
    }
}

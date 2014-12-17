/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.utils;

import pixelitor.GlobalKeyboardWatch;
import pixelitor.utils.GUIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 *
 */
public class OKDialog extends JDialog {
    public OKDialog(Frame owner, String title, JComponent form) {
        super(owner, title);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);

        JPanel p2 = new JPanel();
        JButton ok = new JButton("OK");
        p2.add(ok);
        add(p2, BorderLayout.SOUTH);

        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                closeDialog();
            }
        });

        // cancel when ESC is pressed
        ((JComponent) getContentPane()).registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeDialog();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        pack();

        GUIUtils.centerOnScreen(this);
        GlobalKeyboardWatch.setShowHideAllForTab(false);
        setVisible(true);
    }

    private void closeDialog() {
        setVisible(false);
        GlobalKeyboardWatch.setShowHideAllForTab(true);
        dispose();
    }
}

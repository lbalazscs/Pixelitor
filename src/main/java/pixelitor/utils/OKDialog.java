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
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.KeyEvent;

/**
 * A dialog with an OK button at the bottom
 */
public class OKDialog extends JDialog {
    private final String okButtonText;
    private JScrollPane scrollPane;

    public OKDialog(Frame owner, JComponent form, String title) {
        this(owner, form, title, "OK");
    }

    public OKDialog(Frame owner, JComponent form, String title, String okButtonText) {
        super(owner, title);
        this.okButtonText = okButtonText;

        assert form != null;
        setupGUI(form, true);
    }

    public OKDialog(Frame owner, String title, String okButtonText) {
        super(owner, title);
        this.okButtonText = okButtonText;
    }

    public OKDialog(Dialog owner, String title, String okButtonText) {
        super(owner, title);
        this.okButtonText = okButtonText;
    }


    public void setupGUI(JComponent form, boolean setVisible) {
        setLayout(new BorderLayout());

        scrollPane = new JScrollPane(form, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

//        add(form, BorderLayout.CENTER);

        JPanel p2 = new JPanel();
        JButton ok = new JButton(okButtonText);
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
            GlobalKeyboardWatch.setDialogActive(true);
        } else {
            GlobalKeyboardWatch.setDialogActive(false);
        }
    }

    private void closeDialog() {
        setVisible(false);
        dispose();
    }
}

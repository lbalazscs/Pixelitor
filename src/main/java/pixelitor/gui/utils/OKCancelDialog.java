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

package pixelitor.gui.utils;

import pixelitor.gui.GlobalEvents;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Frame;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static java.awt.BorderLayout.SOUTH;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
import static pixelitor.gui.utils.Screens.Align.SCREEN_CENTER;

/**
 * A dialog with OK and Cancel buttons at the bottom.
 *
 * Usually a better alternative for creating dialogs is {@link DialogBuilder}.
 */
public abstract class OKCancelDialog extends JDialog {
    private JComponent formPanel;
    private JLabel msgLabel;
    private JScrollPane scrollPane;
    private JButton okButton;

    protected OKCancelDialog(JComponent form, Frame owner,
                             String title, String okText, String cancelText) {
        super(owner, title, true);
        init(form, okText, cancelText, true);
    }

    private void init(JComponent form,
                      String okText, String cancelText, boolean addScrollBars) {
        assert EventQueue.isDispatchThread() : "not on EDT";

        formPanel = form;

        setLayout(new BorderLayout());
        addForm(form, addScrollBars);
        JPanel southPanel = new JPanel();
        okButton = new JButton(okText);
        okButton.setName("ok");
        JButton cancelButton = new JButton(cancelText);
        cancelButton.setName("cancel");

        GlobalEvents.dialogOpened(getTitle());

        GUIUtils.addOKCancelButtons(southPanel, okButton, cancelButton);

        add(southPanel, SOUTH);

        getRootPane().setDefaultButton(okButton);
        okButton.addActionListener(e -> {
            try {
                okAction();
            } catch (Exception ex) {
                Messages.showException(ex);
            }
        });

        cancelButton.addActionListener(e -> {
            try {
                cancelAction();
            } catch (Exception ex) {
                Messages.showException(ex);
            }
        });

        GUIUtils.setupCancelWhenTheDialogIsClosed(this, this::cancelAction);
        GUIUtils.setupCancelWhenEscIsPressed(this, this::cancelAction);

        pack();
        Screens.position(this, SCREEN_CENTER);
    }

    public void setOKButtonText(String text) {
        okButton.setText(text);
    }

    public void close() {
        setVisible(false);
        GlobalEvents.dialogClosed(getTitle());
        dispose();
    }

    protected abstract void okAction();

    /**
     * The default implementation only calls close()
     * If overridden, call close() manually
     */
    protected void cancelAction() {
        close();
    }

    public void setHeaderMessage(String msg) {
        if (msgLabel != null) { // there was a message before
            remove(msgLabel);
        }
        msgLabel = new JLabel(msg);
        msgLabel.setBorder(createEmptyBorder(0, 5, 0, 5));
        add(msgLabel, NORTH);
        revalidate();
    }

    public void changeForm(JComponent form) {
        if(scrollPane != null) {
            remove(scrollPane);
        } else {
            remove(formPanel);
        }
        formPanel = form;
        addForm(formPanel, true);
        revalidate();
    }

    private void addForm(JComponent form, boolean addScrollBars) {
        if (addScrollBars) {
            scrollPane = new JScrollPane(form,
                    VERTICAL_SCROLLBAR_AS_NEEDED,
                    HORIZONTAL_SCROLLBAR_NEVER);
            add(scrollPane, CENTER);
        } else {
            add(form, CENTER);
            scrollPane = null; // so that we later know that we have to remove from the root
        }
    }
}

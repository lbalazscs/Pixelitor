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

package pixelitor.gui.utils;

import pixelitor.gui.GUIText;
import pixelitor.gui.GlobalEvents;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Frame;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static java.awt.BorderLayout.SOUTH;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
import static pixelitor.gui.utils.Screens.Align.SCREEN_CENTER;
import static pixelitor.utils.Threads.callInfo;
import static pixelitor.utils.Threads.calledOnEDT;

/**
 * A dialog with OK and Cancel buttons at the bottom.
 *
 * For creating dialogs, consider using {@link DialogBuilder} for more flexibility.
 */
public abstract class OKCancelDialog extends JDialog {
    private JComponent content;
    private JLabel headerMsgLabel;
    private JScrollPane scrollPane;
    private final JButton okButton;

    protected OKCancelDialog(JComponent content, Frame owner,
                             String title, String okButtonText) {
        super(owner, title, true);
        assert calledOnEDT() : callInfo();

        this.content = content;

        setLayout(new BorderLayout());
        addContent(content);

        JPanel footerPanel = new JPanel();
        okButton = new JButton(okButtonText);
        JButton cancelButton = new JButton(GUIText.CANCEL);

        GlobalEvents.modalDialogOpened();

        GUIUtils.addOKCancelButtons(footerPanel, okButton, cancelButton);
        add(footerPanel, SOUTH);
        initOKButton();
        initCancelButton(cancelButton);

        pack();
        Screens.positionWindow(this, SCREEN_CENTER, null);
    }

    private void initOKButton() {
        okButton.setName("ok");
        getRootPane().setDefaultButton(okButton);
        okButton.addActionListener(e -> {
            try {
                dialogAccepted();
            } catch (Exception ex) {
                Messages.showException(ex);
            }
        });
    }

    private void initCancelButton(JButton cancelButton) {
        cancelButton.setName("cancel");
        cancelButton.addActionListener(e -> {
            try {
                dialogCanceled();
            } catch (Exception ex) {
                Messages.showException(ex);
            }
        });

        GUIUtils.setupCloseAction(this, this::dialogCanceled);
        GUIUtils.setupEscAction(this, this::dialogCanceled);
    }

    public void setOKButtonText(String text) {
        okButton.setText(text);
    }

    /**
     * Action to perform when the OK button is clicked.
     */
    protected abstract void dialogAccepted();

    /**
     * Action to perform when the Cancel button is clicked.
     * The default implementation closes the dialog.
     */
    protected void dialogCanceled() {
        close();
    }

    public void close() {
        setVisible(false);
        GlobalEvents.modalDialogClosed();
        dispose();
    }

    /**
     * Sets or updates the header message text.
     */
    public void setHeaderMessage(String text) {
        // if text is null or empty, remove the header
        if (text == null || text.isEmpty()) {
            if (headerMsgLabel != null) {
                remove(headerMsgLabel);
                headerMsgLabel = null;
                revalidate();
                repaint();
            }
            return;
        }

        if (headerMsgLabel == null) {
            // create label if it doesn't exist
            headerMsgLabel = new JLabel();
            headerMsgLabel.setBorder(createEmptyBorder(0, 5, 0, 5));
            add(headerMsgLabel, NORTH);
        }
        headerMsgLabel.setText(text);
        revalidate();
    }

    /**
     * Replaces the main content of the dialog with a new component.
     */
    public void updateContent(JComponent newContent) {
        remove(scrollPane);

        this.content = newContent;
        addContent(newContent);

        revalidate();
    }

    private void addContent(JComponent content) {
        scrollPane = new JScrollPane(content,
            VERTICAL_SCROLLBAR_AS_NEEDED,
            HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, CENTER);
    }

    public JButton getOkButton() {
        return okButton;
    }
}

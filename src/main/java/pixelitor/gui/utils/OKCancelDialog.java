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

package pixelitor.gui.utils;

import com.bric.util.JVM;
import pixelitor.gui.GlobalKeyboardWatch;
import pixelitor.gui.PixelitorWindow;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * A dialog with OK and Cancel buttons at the bottom
 */
public abstract class OKCancelDialog extends JDialog {
    protected JComponent formPanel;
    private JLabel messageLabel;
    private JScrollPane scrollPane;
    private JButton okButton;

    protected OKCancelDialog(JComponent form, String title) {
        this(form, PixelitorWindow.getInstance(), title, "OK", "Cancel");
    }

    protected OKCancelDialog(JComponent form, Frame owner, String title) {
        this(form, owner, title, "OK", "Cancel");
    }

    protected OKCancelDialog(JComponent form, Dialog owner, String title) {
        this(form, owner, title, "OK", "Cancel");
    }

    protected OKCancelDialog(JComponent form, Frame owner, String title, String okText, String cancelText) {
        this(form, owner, title, okText, cancelText, true);
    }

    protected OKCancelDialog(JComponent form, Dialog owner, String title, String okText, String cancelText) {
        this(form, owner, title, okText, cancelText, true);
    }

    protected OKCancelDialog(JComponent form, Frame owner, String title, String okText, String cancelText, boolean addScrollBars) {
        super(owner, title, true);
        init(form, okText, cancelText, addScrollBars);
    }

    protected OKCancelDialog(JComponent form, Dialog owner, String title, String okText, String cancelText, boolean addScrollBars) {
        super(owner, title, true);
        init(form, okText, cancelText, addScrollBars);
    }

    private void init(JComponent form, String okText, String cancelText, boolean addScrollBars) {
        assert EventQueue.isDispatchThread();

        this.formPanel = form;

        setLayout(new BorderLayout());
        addForm(form, addScrollBars);
        JPanel southPanel = new JPanel();
        okButton = new JButton(okText);
        okButton.setName("ok");
        JButton cancelButton = new JButton(cancelText);
        cancelButton.setName("cancel");

        GlobalKeyboardWatch.setDialogActive(true);

        if (JVM.isMac) {
            southPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
            southPanel.add(cancelButton);
            southPanel.add(okButton);
        } else {
            southPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
            southPanel.add(okButton);
            southPanel.add(cancelButton);
        }

        add(southPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(okButton);
        okButton.addActionListener(e -> {
            try {
                dialogAccepted();
            } catch (Exception ex) {
                Messages.showException(ex);
            }
        });

        cancelButton.addActionListener(e -> {
            try {
                dialogCanceled();
            } catch (Exception ex) {
                Messages.showException(ex);
            }
        });

        // cancel when window is closed
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // the user pressed the X button...
                dialogCanceled();
            }
        });

        // cancel when ESC is pressed
        ((JComponent) getContentPane()).registerKeyboardAction(e -> dialogCanceled(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        pack();
        GUIUtils.centerOnScreen(this);
    }

    public void setOKButtonEnabled(boolean b) {
        okButton.setEnabled(b);
    }

    public void setOKButtonText(String text) {
        okButton.setText(text);
    }

    public void close() {
        setVisible(false);
        GlobalKeyboardWatch.setDialogActive(false);
        dispose();
    }

    protected abstract void dialogAccepted();

    /**
     * The default implementation only calls close()
     * If overridden, call close() manually
     */
    protected void dialogCanceled() {
        close();
    }

    public void setHeaderMessage(String message) {
        if(messageLabel != null) { // there was a message before
            remove(messageLabel);
        }
        messageLabel = new JLabel(message);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        add(messageLabel, BorderLayout.NORTH);
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
            scrollPane = new JScrollPane(form, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            add(scrollPane, BorderLayout.CENTER);
        } else {
            add(form, BorderLayout.CENTER);
            scrollPane = null; // so that we later know that we have to remove from the root
        }
    }

}

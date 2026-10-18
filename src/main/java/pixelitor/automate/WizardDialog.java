/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.automate;

import com.bric.util.JVM;
import pixelitor.gui.GUIText;
import pixelitor.gui.GlobalEvents;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.Screens;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;

import static java.awt.BorderLayout.*;
import static java.awt.FlowLayout.RIGHT;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
import static pixelitor.gui.utils.Screens.Align.SCREEN_CENTER;
import static pixelitor.utils.Threads.callInfo;
import static pixelitor.utils.Threads.calledOnEDT;

/**
 * The dialog used by the wizards.
 */
public final class WizardDialog extends JDialog {
    public static final String BACK_TEXT = "< " + GUIText.BACK;
    public static final String NEXT_TEXT = GUIText.NEXT + " >";

    private final JLabel headerMsgLabel;
    private final JScrollPane scrollPane;
    private final JButton backButton;
    private final JButton nextButton;

    public WizardDialog(Frame owner, String title, int width, int height,
                        Runnable onBack, Runnable onNext, Runnable onCancel) {
        super(owner, title, true);

        assert calledOnEDT() : callInfo();
        assert onBack != null && onNext != null && onCancel != null;

        setLayout(new BorderLayout());

        headerMsgLabel = new JLabel();
        headerMsgLabel.setBorder(createEmptyBorder(0, 5, 0, 5));
        headerMsgLabel.setVisible(false);
        add(headerMsgLabel, NORTH);

        scrollPane = new JScrollPane(
            VERTICAL_SCROLLBAR_AS_NEEDED,
            HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, CENTER);

        JPanel footerPanel = new JPanel();
        backButton = new JButton(BACK_TEXT);
        nextButton = new JButton(NEXT_TEXT);
        JButton cancelButton = new JButton(GUIText.CANCEL);

        GlobalEvents.modalDialogOpened();

        addButtons(footerPanel, backButton, nextButton, cancelButton);
        add(footerPanel, SOUTH);

        initBackButton(onBack);
        initNextButton(onNext);
        initCancelButton(cancelButton, onCancel);

        setSize(width, height);
        Screens.positionWindow(this, SCREEN_CENTER);
    }

    private void addButtons(JPanel panel, JButton backButton,
                            JButton nextButton, JButton cancelButton) {
        panel.setLayout(new FlowLayout(RIGHT, 5, 5));
        if (JVM.isLinux || JVM.isMac) {
            panel.add(cancelButton);
            panel.add(backButton);
            panel.add(nextButton);
        } else { // Windows and unknown platforms
            panel.add(backButton);
            panel.add(nextButton);
            panel.add(cancelButton);
        }
    }

    private void initBackButton(Runnable onBack) {
        backButton.setName("back");
        backButton.setEnabled(false);
        backButton.addActionListener(_ -> runAction(onBack));
    }

    private void initNextButton(Runnable onNext) {
        nextButton.setName("next");
        getRootPane().setDefaultButton(nextButton);
        nextButton.addActionListener(_ -> runAction(onNext));
    }

    private void initCancelButton(JButton cancelButton, Runnable onCancel) {
        cancelButton.setName("cancel");
        cancelButton.addActionListener(_ -> runAction(onCancel));

        GUIUtils.installCloseHandler(this, () -> runAction(onCancel));
        GUIUtils.installEscHandler(this, () -> runAction(onCancel));
    }

    private void runAction(Runnable action) {
        try {
            action.run();
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    public void setBackButtonEnabled(boolean enabled) {
        backButton.setEnabled(enabled);
    }

    public void setNextButtonText(String text) {
        nextButton.setText(text);
    }

    public void setNextButtonEnabled(boolean enabled) {
        nextButton.setEnabled(enabled);
    }

    public void close() {
        setVisible(false);
        GlobalEvents.modalDialogClosed();
        dispose();
    }

    public void setHeaderMessage(String text) {
        boolean visible = text != null && !text.isEmpty();
        if (visible) {
            headerMsgLabel.setText("<html>" + text);
        }
        headerMsgLabel.setVisible(visible);
        revalidate();
    }

    /**
     * Sets the main content of the dialog.
     */
    public void setContent(JComponent content) {
        scrollPane.setViewportView(content);
        revalidate();
    }
}

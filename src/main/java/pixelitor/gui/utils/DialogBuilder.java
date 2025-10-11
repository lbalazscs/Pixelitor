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
import pixelitor.gui.PixelitorWindow;
import pixelitor.menus.edit.CopyAction;
import pixelitor.utils.Keys;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.function.Predicate;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.SOUTH;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
import static pixelitor.gui.utils.Screens.Align.SCREEN_CENTER;

/**
 * A fluent interface for building JDialogs.
 */
public class DialogBuilder {
    private static final String DEFAULT_OK_TEXT = GUIText.OK;
    private static final String DEFAULT_CANCEL_TEXT = GUIText.CANCEL;

    private String okText;
    private String cancelText;
    private boolean addOKButton = true;
    private boolean addCancelButton = true;
    private JComponent content;
    private boolean addScrollBars;
    private Window owner;
    private String title;
    private boolean modal = true;
    private boolean disposeOnClose = true;
    private Screens.Align align = SCREEN_CENTER;
    private JComponent parent;

    private boolean enableCopyShortcuts;

    private Runnable okAction;
    private Runnable cancelAction;
    private Predicate<JDialog> validator;

    private JDialog dialog;
    private boolean canceled;

    // Normally, a dialog is validated only when the "OK" button is
    // clicked. However, sometimes (e.g., if the "OK" button text is
    // "Close" and there is no Cancel button, but the dialog can still
    // be canceled with X or Esc), we want to prevent closing it
    // without validation.
    private boolean validateOnCancel = false;

    private String name;
    private JButton okButton;
    private JMenuBar menuBar;
    private Runnable onVisibleAction;

    private boolean built = false;

    public DialogBuilder() {
    }

    public DialogBuilder title(String title) {
        this.title = title;
        return this;
    }

    public DialogBuilder owner(Window window) {
        this.owner = window;
        return this;
    }

    /**
     * Sets the name property of the dialog (for AssertJ-Swing tests).
     */
    public DialogBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets an alternative text for the "OK" button.
     */
    public DialogBuilder okText(String text) {
        okText = text;
        return this;
    }

    /**
     * Sets an alternative text for the "Cancel" button.
     */
    public DialogBuilder cancelText(String text) {
        cancelText = text;
        return this;
    }

    /**
     * Uses the given component as the dialog's content.
     */
    public DialogBuilder content(JComponent content) {
        assert content != null;
        this.content = content;
        return this;
    }

    /**
     * Uses the given component as the dialog's content,
     * and also sets up validation based on it.
     */
    public DialogBuilder validatedContent(ValidatedPanel validatedPanel) {
        content = validatedPanel;
        return validator(d -> {
            ValidationResult validationResult = validatedPanel.validateSettings();
            if (validationResult.isValid()) {
                return true; // valid, allow the closing of the dialog
            } else {
                validationResult.showErrorDialog(d);
                return false;
            }
        });
    }

    public DialogBuilder notModal() {
        modal = false;
        return this;
    }

    public DialogBuilder align(Screens.Align align) {
        this.align = align;
        return this;
    }

    public DialogBuilder reusable() {
        disposeOnClose = false;
        return this;
    }

    public DialogBuilder validateOnCancel() {
        validateOnCancel = true;
        return this;
    }

    public DialogBuilder withScrollbars() {
        addScrollBars = true;
        return this;
    }

    public DialogBuilder enableCopyShortcuts() {
        enableCopyShortcuts = true;
        return this;
    }

    public DialogBuilder noCancelButton() {
        addCancelButton = false;
        return this;
    }

    public DialogBuilder noOKButton() {
        addOKButton = false;
        return this;
    }

    public DialogBuilder okAction(Runnable r) {
        okAction = r;
        return this;
    }

    public DialogBuilder cancelAction(Runnable r) {
        cancelAction = r;
        return this;
    }

    /**
     * When "OK" is pressed (and when canceled, if validateOnCancel is set),
     * the dialog will close only if the given predicate evaluates to true.
     * The predicate must show an error dialog if it returns false.
     * The predicate will be evaluated with the built dialog,
     * which should be used as the owner of the error dialog.
     */
    public DialogBuilder validator(Predicate<JDialog> p) {
        validator = p;
        return this;
    }

    public DialogBuilder menuBar(JMenuBar m) {
        menuBar = m;
        return this;
    }

    public DialogBuilder parentComponent(JComponent c) {
        parent = c;
        return this;
    }

    public JButton getOkButton() {
        return okButton;
    }

    /**
     * Builds the dialog and also shows it.
     */
    public DialogBuilder show() {
        if (RandomGUITest.isRunning()) {
            return this; // avoid showing dialogs
        }

        JDialog d = build();
        if (parent != null) {
            GUIUtils.showDialog(d, parent);
        } else {
            GUIUtils.showDialog(d, align);
        }

        return this;
    }

    /**
     * Builds the dialog without showing it.
     */
    public JDialog build() {
        if (built) {
            throw new IllegalStateException("can only be used once");
        }
        built = true;

        assert content != null : "no content";

        setupDefaults();
        createDialog();

        dialog.setTitle(title);
        dialog.setModal(modal);
        if (menuBar != null) {
            dialog.setJMenuBar(menuBar);
        }

        if (name != null) {
            dialog.setName(name);
        }

        addContent(dialog);
        addButtons(dialog);

        Runnable cancelTask = () -> dialogCanceled(dialog);
        GUIUtils.setupCloseAction(dialog, cancelTask);
        GUIUtils.setupEscAction(dialog, cancelTask);

        if (enableCopyShortcuts) {
            JComponent contentPane = (JComponent) dialog.getContentPane();
            InputMap inputMap = contentPane.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            ActionMap actionMap = contentPane.getActionMap();

            inputMap.put(Keys.CTRL_C, "copy");
            actionMap.put("copy", CopyAction.COPY_LAYER);

            inputMap.put(Keys.CTRL_SHIFT_C, "copyvis");
            actionMap.put("copyvis", CopyAction.COPY_COMPOSITE);
        }

        dialog.pack();

        if (onVisibleAction != null) {
            dialog.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentShown(ComponentEvent e) {
                    onVisibleAction.run();
                }
            });
        }

        return dialog;
    }

    private void createDialog() {
        if (owner != null) {
            dialog = new BuiltDialog(owner, modal);
        } else {
            var pw = PixelitorWindow.get();
            dialog = new BuiltDialog(pw, modal);
        }
    }

    public JDialog getDialog() {
        assert !modal; // for modal dialogs it doesn't make sense to call this
        assert dialog != null;
        return dialog;
    }

    private void addContent(JDialog d) {
        d.setLayout(new BorderLayout());
        if (addScrollBars) {
            JScrollPane scrollPane = new JScrollPane(content,
                VERTICAL_SCROLLBAR_AS_NEEDED,
                HORIZONTAL_SCROLLBAR_NEVER);
            d.add(scrollPane, CENTER);
        } else {
            d.add(content, CENTER);
        }
    }

    private void addButtons(JDialog d) {
        if (!addOKButton && !addCancelButton) {
            return;
        }

        if (addOKButton) {
            okButton = new JButton(okText);
            okButton.setName("ok");
            okButton.addActionListener(e -> okButtonPressed(d));
            d.getRootPane().setDefaultButton(okButton);
        }

        JButton cancelButton = null;
        if (addCancelButton) {
            cancelButton = new JButton(cancelText);
            cancelButton.setName("cancel");

            cancelButton.addActionListener(e -> dialogCanceled(d));
        }

        JPanel southPanel = new JPanel();
        if (okButton != null && cancelButton != null) {
            GUIUtils.addOKCancelButtons(southPanel, okButton, cancelButton);
        } else if (okButton != null) {
            southPanel.add(okButton);
        } else {
            southPanel.add(cancelButton);
        }
        d.add(southPanel, SOUTH);
    }

    private void okButtonPressed(JDialog d) {
        if (isDialogInvalid(d)) {
            // keep the dialog open
            return;
        }

        closeDialog(d);
        if (okAction != null) {
            okAction.run();
        }
        canceled = false;
    }

    // a dialog without a Cancel button can still be canceled with Esc/X
    private void dialogCanceled(JDialog d) {
        if (validateOnCancel && isDialogInvalid(d)) {
            // keep the dialog open
            return;
        }
        closeDialog(d);
        if (cancelAction != null) {
            cancelAction.run();
        }
        canceled = true;
    }

    public boolean wasAccepted() {
        return !canceled;
    }

    private boolean isDialogInvalid(JDialog d) {
        return validator != null && !validator.test(d);
    }

    private void closeDialog(JDialog d) {
        GUIUtils.closeDialog(d, disposeOnClose);
    }

    private void setupDefaults() {
        if (okText == null) {
            okText = DEFAULT_OK_TEXT;
        }
        if (cancelText == null) {
            cancelText = DEFAULT_CANCEL_TEXT;
        }
    }

    public DialogBuilder onVisibleAction(Runnable onVisibleAction) {
        this.onVisibleAction = onVisibleAction;
        return this;
    }

    private static class BuiltDialog extends JDialog {
        private final boolean rootDialog; // true if the owner is the main window

        BuiltDialog(Window owner, boolean modal) {
            super(owner, modal ? ModalityType.APPLICATION_MODAL : ModalityType.MODELESS);
            rootDialog = (owner instanceof Frame); // the main window is the only frame
        }

        @Override
        public void setVisible(boolean visible) {
            if (isModal()) {
                if (visible) {
                    GlobalEvents.dialogOpened(getTitle());
                    assert !rootDialog || GlobalEvents.getModalDialogCount() == 1;
                } else {
                    GlobalEvents.dialogClosed(getTitle());
                    assert !rootDialog || GlobalEvents.getModalDialogCount() == 0;
                }
            }
            super.setVisible(visible);
        }
    }
}

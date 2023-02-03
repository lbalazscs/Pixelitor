/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import java.util.function.Predicate;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.SOUTH;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
import static pixelitor.gui.utils.Screens.Align.SCREEN_CENTER;

/**
 * A fluent interface for building JDialogs
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
    private JFrame frameOwner;
    private JDialog dialogOwner;
    private String title;
    private boolean modal = true;
    private boolean disposeWhenClosing = true;
    private Screens.Align align = SCREEN_CENTER;
    private JComponent parent;

    private boolean enableCopyVisibleShortcut;

    private Runnable okAction;
    private Runnable cancelAction;
    private Predicate<JDialog> validator;

    private JDialog dialog;
    private boolean cancelled;

    // normally a dialog is validated only when OK is pressed,
    // but sometimes (for example if the OK button text is "Close",
    // and there is no Cancel button, but the dialog can still be
    // canceled with X or Esc) we just don't want to allow
    // closing it without validating.
    private boolean validateWhenCanceled = false;

    private String name;
    private JButton okButton;
    private JMenuBar menuBar;

    public DialogBuilder() {
    }

    public DialogBuilder title(String s) {
        title = s;
        return this;
    }

    public DialogBuilder owner(Window window) {
        if (window instanceof JFrame) {
            frameOwner = (JFrame) window;
        } else if (window instanceof JDialog) {
            dialogOwner = (JDialog) window;
        } else {
            throw new IllegalStateException(window == null
                ? "null window"
                : window.getClass().getName());
        }
        return this;
    }

    /**
     * Sets the name property of the dialog (for AssertJ-Swing tests)
     */
    public DialogBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets an alternative text for the "OK" button
     */
    public DialogBuilder okText(String s) {
        okText = s;
        return this;
    }

    /**
     * Sets an alternative text for the "Cancel" button
     */
    public DialogBuilder cancelText(String s) {
        cancelText = s;
        return this;
    }

    /**
     * Uses the given component as the contents of the dialog.
     */
    public DialogBuilder content(JComponent form) {
        assert form != null;
        content = form;
        return this;
    }

    /**
     * Uses the given component as the contents of the dialog, and also
     * sets up validation based on it.
     */
    public DialogBuilder validatedContent(ValidatedPanel validatedPanel) {
        content = validatedPanel;
        return validator(d -> {
            ValidationResult validationResult = validatedPanel.validateSettings();
            if (validationResult.isOK()) {
                return true; // valid, let the dialog close
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

    public DialogBuilder willBeShownAgain() {
        disposeWhenClosing = false;
        return this;
    }

    public DialogBuilder validateWhenCanceled() {
        validateWhenCanceled = true;
        return this;
    }

    public DialogBuilder withScrollbars() {
        addScrollBars = true;
        return this;
    }

    public DialogBuilder enableCopyVisibleShortcut() {
        enableCopyVisibleShortcut = true;
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
     * When OK is pressed (and when canceled, if validateWhenCanceled is set),
     * the dialog will close only if the given predicate evaluates to true.
     * The predicate must show an error dialog if it is returning false.
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
            return null; // avoid dialogs
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

        Runnable cancelTask = () -> dialogCancelled(dialog);
        GUIUtils.setupCloseAction(dialog, cancelTask);
        GUIUtils.setupEscAction(dialog, cancelTask);

        if (enableCopyVisibleShortcut) {
            JComponent contentPane = (JComponent) dialog.getContentPane();
            InputMap inputMap = contentPane.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            ActionMap actionMap = contentPane.getActionMap();

            inputMap.put(Keys.CTRL_C, "copy");
            actionMap.put("copy", CopyAction.COPY_LAYER);

            inputMap.put(Keys.CTRL_SHIFT_C, "copyvis");
            actionMap.put("copyvis", CopyAction.COPY_COMPOSITE);
        }

        dialog.pack();
        return dialog;
    }

    private void createDialog() {
        if (frameOwner != null) {
            dialog = new BuiltDialog(frameOwner, modal);
        } else if (dialogOwner != null) {
            dialog = new BuiltDialog(dialogOwner, modal);
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

            cancelButton.addActionListener(e -> dialogCancelled(d));
        }

        JPanel southPanel = null;
        if (addOKButton || addCancelButton) {
            southPanel = new JPanel();
            d.add(southPanel, SOUTH);
        }

        if (addOKButton) {
            if (addCancelButton) { // add both
                GUIUtils.addOKCancelButtons(southPanel, okButton, cancelButton);
            } else { // only ok button
                southPanel.add(okButton);
            }
        }
    }

    private void okButtonPressed(JDialog d) {
        if (dialogIsInvalid(d)) {
            // keep the dialog open
            return;
        }

        closeDialog(d);
        if (okAction != null) {
            okAction.run();
        }
        cancelled = false;
    }

    // a dialog without a Cancel button can still be cancelled with Esc/X
    private void dialogCancelled(JDialog d) {
        if (validateWhenCanceled && dialogIsInvalid(d)) {
            // keep the dialog open
            return;
        }
        closeDialog(d);
        if (cancelAction != null) {
            cancelAction.run();
        }
        cancelled = true;
    }

    public boolean wasAccepted() {
        return !cancelled;
    }

    private boolean dialogIsInvalid(JDialog d) {
        return validator != null && !validator.test(d);
    }

    private void closeDialog(JDialog d) {
        assert d.isVisible();
        GUIUtils.closeDialog(d, disposeWhenClosing);
    }

    private void setupDefaults() {
        if (okText == null) {
            okText = DEFAULT_OK_TEXT;
        }
        if (cancelText == null) {
            cancelText = DEFAULT_CANCEL_TEXT;
        }
    }

    static class BuiltDialog extends JDialog {
        private final boolean isModal;
        private final boolean rootDialog; // true if the owner is the main window

        public BuiltDialog(Frame owner, boolean isModal) {
            super(owner);
            this.isModal = isModal;
            rootDialog = true; // the main window is the only frame
        }

        public BuiltDialog(Window owner, boolean isModal) {
            super(owner);
            this.isModal = isModal;
            rootDialog = false;
        }

        @Override
        public void setVisible(boolean b) {
            assert isModal == isModal();
            if (isModal) {
                if (b) {
                    GlobalEvents.dialogOpened(getTitle());
                    assert !rootDialog || GlobalEvents.getNumModalDialogs() == 1;
                } else {
                    GlobalEvents.dialogClosed(getTitle());
                    assert !rootDialog || GlobalEvents.getNumModalDialogs() == 0;
                }
            }
            super.setVisible(b);
        }
    }
}

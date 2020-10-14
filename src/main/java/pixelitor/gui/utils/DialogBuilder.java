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
import pixelitor.gui.PixelitorWindow;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Window;
import java.util.function.Predicate;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.SOUTH;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
import static pixelitor.gui.utils.Screens.Align.SCREEN_CENTER;

/**
 * A fluent interface for building JDialogs
 */
public class DialogBuilder {
    private static final String DEFAULT_OK_TEXT =
        UIManager.getString("OptionPane.okButtonText");
    private static final String DEFAULT_CANCEL_TEXT =
        UIManager.getString("OptionPane.cancelButtonText");

    private String okText;
    private String cancelText;
    private boolean addOKButton = true;
    private boolean addCancelButton = true;
    private JComponent content;
    private boolean addScrollBars;
    private JFrame frameOwner;
    private JDialog dialogOwner;
    private String title;
    private boolean notifyGlobalEvents = true;
    private boolean modal = true;
    private boolean disposeWhenClosing = true;
    private Screens.Align align = SCREEN_CENTER;

    private Runnable okAction;
    private Runnable cancelAction;
    private Predicate<JDialog> validator;

    // normally a dialog is validated only when OK is pressed,
    // but sometimes (for example if the OK button text is "Close",
    // and there is no Cancel button, but the dialog can still be
    // canceled with X or Esc) we just don't want to allow
    // closing it without validating.
    private boolean validateWhenCanceled = false;

    private String name;
    private JButton okButton;

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
            ValidationResult validationResult = validatedPanel.checkValidity();
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

    public DialogBuilder noCancelButton() {
        addCancelButton = false;
        return this;
    }

    public DialogBuilder noOKButton() {
        addOKButton = false;
        return this;
    }

    public DialogBuilder noGlobalKeyChange() {
        notifyGlobalEvents = false;
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
     * The argument of the predicate is the dialog which is built here,
     * and should be used as the owner of the error dialog.
     */
    public DialogBuilder validator(Predicate<JDialog> p) {
        validator = p;
        return this;
    }

    public JButton getOkButton() {
        return okButton;
    }

    /**
     * Builds the dialog and also shows it.
     */
    public JDialog show() {
        if (RandomGUITest.isRunning()) {
            return null; // avoid dialogs
        }

        JDialog d = build();
        GUIUtils.showDialog(d, align);
        return d;
    }

    /**
     * Builds the dialog without showing it.
     */
    public JDialog build() {
        assert content != null : "no content";

        setupDefaults();

        JDialog d = createDialog();

        d.setTitle(title);
        d.setModal(modal);

        if (name != null) {
            d.setName(name);
        }

        addContent(d);
        addButtons(d);

        Runnable cancelTask = () -> dialogCancelled(d);
        GUIUtils.setupCancelWhenTheDialogIsClosed(d, cancelTask);
        GUIUtils.setupCancelWhenEscIsPressed(d, cancelTask);

        d.pack();
        return d;
    }

    private JDialog createDialog() {
        JDialog d;
        if (frameOwner != null) {
            d = new BuiltDialog(frameOwner, notifyGlobalEvents);
        } else if (dialogOwner != null) {
            d = new BuiltDialog(dialogOwner, notifyGlobalEvents);
        } else {
            var pw = PixelitorWindow.get();
            d = new BuiltDialog(pw, notifyGlobalEvents);
        }
        return d;
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
        private final boolean notifyGlobalEvents;
        private final boolean rootDialog; // true if the owner is the main window

        public BuiltDialog(Frame owner, boolean notifyGlobalEvents) {
            super(owner);
            this.notifyGlobalEvents = notifyGlobalEvents;
            rootDialog = true; // the main window is the only frame
        }

        public BuiltDialog(Window owner, boolean notifyGlobalEvents) {
            super(owner);
            this.notifyGlobalEvents = notifyGlobalEvents;
            rootDialog = false;
        }

        @Override
        public void setVisible(boolean b) {
            if (notifyGlobalEvents) {
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

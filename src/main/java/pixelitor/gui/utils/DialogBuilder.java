/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.awt.BorderLayout.SOUTH;
import static java.awt.FlowLayout.CENTER;
import static java.awt.FlowLayout.RIGHT;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE;

/**
 * A fluent interface for building JDialogs
 */
public class DialogBuilder {
    private static final String DEFAULT_OK_TEXT = UIManager.getString("OptionPane.okButtonText");
    private static final String DEFAULT_CANCEL_TEXT = UIManager.getString("OptionPane.cancelButtonText");

    private String okText;
    private String cancelText;
    private boolean addOKButton = true;
    private boolean addCancelButton = true;
    private JComponent form;
    private boolean addScrollBars;
    private JFrame frameOwner;
    private JDialog dialogOwner;
    private String title;
    private boolean reconfigureGlobalKeyWatch = true;
    private boolean modal = true;

    private Runnable okAction;
    private Runnable cancelAction;
    private Predicate<JDialog> validator;

    // normally a dialog is validated only when OK is pressed,
    // but sometimes (for example if the OK button text is "Close",
    // and there is no Cancel button, but the dialog can still be
    // canceled with X or Esc) we just don't want to allow
    // closing it without validating.
    private boolean validateWhenCanceled = false;

    private Supplier<JDialog> dialogFactory;
    private String name;

    public DialogBuilder() {
    }

    public DialogBuilder title(String s) {
        this.title = s;
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
        this.okText = s;
        return this;
    }

    /**
     * Sets an alternative text for the "Cancel" button
     */
    public DialogBuilder cancelText(String s) {
        this.cancelText = s;
        return this;
    }

    /**
     * Uses the given component as the contents of the dialog.
     */
    public DialogBuilder content(JComponent form) {
        this.form = form;
        return this;
    }

    /**
     * Uses the given component as the contents of the dialog, and also
     * sets up validation based on it.
     */
    public DialogBuilder validatedContent(ValidatedPanel validatedPanel) {
        this.form = validatedPanel;
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
        this.modal = false;
        return this;
    }

    public DialogBuilder validateWhenCanceled() {
        this.validateWhenCanceled = true;
        return this;
    }

    public DialogBuilder withScrollbars() {
        this.addScrollBars = true;
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
        this.reconfigureGlobalKeyWatch = false;
        return this;
    }

    public DialogBuilder okAction(Runnable r) {
        this.okAction = r;
        return this;
    }

    public DialogBuilder cancelAction(Runnable r) {
        this.cancelAction = r;
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
        this.validator = p;
        return this;
    }

    public DialogBuilder dialogFactory(Supplier<JDialog> dialogFactory) {
        this.dialogFactory = dialogFactory;
        return this;
    }

    /**
     * Builds the dialog and also shows it.
     */
    public JDialog show() {
        JDialog d = build();
        GUIUtils.showDialog(d);
        return d;
    }

    /**
     * Builds the dialog without showing it.
     */
    public JDialog build() {
        assert form != null : "no form";

        setupDefaults();

        JDialog d;
        if (dialogFactory != null) {
            d = dialogFactory.get();
        } else {
            if (frameOwner != null) {
                d = new JDialog(frameOwner);
            } else if (dialogOwner != null) {
                d = new JDialog(dialogOwner);
            } else {
                PixelitorWindow pw = PixelitorWindow.getInstance();
                d = new JDialog(pw);
            }
        }

        d.setTitle(title);
        d.setModal(modal);

        if (name != null) {
            d.setName(name);
        }

        d.setLayout(new BorderLayout());
        if (addScrollBars) {
            JScrollPane scrollPane = new JScrollPane(form,
                    VERTICAL_SCROLLBAR_AS_NEEDED,
                    HORIZONTAL_SCROLLBAR_NEVER);
            d.add(scrollPane, BorderLayout.CENTER);
        } else {
            d.add(form, BorderLayout.CENTER);
        }

        JButton okButton = null;
        if (addOKButton) {
            okButton = new JButton(okText);
            okButton.setName("ok");
            okButton.addActionListener(e -> {
                if (validator != null) {
                    if (!validator.test(d)) {
                        // keep the dialog open
                        return;
                    }
                }

                closeDialog(d);
                if (okAction != null) {
                    okAction.run();
                }
            });
            d.getRootPane().setDefaultButton(okButton);
        }
        JButton cancelButton = null;
        if (addCancelButton) {
            cancelButton = new JButton(cancelText);
            cancelButton.setName("cancel");

            cancelButton.addActionListener(e -> cancelDialog(d, cancelAction));
        }

        JPanel southPanel = null;
        if (addOKButton || addCancelButton) {
            southPanel = new JPanel();
            d.add(southPanel, SOUTH);
        }

        if (addOKButton) {
            if (addCancelButton) { // add both
                if (JVM.isMac) {
                    southPanel.setLayout(new FlowLayout(RIGHT, 5, 5));
                    southPanel.add(cancelButton);
                    southPanel.add(okButton);
                } else {
                    southPanel.setLayout(new FlowLayout(CENTER, 5, 5));
                    southPanel.add(okButton);
                    southPanel.add(cancelButton);
                }
            } else { // only ok button
                southPanel.add(okButton);
            }
        }

        if (reconfigureGlobalKeyWatch) {
            GlobalKeyboardWatch.setDialogActive(true);
        }

        // cancel when window is closed
        d.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        d.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // the user pressed the X button...
                cancelDialog(d, cancelAction);
            }
        });

        // cancel when ESC is pressed
        ((JComponent) d.getContentPane()).registerKeyboardAction(e ->
                        cancelDialog(d, cancelAction),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);


        d.pack();
        return d;
    }

    private void setupDefaults() {
        if (okText == null) {
            okText = DEFAULT_OK_TEXT;
        }
        if (cancelText == null) {
            cancelText = DEFAULT_CANCEL_TEXT;
        }
    }

    // an OK dialog can still be cancelled with Esc/X
    private void cancelDialog(JDialog d, Runnable cancelAction) {
        if (validateWhenCanceled && validator != null) {
            if (!validator.test(d)) {
                // keep the dialog open
                return;
            }
        }
        closeDialog(d);
        if (cancelAction != null) {
            cancelAction.run();
        }
    }

    private void closeDialog(JDialog d) {
        d.setVisible(false);
        if (reconfigureGlobalKeyWatch) {
            GlobalKeyboardWatch.setDialogActive(false);
        }
        d.dispose();
    }
}

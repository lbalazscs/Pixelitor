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

package pixelitor.automate;

import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.OKCancelDialog;
import pixelitor.layers.Drawable;

import javax.swing.*;
import java.awt.Component;
import java.util.Objects;

/**
 * A wizard. The individual pages implement
 * the {@link WizardPage} interface.
 */
public abstract class Wizard {
    private OKCancelDialog dialog = null;
    private WizardPage wizardPage;
    private final String dialogTitle;
    private final String finishButtonText;
    private final int initialDialogWidth;
    private final int initialDialogHeight;
    protected final Drawable dr;

    protected Wizard(WizardPage initialPage, String dialogTitle, String finishButtonText, int initialDialogWidth, int initialDialogHeight, Drawable dr) {
        this.wizardPage = initialPage;
        this.dialogTitle = dialogTitle;
        this.finishButtonText = finishButtonText;
        this.initialDialogWidth = initialDialogWidth;
        this.initialDialogHeight = initialDialogHeight;
        this.dr = Objects.requireNonNull(dr);
    }

    /**
     * Show the wizard in a dialog
     */
    public void start(JFrame dialogParent) {
        try {
            showDialog(dialogParent, dialogTitle);
        } finally {
            finalCleanup();
        }
    }

    private void showDialog(JFrame dialogParent, String title) {
        assert dialog == null; // this should be called once per object

        dialog = new OKCancelDialog(
                wizardPage.getPanel(Wizard.this, dr),
                dialogParent,
                title,
                "Next", "Cancel") {

            @Override
            protected void dialogCanceled() {
                wizardPage.onWizardCanceled(dr);
                super.dialogCanceled();
                dispose();
            }

            @Override
            protected void dialogAccepted() { // "next" was pressed
                if (!mayMoveForwardIfNextPressed(wizardPage, this)) {
                    return;
                }

                // move forward
                wizardPage.onMovingToTheNext(Wizard.this, dr);

                if (!mayProceedAfterMovingForward(wizardPage, this)) {
                    return;
                }

                WizardPage nextPage = wizardPage.getNext();
                if (nextPage == null) { // dialog finished
                    dispose();
                    finalAction();
                } else {
                    JComponent panel = nextPage.getPanel(Wizard.this, dr);
                    dialog.changeForm(panel);
                    dialog.setHeaderMessage(nextPage.getHeaderText(Wizard.this));
                    wizardPage = nextPage;

                    if (wizardPage.getNext() == null) { // this is the last page
                        setOKButtonText(finishButtonText);
                    }
                }
            }
        };
        dialog.setHeaderMessage(wizardPage.getHeaderText(this));

        // it was packed already, but this is not correct because of the header message
        // and anyway we don't know the size of the filter dialogs in advance
        dialog.setSize(initialDialogWidth, initialDialogHeight);

        GUIUtils.centerOnScreen(dialog);
        dialog.setVisible(true);
    }

    protected abstract boolean mayMoveForwardIfNextPressed(WizardPage wizardPage, Component dialogParent);

    protected abstract boolean mayProceedAfterMovingForward(WizardPage wizardPage, Component dialogParent);

    protected abstract void finalAction();

    protected abstract void finalCleanup();
}

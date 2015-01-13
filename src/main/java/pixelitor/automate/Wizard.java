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

package pixelitor.automate;

import pixelitor.utils.GUIUtils;
import pixelitor.utils.OKCancelDialog;

import javax.swing.*;
import java.awt.Component;

public abstract class Wizard {
    private OKCancelDialog dialog = null;
    private WizardPage wizardPage;
    private String dialogTitle;

    protected Wizard(WizardPage initialWizardPage, String dialogTitle) {
        this.wizardPage = initialWizardPage;
        this.dialogTitle = dialogTitle;
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

    public void showDialog(JFrame dialogParent, final String title) {
        dialog = new OKCancelDialog(
                wizardPage.getPanel(Wizard.this),
                dialogParent,
                title,
                "Next", "Cancel") {

            @Override
            protected void dialogCanceled() {
                wizardPage.onWizardCancelled(Wizard.this);
                super.dialogCanceled();
                dispose();
            }

            @Override
            protected void dialogAccepted() { // "next" was pressed
                if (!mayMoveForwardIfNextPressed(wizardPage, this)) {
                    return;
                }

                // move forward
                wizardPage.onMovingToTheNext(Wizard.this);

                if (!mayProceedAfterMovingForward(wizardPage, this)) {
                    return;
                }

                WizardPage nextPage = wizardPage.getNext();
                if (nextPage == null) { // dialog finished
                    dispose();
                    executeFinalAction();
                } else {
                    JComponent panel = nextPage.getPanel(Wizard.this);
                    dialog.changeForm(panel);
                    dialog.setHeaderMessage(nextPage.getHeaderText(Wizard.this));
                    wizardPage = nextPage;

                    if (wizardPage.getNext() == null) { // this is the last page
                        setOKButtonText("Render");
                    }
                }
            }
        };
        dialog.setHeaderMessage(wizardPage.getHeaderText(Wizard.this));

        // it was packed already, but this is not correct because of the header message
        // and anyway we don't know the size of the filter dialogs in advance
        dialog.setSize(450, 380);

        GUIUtils.centerOnScreen(dialog);
        dialog.setVisible(true);
    }

    protected abstract boolean mayMoveForwardIfNextPressed(WizardPage wizardPage, Component dialogParent);

    protected abstract boolean mayProceedAfterMovingForward(WizardPage wizardPage, Component dialogParent);

    protected abstract void executeFinalAction();

    protected abstract void finalCleanup();
}

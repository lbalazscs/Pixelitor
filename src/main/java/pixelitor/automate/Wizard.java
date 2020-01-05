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
    private WizardPage currentPage;
    private final String dialogTitle;
    private final String finishButtonText;
    private final int initialDialogWidth;
    private final int initialDialogHeight;
    protected final Drawable dr;

    protected Wizard(WizardPage initialPage,
                     String dialogTitle,
                     String finishButtonText,
                     int initialDialogWidth,
                     int initialDialogHeight,
                     Drawable dr) {
        currentPage = initialPage;
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
                currentPage.createPanel(this, dr),
                dialogParent,
                title,
                "Next", "Cancel") {

            @Override
            protected void cancelAction() {
                currentPage.onWizardCanceled(dr);
                super.cancelAction();
            }

            @Override
            protected void okAction() {
                nextPressed(dialog);
            }
        };
        dialog.setHeaderMessage(currentPage.getHeaderText(this));

        // it is packed already, but not correctly, because of the header message
        // and anyway we don't know the size of the filter dialogs in advance
        dialog.setSize(initialDialogWidth, initialDialogHeight);

        GUIUtils.showDialog(dialog);
    }

    private void nextPressed(OKCancelDialog dialog) {
        if (!mayMoveForwardIfNextPressed(currentPage, dialog)) {
            return;
        }

        // move forward
        currentPage.onMovingToTheNext(this, dr);

        if (!mayProceedAfterMovingForward(currentPage, dialog)) {
            return;
        }

        WizardPage nextPage = currentPage.getNext();
        if (nextPage == null) { // dialog finished
            dialog.close();
            finalAction();
        } else {
            JComponent panel = nextPage.createPanel(this, dr);
            this.dialog.changeForm(panel);
            this.dialog.setHeaderMessage(nextPage.getHeaderText(this));
            currentPage = nextPage;

            if (currentPage.getNext() == null) { // this is the last page
                dialog.setOKButtonText(finishButtonText);
            }
        }
    }

    protected abstract boolean mayMoveForwardIfNextPressed(WizardPage wizardPage,
                                                           Component dialogParent);

    protected abstract boolean mayProceedAfterMovingForward(WizardPage wizardPage,
                                                            Component dialogParent);

    protected abstract void finalAction();

    protected abstract void finalCleanup();
}

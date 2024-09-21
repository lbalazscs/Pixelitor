/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import java.util.Objects;
import java.util.Optional;

/**
 * A wizard that manages navigation
 * through a sequence of {@link WizardPage}s.
 */
public abstract class Wizard {
    private OKCancelDialog dialog = null;
    private WizardPage activePage;
    private final String title;
    private final String finishButtonText;
    private final int initialWidth;
    private final int initialHeight;
    protected final Drawable dr;

    protected Wizard(WizardPage initialPage, String title, String finishButtonText, int initialWidth, int initialHeight, Drawable dr) {
        activePage = initialPage;
        this.title = title;
        this.finishButtonText = finishButtonText;
        this.initialWidth = initialWidth;
        this.initialHeight = initialHeight;
        this.dr = Objects.requireNonNull(dr);
    }

    /**
     * Show the wizard in a dialog
     */
    public void showDialog(JFrame dialogParent) {
        try {
            showDialog(dialogParent, title);
        } finally {
            performCleanup();
        }
    }

    private void showDialog(JFrame dialogParent, String title) {
        assert dialog == null; // this should be called once per object

        dialog = new OKCancelDialog(activePage.createPanel(this, dr), dialogParent, title, "Next") {
            @Override
            protected void cancelAction() {
                activePage.onWizardCanceled(dr);
                super.cancelAction();
            }

            @Override
            protected void okAction() {
                handleNextButtonPress(dialog);
            }
        };
        dialog.setHeaderMessage(activePage.getHelpText(this));

        // It's packed already, but not correctly, because of the header message,
        // and anyway we don't know the size of the filter dialogs in advance.
        dialog.setSize(initialWidth, initialHeight);
        activePage.onPageShown(dialog);
        GUIUtils.showDialog(dialog);
    }

    private void handleNextButtonPress(OKCancelDialog dialog) {
        if (!activePage.validatePage(this, dialog)) {
            return;
        }

        activePage.onComplete(this, dr);

        Optional<WizardPage> nextPage = activePage.getNextPage();
        if (nextPage.isPresent()) {
            transitionToNextPage(dialog, nextPage.get());
        } else {
            // dialog finished
            dialog.close();
            onWizardComplete();
        }
    }

    private void transitionToNextPage(OKCancelDialog dialog, WizardPage nextPage) {
        JComponent panel = nextPage.createPanel(this, dr);
        dialog.changeForm(panel);
        dialog.setHeaderMessage(nextPage.getHelpText(this));
        activePage = nextPage;

        if (activePage.isFinalPage()) {
            dialog.setOKButtonText(finishButtonText);
        }
    }

    /**
     * Called when the wizard completes successfully (the "Finish" button was clicked).
     */
    protected abstract void onWizardComplete();

    /**
     * Performs final cleanup operations when the wizard terminates.
     * Called after both successful completion and cancellation.
     */
    protected abstract void performCleanup();
}

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

import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.OKCancelDialog;
import pixelitor.layers.Drawable;

import javax.swing.*;
import java.util.Objects;
import java.util.Optional;

/**
 * A wizard that manages navigation through a sequence of {@link WizardPage}s.
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
        this.activePage = initialPage;
        this.title = title;
        this.finishButtonText = finishButtonText;
        this.initialWidth = initialWidth;
        this.initialHeight = initialHeight;
        this.dr = Objects.requireNonNull(dr);
    }

    /**
     * Starts the wizard in a modal dialog.
     */
    public void start(JFrame dialogParent) {
        // a wizard is not reusable and can be shown only once
        assert dialog == null;

        try {
            createDialog(dialogParent);
            GUIUtils.showDialog(dialog);
        } finally {
            performCleanup();
        }
    }

    private void createDialog(JFrame dialogParent) {
        dialog = new OKCancelDialog(activePage.createPanel(this, dr), dialogParent, title, "Next") {
            @Override
            protected void dialogCanceled() {
                activePage.onWizardCanceled(dr);
                super.dialogCanceled();
            }

            @Override
            protected void dialogAccepted() {
                handleNextButtonPress();
            }
        };
        dialog.setHeaderMessage(activePage.getHelpText(this));

        // it's packed already, but not correctly, because of the header message,
        // and anyway we don't know the size of the filter dialogs in advance
        dialog.setSize(initialWidth, initialHeight);
        activePage.onPageShown(this, dialog);
    }

    private void handleNextButtonPress() {
        if (!activePage.validatePage(this, dialog)) {
            return;
        }

        activePage.onComplete(this, dr);

        Optional<WizardPage> nextPage = activePage.getNextPage();
        if (nextPage.isPresent()) {
            transitionToNextPage(nextPage.get());
        } else {
            // wizard finished
            dialog.close();
            onWizardComplete();
        }
    }

    private void transitionToNextPage(WizardPage nextPage) {
        JComponent panel = nextPage.createPanel(this, dr);
        dialog.updateContent(panel);
        dialog.setHeaderMessage(nextPage.getHelpText(this));
        activePage = nextPage;

        activePage.onPageShown(this, dialog);

        if (activePage.isFinalPage()) {
            dialog.setOKButtonText(finishButtonText);
        }
    }

    /**
     * Called when the wizard completes successfully.
     */
    protected abstract void onWizardComplete();

    /**
     * Performs cleanup when the wizard terminates, either by completion or cancellation.
     */
    protected abstract void performCleanup();
}

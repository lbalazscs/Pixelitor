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

package pixelitor.automate;

import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.OKCancelDialog;
import pixelitor.layers.Drawable;

import javax.swing.*;
import java.util.Objects;
import java.util.Optional;

/**
 * A wizard. The individual pages implement
 * the {@link WizardPage} interface.
 */
public abstract class Wizard {
    private OKCancelDialog dialog = null;
    private WizardPage currentPage;
    private final String title;
    private final String finishButtonText;
    private final int initialWidth;
    private final int initialHeight;
    protected final Drawable dr;

    protected Wizard(WizardPage initialPage, String title, String finishButtonText, int initialWidth, int initialHeight, Drawable dr) {
        currentPage = initialPage;
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
            finalCleanup();
        }
    }

    private void showDialog(JFrame dialogParent, String title) {
        assert dialog == null; // this should be called once per object

        dialog = new OKCancelDialog(currentPage.createPanel(this, dr), dialogParent, title, "Next") {
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
        dialog.setHeaderMessage(currentPage.getHelpText(this));

        // It's packed already, but not correctly, because of the header message,
        // and anyway we don't know the size of the filter dialogs in advance.
        dialog.setSize(initialWidth, initialHeight);
        currentPage.onShowingInDialog(dialog);
        GUIUtils.showDialog(dialog);
    }

    private void nextPressed(OKCancelDialog dialog) {
        if (!currentPage.isValid(this, dialog)) {
            return;
        }

        currentPage.finish(this, dr);

        Optional<WizardPage> nextPage = currentPage.getNext();
        if (nextPage.isPresent()) {
            showNextPage(dialog, nextPage.get());
        } else {
            // dialog finished
            dialog.close();
            finalAction();
        }
    }

    private void showNextPage(OKCancelDialog dialog, WizardPage nextPage) {
        JComponent panel = nextPage.createPanel(this, dr);
        dialog.changeForm(panel);
        dialog.setHeaderMessage(nextPage.getHelpText(this));
        currentPage = nextPage;

        if (currentPage.isLast()) {
            dialog.setOKButtonText(finishButtonText);
        }
    }

    /**
     * Called when the wizard is finished (the "Finish" button was clicked).
     */
    protected abstract void finalAction();

    /**
     * Called when the wizard is closed, either by the user
     * clicking the "Cancel" button or by the wizard finishing.
     */
    protected abstract void finalCleanup();
}

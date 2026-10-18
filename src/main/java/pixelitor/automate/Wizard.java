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
import pixelitor.layers.Drawable;

import javax.swing.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * A wizard that manages navigation through a sequence of {@link WizardPage}s.
 */
public abstract class Wizard {
    private WizardDialog dialog = null;
    private WizardPage currentPage;
    private final Deque<WizardPage> history = new ArrayDeque<>();
    private final String title;
    private final String finishButtonText;
    private final int initialWidth;
    private final int initialHeight;
    protected final Drawable dr;
    private boolean previewing = false;

    protected Wizard(String title, String finishButtonText, int initialWidth, int initialHeight, Drawable dr) {
        this.title = title;
        this.finishButtonText = finishButtonText;
        this.initialWidth = initialWidth;
        this.initialHeight = initialHeight;
        this.dr = Objects.requireNonNull(dr);
    }

    protected abstract WizardPage createFirstPage();

    /**
     * Starts the wizard in a modal dialog.
     */
    public void start(JFrame dialogParent) {
        // a wizard instance is single-use and can be shown only once
        assert dialog == null;

        currentPage = createFirstPage();

        try {
            dialog = new WizardDialog(dialogParent, title, initialWidth, initialHeight,
                this::handleBackAction,
                this::handleNextAction,
                this::handleCancelAction);
            showPage(currentPage);
            GUIUtils.showDialog(dialog); // blocks until the dialog closes
        } finally {
            stopPreviewing();
            onWizardClosed();
        }
    }

    private void showPage(WizardPage page) {
        currentPage = page;
        JComponent panel = page.createPanel();
        dialog.setContent(panel);
        dialog.setHeaderMessage(page.getHelpText());
        currentPage.onPageShown();
        refreshButtons();
    }

    /**
     * Refreshes the text and enabled state of the wizard's navigation buttons.
     */
    public void refreshButtons() {
        String text = currentPage.hasNextPage() ? WizardDialog.NEXT_TEXT : finishButtonText;
        dialog.setNextButtonText(text);
        dialog.setNextButtonEnabled(currentPage.canProceed());
        dialog.setBackButtonEnabled(!history.isEmpty());
    }

    /**
     * Starts filter previewing on the drawable if not already active.
     */
    public void startPreviewing() {
        if (!previewing) {
            dr.startPreviewing();
            previewing = true;
        }
    }

    /**
     * Stops filter previewing on the drawable if currently active.
     */
    public void stopPreviewing() {
        if (previewing) {
            dr.stopPreviewing();
            previewing = false;
        }
    }

    private void handleNextAction() {
        if (!currentPage.validatePage()) {
            return;
        }

        currentPage.onNext();

        if (currentPage.hasNextPage()) {
            WizardPage nextPage = currentPage.createNextPage();
            currentPage.disposePanel();
            history.push(currentPage);
            showPage(nextPage);
        } else {
            // wizard finished
            currentPage.disposePanel();
            stopPreviewing();
            dialog.close();
            onWizardComplete();
        }
    }

    private void handleBackAction() {
        if (history.isEmpty()) {
            return;
        }

        currentPage.onBack();
        currentPage.disposePanel();
        WizardPage prev = history.pop();
        prev.onReturnedTo();
        showPage(prev);
    }

    private void handleCancelAction() {
        currentPage.disposePanel();
        dialog.close();
    }

    /**
     * Called when the wizard completes successfully.
     */
    protected abstract void onWizardComplete();

    /**
     * Performs cleanup when the wizard terminates, either by completion or cancellation.
     */
    protected void onWizardClosed() {
    }

    public Drawable getDrawable() {
        return dr;
    }
}

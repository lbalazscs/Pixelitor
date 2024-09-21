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

import pixelitor.gui.utils.OKCancelDialog;
import pixelitor.layers.Drawable;

import javax.swing.*;
import java.awt.Component;
import java.util.Optional;

/**
 * A single page in a {@link Wizard}.
 * Each page manages its own state, validation, and transition logic.
 */
public interface WizardPage {
    /**
     * Returns the HTML help text to be displayed for this page.
     */
    String getHelpText(Wizard wizard);

    /**
     * Determines the next page in the wizard sequence.
     */
    Optional<WizardPage> getNextPage();

    /**
     * Creates and returns the UI panel for this page.
     */
    JComponent createPanel(Wizard wizard, Drawable dr);

    /**
     * Called if the wizard was canceled while on this page.
     */
    void onWizardCanceled(Drawable dr);

    /**
     * Performs completion actions for this page before transitioning to the next.
     */
    void onComplete(Wizard wizard, Drawable dr);

    /**
     * Validates the current page state.
     */
    default boolean validatePage(Wizard wizard, Component dialogParent) {
        return true;
    }

    /**
     * Checks if this is the final page in the wizard sequence.
     */
    default boolean isFinalPage() {
        return getNextPage().isEmpty();
    }

    /**
     * Called when the page is about to be displayed in the dialog.
     * Allows for any necessary initialization or setup.
     */
    default void onPageShown(OKCancelDialog dialog) {
    }
}

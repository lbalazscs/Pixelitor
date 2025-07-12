/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
     * Creates the UI panel for this page.
     */
    JComponent createPanel(Wizard wizard, Drawable dr);

    /**
     * Returns the HTML help text for this page.
     */
    String getHelpText(Wizard wizard);

    /**
     * Returns the next page in the wizard sequence.
     */
    Optional<WizardPage> getNextPage();

    /**
     * Checks if this is the final page of the wizard.
     */
    default boolean isFinalPage() {
        return getNextPage().isEmpty();
    }

    /**
     * Performs initialization when the page is shown.
     */
    default void onPageShown(Wizard wizard, OKCancelDialog dialog) {
        // empty by default
    }

    /**
     * Validates the current page's state before proceeding.
     */
    default boolean validatePage(Wizard wizard, Component dialogParent) {
        return true;
    }

    /**
     * Performs completion actions for this page before transitioning to the next.
     */
    void onComplete(Wizard wizard, Drawable dr);

    /**
     * Called if the wizard was canceled while on this page.
     */
    void onWizardCanceled(Drawable dr);
}

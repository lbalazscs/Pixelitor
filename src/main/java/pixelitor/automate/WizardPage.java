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

import javax.swing.*;

/**
 * A single page in a {@link Wizard}.
 * Each page manages its own state, validation, and navigation logic.
 * <p>
 * Lifecycle, as driven by the wizard:
 * <ul>
 *   <li>Shown: {@link #createPanel()} → {@link #onPageShown()}; the buttons are then
 *       refreshed using {@link #hasNextPage()} and {@link #canProceed()}.
 *   <li>Next: {@link #validatePage()} → {@link #onNext()} → ({@link #hasNextPage()} ?
 *       {@link #createNextPage()}) → {@link #disposePanel()}.
 *   <li>Back: {@link #onBack()} → {@link #disposePanel()} → previous page's
 *       {@link #onReturnedTo()} → {@link #createPanel()} on the previous page.
 *   <li>Cancel: {@link #disposePanel()}.
 * </ul>
 */
public interface WizardPage {
    /**
     * Creates the UI panel for this page.
     * <p>
     * Can be called more than once on the same instance (once per display, such as
     * when the page is first shown and whenever returned to via Back navigation).
     * Implementations may return a freshly created panel or a cached panel.
     */
    JComponent createPanel();

    /**
     * Returns the HTML help text for this page.
     */
    String getHelpText();

    /**
     * Performs initialization when the page is displayed in the wizard dialog.
     */
    default void onPageShown() {
        // empty by default
    }

    /**
     * Checks if this page has a next page in the wizard sequence.
     */
    boolean hasNextPage();

    /**
     * Creates the next page in the wizard sequence.
     * Called only if {@link #hasNextPage()} returned true.
     */
    default WizardPage createNextPage() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns whether the Next/Finish button should be enabled.
     */
    default boolean canProceed() {
        return true;
    }

    /**
     * Validates the current page's state before allowing navigation to the next page.
     */
    default boolean validatePage() {
        return true;
    }

    /**
     * Performs completion actions for this page before
     * transitioning to the next page or completing the wizard.
     */
    void onNext();

    /**
     * Called on the current page when the user presses Back, before navigating away.
     */
    default void onBack() {
        // empty by default
    }

    /**
     * Called on a page that is re-displayed because the
     * user pressed Back on the following page.
     * <p>
     * The page should undo the effects of its own earlier
     * {@link #onNext()} call (e.g. downstream configurations or model state).
     */
    default void onReturnedTo() {
        // empty by default
    }

    /**
     * Releases resources held by the page's current UI panel.
     * Called each time the page is left in either direction,
     * and when the wizard finishes or is canceled.
     */
    default void disposePanel() {
    }
}

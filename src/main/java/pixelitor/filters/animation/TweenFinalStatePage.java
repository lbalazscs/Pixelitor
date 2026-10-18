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

package pixelitor.filters.animation;

import pixelitor.automate.WizardPage;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.gui.utils.Themes;

import javax.swing.*;
import java.util.Objects;

/**
 * The third page in the {@link TweenWizard}, where
 * the user configures the final state of the filter.
 */
public class TweenFinalStatePage implements WizardPage {
    private final TweenWizard wizard;
    private FilterGUI filterGUI;

    public TweenFinalStatePage(TweenWizard wizard) {
        this.wizard = Objects.requireNonNull(wizard);
    }

    @Override
    public JComponent createPanel() {
        filterGUI = wizard.getFilter().createGUI(wizard.getDrawable(), false);
        return filterGUI;
    }

    @Override
    public String getHelpText() {
        String color = Themes.getActive().isDark() ? "#5DCF6E" : "blue";
        String text = "<b><font color=" + color + " size=+1>Final</font></b> settings for the <i>"
            + wizard.getFilter().getName() + "</i> filter.";

        if (wizard.getFilter().getParamSet().hasGradient()) {
            text += "<br>Note: Only modify gradient stop colors and positions, not the number of stops.";
        }
        return text;
    }

    @Override
    public void onPageShown() {
        // The re-run is not needed because the wizard keeps the preview alive
        // and it already shows the state this page expects, both when arriving
        // from the Initial page and when returning from the Settings page
        // (model values and end-state mode are untouched by the Settings page).

        // TODO: if "Show Original" is ticked on this page,
        //   then after a next-then-back sequence, this page
        //   shows the original image with an unchecked checkbox
    }

    @Override
    public boolean hasNextPage() {
        return true;
    }

    @Override
    public WizardPage createNextPage() {
        return new TweenOutputSettingsPage(wizard);
    }

    @Override
    public void onNext() {
        wizard.getAnimation().captureFinalState();
    }

    @Override
    public void disposePanel() {
        if (filterGUI != null) {
            filterGUI.dispose();
            filterGUI = null;
        }
    }
}

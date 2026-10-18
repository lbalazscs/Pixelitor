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
 * The second page in the {@link TweenWizard}, where
 * the user configures the initial state of the filter.
 */
public class TweenInitialStatePage implements WizardPage {
    private final TweenWizard wizard;
    private FilterGUI filterGUI;
    private boolean returning;

    public TweenInitialStatePage(TweenWizard wizard) {
        this.wizard = Objects.requireNonNull(wizard);
    }

    @Override
    public JComponent createPanel() {
        wizard.startPreviewing();
        // the panel is created, but the filter is not executed yet
        filterGUI = wizard.getFilter().createGUI(wizard.getDrawable(), !returning);
        returning = false;
        return filterGUI;
    }

    @Override
    public String getHelpText() {
        String color = Themes.getActive().isDark() ? "#76ABFF" : "blue";
        return "<b><font color=" + color + " size=+1>Initial</font></b> settings for the <i>"
            + wizard.getFilter().getName() + "</i> filter.";
    }

    @Override
    public void onPageShown() {
        // On first display it creates the first preview;
        // when returning from the Final page the canvas still shows
        // the FINAL-state preview, so the filter must be re-run with
        // the restored initial state.
        wizard.getFilter().getParamSet().runFilter();
    }

    @Override
    public boolean hasNextPage() {
        return true;
    }

    @Override
    public WizardPage createNextPage() {
        return new TweenFinalStatePage(wizard);
    }

    @Override
    public void onNext() {
        wizard.getAnimation().captureInitialState();
        wizard.getFilter().getParamSet().setAnimationEndStateMode(true);
    }

    @Override
    public void onBack() {
        wizard.stopPreviewing();
    }

    @Override
    public void onReturnedTo() {
        returning = true;
        wizard.getFilter().getParamSet().setAnimationEndStateMode(false);
        wizard.getAnimation().restoreInitialState();
    }

    @Override
    public void disposePanel() {
        if (filterGUI != null) {
            filterGUI.dispose();
            filterGUI = null;
        }
    }
}

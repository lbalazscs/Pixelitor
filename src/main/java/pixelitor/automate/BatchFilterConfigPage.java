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

import pixelitor.filters.Filter;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.FilterWithGUI;

import javax.swing.*;
import java.util.Objects;

/**
 * The second page in the {@link BatchFilterWizard}, allowing
 * the user to configure settings for the selected filter.
 */
public class BatchFilterConfigPage implements WizardPage {
    private final BatchFilterWizard wizard;
    private FilterGUI filterGUI;

    public BatchFilterConfigPage(BatchFilterWizard wizard) {
        this.wizard = Objects.requireNonNull(wizard);
    }

    @Override
    public JComponent createPanel() {
        Filter filter = wizard.getFilter();

        // this page is shown only if the selected filter has a GUI
        FilterWithGUI guiFilter = (FilterWithGUI) filter;

        wizard.startPreviewing();

        filterGUI = guiFilter.createGUI(wizard.getDrawable(), true);
        return filterGUI;
    }

    @Override
    public String getHelpText() {
        return "Configure filter settings.";
    }

    @Override
    public void onPageShown() {
        filterGUI.startPreview(true);
    }

    @Override
    public boolean hasNextPage() {
        return false;
    }

    @Override
    public void onNext() {
    }

    @Override
    public void onBack() {
        wizard.stopPreviewing();
    }

    @Override
    public void disposePanel() {
        if (filterGUI != null) {
            filterGUI.dispose();
            filterGUI = null;
        }
    }
}

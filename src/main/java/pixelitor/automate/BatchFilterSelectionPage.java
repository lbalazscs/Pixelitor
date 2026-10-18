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

import org.jdesktop.swingx.VerticalLayout;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.util.FilterSearchPanel;
import pixelitor.filters.util.Filters;
import pixelitor.gui.utils.ValidationResult;

import javax.swing.*;
import java.util.Objects;

/**
 * The first page in the {@link BatchFilterWizard}, allowing the
 * user to select a filter and input/output directories.
 */
public class BatchFilterSelectionPage implements WizardPage {
    private final BatchFilterWizard wizard;
    private OpenSaveDirsPanel openSaveDirsPanel;
    private FilterSearchPanel filterSelector;

    public BatchFilterSelectionPage(BatchFilterWizard wizard) {
        this.wizard = Objects.requireNonNull(wizard);
    }

    @Override
    public JComponent createPanel() {
        filterSelector = new FilterSearchPanel(Filters.getAllFilters());
        filterSelector.setBorder(BorderFactory.createTitledBorder("Select Filter"));

        var mainPanel = new JPanel(new VerticalLayout());
        mainPanel.add(filterSelector);
        openSaveDirsPanel = new OpenSaveDirsPanel();
        mainPanel.add(openSaveDirsPanel);

        return mainPanel;
    }

    @Override
    public String getHelpText() {
        return "Apply a filter to every image in a folder.";
    }

    @Override
    public void onPageShown() {
        filterSelector.addSelectionListener(_ -> wizard.refreshButtons());
    }

    @Override
    public boolean hasNextPage() {
        var selectedAction = filterSelector.getSelectedFilter();
        if (selectedAction == null) {
            // so the button reads "Next" rather than "Start Processing"
            return true;
        }
        return selectedAction.getFilter() instanceof FilterWithGUI;
    }

    @Override
    public WizardPage createNextPage() {
        return new BatchFilterConfigPage(wizard);
    }

    @Override
    public boolean validatePage() {
        ValidationResult combinedResult = filterSelector.validateSettings()
            .and(openSaveDirsPanel.validateSettings());

        if (!combinedResult.isValid()) {
            combinedResult.showErrorDialog(filterSelector);
            return false; // keeps user on the current wizard page
        }

        return true;
    }

    @Override
    public void onNext() {
        var selectedAction = filterSelector.getSelectedFilter();
        var filter = selectedAction.getFilter();

        wizard.setFilter(filter);

        openSaveDirsPanel.rememberSettings();
    }
}

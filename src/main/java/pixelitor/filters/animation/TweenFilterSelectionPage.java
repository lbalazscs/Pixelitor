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
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.util.FilterAction;
import pixelitor.filters.util.FilterSearchPanel;
import pixelitor.filters.util.Filters;

import javax.swing.*;
import java.util.Objects;

/**
 * The first page in the {@link TweenWizard}, where
 * the user selects the filter to animate.
 */
public class TweenFilterSelectionPage implements WizardPage {
    private final TweenWizard wizard;
    private FilterSearchPanel filterSelector;

    public TweenFilterSelectionPage(TweenWizard wizard) {
        this.wizard = Objects.requireNonNull(wizard);
    }

    @Override
    public JComponent createPanel() {
        filterSelector = new FilterSearchPanel(Filters.getAnimationFilters());
        return filterSelector;
    }

    @Override
    public String getHelpText() {
        return "Select a filter to create your tweening animation. The animation " +
            "will interpolate between two states of the selected filter.";
    }

    @Override
    public void onPageShown() {
        filterSelector.addSelectionListener(_ -> wizard.refreshButtons());
    }

    @Override
    public boolean hasNextPage() {
        return true;
    }

    @Override
    public WizardPage createNextPage() {
        return new TweenInitialStatePage(wizard);
    }

    @Override
    public boolean canProceed() {
        return filterSelector != null && filterSelector.hasSelection();
    }

    @Override
    public void onNext() {
        FilterAction selectedItem = filterSelector.getSelectedFilter();
        // animatable filters are always parametrized
        ParametrizedFilter filter = (ParametrizedFilter) selectedItem.getFilter();
        wizard.getAnimation().setFilter(filter);
    }
}

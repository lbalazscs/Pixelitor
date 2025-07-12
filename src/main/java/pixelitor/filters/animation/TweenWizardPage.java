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

package pixelitor.filters.animation;

import pixelitor.automate.Wizard;
import pixelitor.automate.WizardPage;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.util.FilterAction;
import pixelitor.filters.util.FilterSearchPanel;
import pixelitor.filters.util.Filters;
import pixelitor.gui.utils.OKCancelDialog;
import pixelitor.gui.utils.Themes;
import pixelitor.gui.utils.ValidationResult;
import pixelitor.layers.Drawable;

import javax.swing.*;
import java.awt.Component;
import java.util.Optional;

/**
 * A page in the tweening animation wizard.
 */
public enum TweenWizardPage implements WizardPage {
    FILTER_SELECTION {
        private FilterSearchPanel filterSelector;

        @Override
        public String getHelpText(Wizard wizard) {
            return "<html>Select a filter to create your tweening animation. The animation " +
                "will interpolate between two states of the selected filter.";
        }

        @Override
        public Optional<WizardPage> getNextPage() {
            return Optional.of(STARTING_FILTER_STATE);
        }

        @Override
        public JComponent createPanel(Wizard wizard, Drawable dr) {
            filterSelector = new FilterSearchPanel(Filters.getAnimationFilters());
            return filterSelector;
        }

        @Override
        public void onPageShown(Wizard wizard, OKCancelDialog dialog) {
            JButton okButton = dialog.getOkButton();

            okButton.setEnabled(false);
            filterSelector.addSelectionListener(e ->
                okButton.setEnabled(filterSelector.hasSelection()));
        }

        @Override
        public void onWizardCanceled(Drawable dr) {
            // no cleanup needed
        }

        @Override
        public void onComplete(Wizard wizard, Drawable dr) {
            FilterAction selectedItem = filterSelector.getSelectedFilter();
            ParametrizedFilter filter = (ParametrizedFilter) selectedItem.getFilter();
            getAnimation(wizard).setFilter(filter);
        }
    }, STARTING_FILTER_STATE {
        @Override
        public String getHelpText(Wizard wizard) {
            String color = Themes.getActive().isDark() ? "#76ABFF" : "blue";
            return "<html><b><font color=" + color + " size=+1>Initial</font></b> settings for the <i>"
                + getFilter(wizard).getName() + "</i> filter.";
        }

        @Override
        public Optional<WizardPage> getNextPage() {
            return Optional.of(ENDING_FILTER_STATE);
        }

        @Override
        public JComponent createPanel(Wizard wizard, Drawable dr) {
            dr.startPreviewing();
            // the panel is created, but the filter is not run yet
            return getFilter(wizard).createGUI(dr, true);
        }

        @Override
        public void onPageShown(Wizard wizard, OKCancelDialog dialog) {
            getFilter(wizard).getParamSet().runFilter();
        }

        @Override
        public void onWizardCanceled(Drawable dr) {
            dr.onFilterDialogCanceled();
        }

        @Override
        public void onComplete(Wizard wizard, Drawable dr) {
            getAnimation(wizard).captureInitialState();
            getFilter(wizard).getParamSet().setFinalAnimationMode(true);
        }
    }, ENDING_FILTER_STATE {
        @Override
        public String getHelpText(Wizard wizard) {
            String color = Themes.getActive().isDark() ? "#5DCF6E" : "blue";
            String text = "<html><b><font color=" + color + " size=+1>Final</font></b> settings for the <i>"
                + getFilter(wizard).getName() + "</i> filter.";

            if (getFilter(wizard).getParamSet().hasGradient()) {
                text += "<br>Note: Only modify gradient thumb colors and positions, not the number of thumbs.";
            }
            return text;
        }

        @Override
        public Optional<WizardPage> getNextPage() {
            return Optional.of(ANIMATION_SETTINGS);
        }

        @Override
        public JComponent createPanel(Wizard wizard, Drawable dr) {
            return getFilter(wizard).createGUI(dr, false);
        }

        @Override
        public void onPageShown(Wizard wizard, OKCancelDialog dialog) {
            // do nothing: there's no need to re-run the filter,
            // because we have a correct preview from the previous page
        }

        @Override
        public void onWizardCanceled(Drawable dr) {
            dr.onFilterDialogCanceled();
        }

        @Override
        public void onComplete(Wizard wizard, Drawable dr) {
            // cancel the previewing
            dr.onFilterDialogCanceled();

            // save the final state
            getAnimation(wizard).captureFinalState();
        }
    }, ANIMATION_SETTINGS {
        TweenOutputSettingsPanel outputSettingsPanel;

        @Override
        public String getHelpText(Wizard wizard) {
            return "<html><b>Animation Output Settings</b>" +
                "<br>Choose how to save your animation:<ul>" +
                "<li>For file sequences: Select an existing folder" +
                "<li>For single file: Choose a new or existing file in an existing folder";
        }

        @Override
        public Optional<WizardPage> getNextPage() {
            return Optional.empty();
        }

        @Override
        public JComponent createPanel(Wizard wizard, Drawable dr) {
            if (outputSettingsPanel == null) {
                // cache the panel to preserve output settings between page views
                outputSettingsPanel = new TweenOutputSettingsPanel();
            }
            return outputSettingsPanel;
        }

        @Override
        public void onWizardCanceled(Drawable dr) {
            // No cleanup needed
        }

        @Override
        public boolean validatePage(Wizard wizard, Component dialogParent) {
            ValidationResult validity = outputSettingsPanel.validateSettings();
            if (!validity.isValid()) {
                validity.showErrorDialog(dialogParent);
                return false;
            }

            TweenAnimation animation = getAnimation(wizard);
            outputSettingsPanel.configure(animation);

            return animation.checkOverwrite(dialogParent);
        }

        @Override
        public void onComplete(Wizard wizard, Drawable dr) {
            // the settings were already saved during validation
        }
    };

    private static TweenAnimation getAnimation(Wizard wizard) {
        return ((TweenWizard) wizard).getAnimation();
    }

    private static ParametrizedFilter getFilter(Wizard wizard) {
        return getAnimation(wizard).getFilter();
    }
}

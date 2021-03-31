/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.util.FilterUtils;
import pixelitor.gui.utils.OKCancelDialog;
import pixelitor.gui.utils.ValidationResult;
import pixelitor.layers.Drawable;

import javax.swing.*;
import java.awt.Component;
import java.util.Optional;

/**
 * A page in the tweening animation wizard
 */
public enum TweenWizardPage implements WizardPage {
    SELECT_FILTER {
        private FilterSearchPanel searchPanel;

        @Override
        public String getHelpText(Wizard wizard) {
            return "<html> Create a tweening animation based on the settings of the selected filter.";
        }

        @Override
        public Optional<WizardPage> getNext() {
            return Optional.of(INITIAL_FILTER_SETTINGS);
        }

        @Override
        public JComponent createPanel(Wizard wizard, Drawable dr) {
            searchPanel = new FilterSearchPanel(FilterUtils.getAnimationFiltersSorted());
            return searchPanel;
        }

        @Override
        public void onShowingInDialog(OKCancelDialog dialog) {
            JButton okButton = dialog.getOkButton();

            okButton.setEnabled(false);
            searchPanel.addSelectionListener(e ->
                okButton.setEnabled(searchPanel.hasSelection()));
        }

        @Override
        public void onWizardCanceled(Drawable dr) {
        }

        @Override
        public void finish(Wizard wizard, Drawable dr) {
            FilterAction selectedItem = searchPanel.getSelectedFilter();
            ParametrizedFilter filter = (ParametrizedFilter) selectedItem.getFilter();
            getAnimation(wizard).setFilter(filter);
        }
    }, INITIAL_FILTER_SETTINGS {
        @Override
        public String getHelpText(Wizard wizard) {
            return "<html> Select the <b><font color=blue size=+1>initial</font></b> settings for the <i>"
                + getFilter(wizard).getName() + "</i> filter.";
        }

        @Override
        public Optional<WizardPage> getNext() {
            return Optional.of(FINAL_FILTER_SETTINGS);
        }

        @Override
        public JComponent createPanel(Wizard wizard, Drawable dr) {
            ParametrizedFilter filter = getFilter(wizard);
            dr.startPreviewing();

            return filter.createGUI(dr, true);
        }

        @Override
        public void onWizardCanceled(Drawable dr) {
            dr.onFilterDialogCanceled();
        }

        @Override
        public void finish(Wizard wizard, Drawable dr) {
            getAnimation(wizard).copyInitialStateFromCurrent();
            getFilter(wizard).getParamSet().setFinalAnimationSettingMode(true);
        }
    }, FINAL_FILTER_SETTINGS {
        @Override
        public String getHelpText(Wizard wizard) {
            String text = "<html> Select the <b><font color=green size=+1>final</font></b> settings for the <i>"
                + getFilter(wizard).getName() + "</i> filter.";
            boolean hasGradient = getFilter(wizard).getParamSet().hasGradient();
            if (hasGradient) {
                text += "<br>Don't change the number of thumbs for the gradient, only their color or position.";
            }
            return text;
        }

        @Override
        public Optional<WizardPage> getNext() {
            return Optional.of(OUTPUT_SETTINGS);
        }

        @Override
        public JComponent createPanel(Wizard wizard, Drawable dr) {
            // the following 3 lines are necessary because otherwise the image position
            // selectors will show the result of the initial filter and not the original image
            dr.stopPreviewing(); // stop the initial one
            dr.startPreviewing(); // start the final one

            return getFilter(wizard).createGUI(dr, false);
        }

        @Override
        public void onWizardCanceled(Drawable dr) {
            dr.onFilterDialogCanceled();
        }

        @Override
        public void finish(Wizard wizard, Drawable dr) {
            // cancel the previewing
            onWizardCanceled(dr);

            // save the final state
            getAnimation(wizard).copyFinalStateFromCurrent();
        }
    }, OUTPUT_SETTINGS {
        TweenOutputSettingsPanel outputSettingsPanel;

        @Override
        public String getHelpText(Wizard wizard) {
            return "<html> <b>Output settings</b>" +
                "<p>For file sequence output select an existing folder." +
                "<br>For file output select a new or existing file in an existing folder.";
        }

        @Override
        public Optional<WizardPage> getNext() {
            return Optional.empty();
        }

        @Override
        public JComponent createPanel(Wizard wizard, Drawable dr) {
            if (outputSettingsPanel == null) {
                // keeps the output settings by reusing the panel
                outputSettingsPanel = new TweenOutputSettingsPanel();
            }
            return outputSettingsPanel;
        }

        @Override
        public void onWizardCanceled(Drawable dr) {

        }

        @Override
        public boolean isValid(Wizard wizard, Component dialogParent) {
            ValidationResult validity = outputSettingsPanel.checkValidity();
            if (!validity.isOK()) {
                validity.showErrorDialog(dialogParent);
                return false;
            }

            TweenAnimation animation = getAnimation(wizard);
            outputSettingsPanel.copySettingsInto(animation);

            return animation.checkOverwrite(dialogParent);
        }

        @Override
        public void finish(Wizard wizard, Drawable dr) {
            // the settings were already saved while validating
        }
    };

    private static TweenAnimation getAnimation(Wizard wizard) {
        return ((TweenWizard) wizard).getAnimation();
    }

    private static ParametrizedFilter getFilter(Wizard wizard) {
        return getAnimation(wizard).getFilter();
    }
}

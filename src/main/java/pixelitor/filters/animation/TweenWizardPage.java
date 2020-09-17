/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.FilterAction;
import pixelitor.filters.FilterUtils;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.ParametrizedFilterGUI;
import pixelitor.gui.utils.ValidationResult;
import pixelitor.layers.Drawable;

import javax.swing.*;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.Optional;

/**
 * A page in the tweening animation wizard
 */
public enum TweenWizardPage implements WizardPage {
    SELECT_FILTER {
        JComboBox<FilterAction> filtersCB;

        @Override
        public String getHelpText(Wizard wizard) {
            return "<html> Create a tweening animation based on the settings of a filter.";
        }

        @Override
        public Optional<WizardPage> getNext() {
            return Optional.of(INITIAL_FILTER_SETTINGS);
        }

        @Override
        public JComponent createPanel(Wizard wizard, Drawable dr) {
            var p = new JPanel(new FlowLayout());
            p.add(new JLabel("Select Filter:"));
            filtersCB = new JComboBox<>(FilterUtils.getAnimationFiltersSorted());
            p.add(filtersCB);
            return p;
        }

        @Override
        public void onWizardCanceled(Drawable dr) {
        }

        @Override
        public void finish(Wizard wizard, Drawable dr) {
            FilterAction selectedItem = (FilterAction) filtersCB.getSelectedItem();
            ParametrizedFilter filter = (ParametrizedFilter) selectedItem.getFilter();
            getAnimation(wizard).setFilter(filter);
        }
    }, INITIAL_FILTER_SETTINGS {
        @Override
        public String getHelpText(Wizard wizard) {
            return "<html> Select the <b><font color=blue size=+1>initial</font></b> settings for the filter";
        }

        @Override
        public Optional<WizardPage> getNext() {
            return Optional.of(FINAL_FILTER_SETTINGS);
        }

        @Override
        public JComponent createPanel(Wizard wizard, Drawable dr) {
            ParametrizedFilter filter = getAnimation(wizard).getFilter();
            dr.startPreviewing();

            return filter.createGUI(dr);
        }

        @Override
        public void onWizardCanceled(Drawable dr) {
            dr.onFilterDialogCanceled();
        }

        @Override
        public void finish(Wizard wizard, Drawable dr) {
            getAnimation(wizard).copyInitialStateFromCurrent();

            ParametrizedFilterGUI.setResetParams(false);
            getAnimation(wizard).getFilter().getParamSet().setFinalAnimationSettingMode(true);
        }
    }, FINAL_FILTER_SETTINGS {
        @Override
        public String getHelpText(Wizard wizard) {
            String text = "<html> Select the <b><font color=green size=+1>final</font></b> settings for the filter.";
            boolean hasGradient = getAnimation(wizard).getFilter().getParamSet().hasGradient();
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

            ParametrizedFilter filter = getAnimation(wizard).getFilter();

            return filter.createGUI(dr);
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

            if (!animation.checkOverwrite(dialogParent)) {
                return false;
            }

            return true;
        }

        @Override
        public void finish(Wizard wizard, Drawable dr) {
            // the settings were already saved while validating
        }
    };

    private static TweenAnimation getAnimation(Wizard wizard) {
        // all wizards here can be casted to TweenWizard
        return ((TweenWizard) wizard).getAnimation();
    }
}

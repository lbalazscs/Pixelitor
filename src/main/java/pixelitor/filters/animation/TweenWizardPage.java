/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.AdjustPanel;
import pixelitor.filters.gui.ParametrizedAdjustPanel;
import pixelitor.layers.ImageLayer;

import javax.swing.*;
import java.awt.FlowLayout;

/**
 * The pages of the tweening animation export dialog
 */
public enum TweenWizardPage implements WizardPage {
    SELECT_FILTER {
        JComboBox<FilterAction> filtersCB;

        @Override
        public String getHeaderText(Wizard wizard) {
            return "<html> Here you can create a tweening animation <br>based on the settings of a filter.";
        }

        @Override
        public WizardPage getNext() {
            return INITIAL_FILTER_SETTINGS;
        }

        @Override
        public JComponent getPanel(Wizard wizard, ImageLayer layer) {
            JPanel p = new JPanel(new FlowLayout());
            p.add(new JLabel("Select Filter:"));
            filtersCB = new JComboBox<>(FilterUtils.getAnimationFiltersSorted());
            p.add(filtersCB);
            return p;
        }

        @Override
        public void onWizardCancelled(ImageLayer layer) {
        }

        @Override
        public void onMovingToTheNext(Wizard wizard, ImageLayer layer) {
            FilterAction selectedItem = (FilterAction) filtersCB.getSelectedItem();
            FilterWithParametrizedGUI filter = (FilterWithParametrizedGUI) selectedItem.getFilter();
            getAnimation(wizard).setFilter(filter);
        }
    }, INITIAL_FILTER_SETTINGS {
        @Override
        public String getHeaderText(Wizard wizard) {
            return "<html> Select the <b><font color=blue size=+1>initial</font></b> settings for the filter";
        }

        @Override
        public WizardPage getNext() {
            return FINAL_FILTER_SETTINGS;
        }

        @Override
        public JComponent getPanel(Wizard wizard, ImageLayer layer) {
            FilterWithParametrizedGUI filter = getAnimation(wizard).getFilter();
            layer.startPreviewing();
            AdjustPanel adjustPanel = filter.createAdjustPanel(layer);

            return adjustPanel;
        }

        @Override
        public void onWizardCancelled(ImageLayer layer) {
            layer.cancelPressedInDialog();
        }

        @Override
        public void onMovingToTheNext(Wizard wizard, ImageLayer layer) {
            getAnimation(wizard).copyInitialStateFromCurrent();

            ParametrizedAdjustPanel.setResetParams(false);
            getAnimation(wizard).getFilter().getParamSet().setFinalAnimationSettingMode(true);
        }
    }, FINAL_FILTER_SETTINGS {
        @Override
        public String getHeaderText(Wizard wizard) {
            String text = "<html> Select the <b><font color=green size=+1>final</font></b> settings for the filter.";
            boolean hasGradient = getAnimation(wizard).getFilter().getParamSet().hasGradient();
            if (hasGradient) {
                text += "<br>Do not change the number of thumbs for the gradient, only their color or position.";
            }
            return text;
        }

        @Override
        public WizardPage getNext() {
            return OUTPUT_SETTINGS;
        }

        @Override
        public JComponent getPanel(Wizard wizard, ImageLayer layer) {
            // the following 3 lines are necessary because otherwise the image position
            // selectors will show the result of the initial filter and not the original image
            layer.stopPreviewing(); // stop the initial one
            layer.startPreviewing(); // start the final one

            FilterWithParametrizedGUI filter = getAnimation(wizard).getFilter();
            AdjustPanel adjustPanel = filter.createAdjustPanel(layer);

            return adjustPanel;
        }

        @Override
        public void onWizardCancelled(ImageLayer layer) {
            layer.cancelPressedInDialog();
        }

        @Override
        public void onMovingToTheNext(Wizard wizard, ImageLayer layer) {
            // cancel the previewing
            onWizardCancelled(layer);

            // save final state
            getAnimation(wizard).copyFinalStateFromCurrent();
        }
    }, OUTPUT_SETTINGS {
        OutputSettingsPanel outputSettingsPanel;

        @Override
        public String getHeaderText(Wizard wizard) {
            return "<html> <b>Output settings</b>" +
                    "<p>For file sequence output select an existing folder." +
                    "<br>For file output select a new or existing file in an existing folder.";
        }

        @Override
        public WizardPage getNext() {
            return null;
        }

        @Override
        public JComponent getPanel(Wizard wizard, ImageLayer layer) {
            if (outputSettingsPanel == null) {
                outputSettingsPanel = new OutputSettingsPanel();
            }
            return outputSettingsPanel;
        }

        @Override
        public void onWizardCancelled(ImageLayer layer) {

        }


        @Override
        public void onMovingToTheNext(Wizard wizard, ImageLayer layer) {
            TweenAnimation animation = getAnimation(wizard);
            outputSettingsPanel.copySettingsInto(animation);
        }

    };

    private static TweenAnimation getAnimation(Wizard wizard) {
        // all wizards here can be casted to TweenWizard
        return ((TweenWizard) wizard).getAnimation();
    }

}

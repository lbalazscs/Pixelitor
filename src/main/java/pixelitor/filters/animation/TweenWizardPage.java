/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import pixelitor.ImageComponents;
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
public enum TweenWizardPage {
    SELECT_FILTER {
        JComboBox<FilterWithParametrizedGUI> filtersCB;

        @Override
        String getHeaderText(TweenWizard wizard) {
            return "<html> Here you can create a tweening animation <br>based on the settings of a filter.";
        }

        @Override
        TweenWizardPage getNext() {
            return INITIAL_FILTER_SETTINGS;
        }

        @Override
        JComponent getPanel(TweenWizard wizard) {
            JPanel p = new JPanel(new FlowLayout());
            p.add(new JLabel("Select Filter:"));
            filtersCB = new JComboBox<>(FilterUtils.getAnimationFiltersSorted());
            p.add(filtersCB);
            return p;
        }

        @Override
        void onWizardCancelled(TweenWizard wizard) {
        }

        @Override
        void onMovingToTheNext(TweenWizard wizard) {
            FilterWithParametrizedGUI filter = (FilterWithParametrizedGUI) filtersCB.getSelectedItem();
            wizard.getAnimation().setFilter(filter);
        }
    }, INITIAL_FILTER_SETTINGS {
        @Override
        String getHeaderText(TweenWizard wizard) {
            return "<html> Select the <b><font color=blue size=+1>initial</font></b> settings for the filter";
        }

        @Override
        TweenWizardPage getNext() {
            return FINAL_FILTER_SETTINGS;
        }

        @Override
        JComponent getPanel(TweenWizard wizard) {
            FilterWithParametrizedGUI filter = wizard.getAnimation().getFilter();
            ImageComponents.getActiveImageLayer().get().startPreviewing();
            AdjustPanel adjustPanel = filter.createAdjustPanel();
            filter.startDialogSession();

            return adjustPanel;
        }

        @Override
        void onWizardCancelled(TweenWizard wizard) {
            FilterWithParametrizedGUI filter = wizard.getAnimation().getFilter();
            ImageComponents.getActiveImageLayer().get().cancelPressedInDialog();
            filter.endDialogSession();
        }

        @Override
        void onMovingToTheNext(TweenWizard wizard) {
            wizard.getAnimation().copyInitialStateFromCurrent();

            ParametrizedAdjustPanel.setResetParams(false);
            wizard.getAnimation().getFilter().getParamSet().setFinalAnimationSettingMode(true);
        }
    }, FINAL_FILTER_SETTINGS {
        @Override
        String getHeaderText(TweenWizard wizard) {
            String text = "<html> Select the <b><font color=green size=+1>final</font></b> settings for the filter.";
            boolean hasGradient = wizard.getAnimation().getFilter().getParamSet().hasGradient();
            if (hasGradient) {
                text += "<br>Do not change the number of thumbs for the gradient, only their color or position.";
            }
            return text;
        }

        @Override
        TweenWizardPage getNext() {
            return OUTPUT_SETTINGS;
        }

        @Override
        JComponent getPanel(TweenWizard wizard) {
            // the following 3 lines are necessary because otherwise the image position
            // selectors will show the result of the initial filter and not the original image
            ImageLayer imageLayer = ImageComponents.getActiveImageLayer().get();
            imageLayer.cancelPressedInDialog(); // cancel the initial one
            imageLayer.startPreviewing(); // start the final one

            FilterWithParametrizedGUI filter = wizard.getAnimation().getFilter();
            AdjustPanel adjustPanel = filter.createAdjustPanel();

            return adjustPanel;
        }

        @Override
        void onWizardCancelled(TweenWizard wizard) {
            FilterWithParametrizedGUI filter = wizard.getAnimation().getFilter();
            ImageComponents.getActiveImageLayer().get().cancelPressedInDialog();
            filter.endDialogSession();
        }

        @Override
        void onMovingToTheNext(TweenWizard wizard) {
            // cancel the previewing
            onWizardCancelled(wizard);

            // save final state
            wizard.getAnimation().copyFinalStateFromCurrent();
        }
    }, OUTPUT_SETTINGS {
        OutputSettingsPanel outputSettingsPanel;

        @Override
        String getHeaderText(TweenWizard wizard) {
            return "<html> <b>Output settings</b>" +
                    "<p>For file sequence output select an existing folder." +
                    "<br>For file output select a new or existing file in an existing folder.";
        }

        @Override
        TweenWizardPage getNext() {
            return null;
        }

        @Override
        JComponent getPanel(TweenWizard wizard) {
            if (outputSettingsPanel == null) {
                outputSettingsPanel = new OutputSettingsPanel();
            }
            return outputSettingsPanel;
        }

        @Override
        void onWizardCancelled(TweenWizard wizard) {

        }


        @Override
        void onMovingToTheNext(TweenWizard wizard) {
            TweenAnimation animation = wizard.getAnimation();
            outputSettingsPanel.copySettingsInto(animation);
        }

    };

    abstract String getHeaderText(TweenWizard wizard);

    abstract TweenWizardPage getNext();

    abstract JComponent getPanel(TweenWizard wizard);

    /**
     * Called if the wizard was cancelled while in this state
     */
    abstract void onWizardCancelled(TweenWizard wizard);

    /**
     * Called if next was pressed while in this state before moving to the next
     */
    abstract void onMovingToTheNext(TweenWizard wizard);
}

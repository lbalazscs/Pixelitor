/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters.animation;

import pixelitor.ImageComponents;
import pixelitor.filters.FilterUtils;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.AdjustPanel;
import pixelitor.filters.gui.ParametrizedAdjustPanel;

import javax.swing.*;
import java.awt.FlowLayout;
import java.io.File;

/**
 * The states of the keyframe animation export dialog
 */
public enum TweenWizardState {
    SELECT_FILTER {
        JComboBox<FilterWithParametrizedGUI> filtersCB;

        @Override
        String getHeaderText() {
            return "<html> Here you can create a basic keyframe animation <br>based on the settings of a filter.";
        }

        @Override
        TweenWizardState getNext() {
            return INITIAL_FILTER_SETTINGS;
        }

        @Override
        JComponent getPanel(final TweenWizard wizard) {
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
        String getHeaderText() {
            return "<html> Select the <b><font color=blue size=+1>initial</font></b> settings for the filter";
        }

        @Override
        TweenWizardState getNext() {
            return FINAL_FILTER_SETTINGS;
        }

        @Override
        JComponent getPanel(TweenWizard wizard) {
            FilterWithParametrizedGUI filter = wizard.getAnimation().getFilter();
            ImageComponents.getActiveComp().getActiveImageLayer().startPreviewing();
            AdjustPanel adjustPanel = filter.getAdjustPanel();
            filter.startDialogSession();

            return adjustPanel;
        }

        @Override
        void onWizardCancelled(TweenWizard wizard) {
            FilterWithParametrizedGUI filter = wizard.getAnimation().getFilter();
            ImageComponents.getActiveComp().getActiveImageLayer().cancelPreviewing();
            filter.endDialogSession();
        }

        @Override
        void onMovingToTheNext(TweenWizard wizard) {
//            ParamSet paramSet = wizard.getAnimation().getFilter().getParamSet();
//            ParamSetState initialState = paramSet.copyState();
//            wizard.getAnimation().setInitialState(initialState);
            wizard.getAnimation().copyInitialStateFromCurrent();

            ParametrizedAdjustPanel.setResetParams(false);
            wizard.getAnimation().getFilter().getParamSet().setFinalAnimationSettingMode(true);
        }
    }, FINAL_FILTER_SETTINGS {
        @Override
        String getHeaderText() {
            return "<html> Select the <b><font color=green size=+1>final</font></b> settings for the filter.";
        }

        @Override
        TweenWizardState getNext() {
            return SELECT_OUTPUT_SETTINGS;
        }

        @Override
        JComponent getPanel(TweenWizard wizard) {
            FilterWithParametrizedGUI filter = wizard.getAnimation().getFilter();
            AdjustPanel adjustPanel = filter.getAdjustPanel();
            return adjustPanel;
        }

        @Override
        void onWizardCancelled(TweenWizard wizard) {
            FilterWithParametrizedGUI filter = wizard.getAnimation().getFilter();
            ImageComponents.getActiveComp().getActiveImageLayer().cancelPreviewing();
            filter.endDialogSession();
        }

        @Override
        void onMovingToTheNext(TweenWizard wizard) {
            // cancel the previewing
            onWizardCancelled(wizard);

            // save final state
            wizard.getAnimation().copyFinalStateFromCurrent();

//            ParamSet paramSet = wizard.getAnimation().getFilter().getParamSet();
//            wizard.setFinalState(paramSet.copyState());
        }
    }, SELECT_OUTPUT_SETTINGS {
        OutputSettingsPanel outputSettingsPanel;

        @Override
        String getHeaderText() {
            return "<html> <b>Output settings</b>" +
                    "<p>For file output select a file in an existing directory. " +
                    "<br>For file sequence output select an existing directory.";
        }

        @Override
        TweenWizardState getNext() {
            return null;
        }

        @Override
        JComponent getPanel(TweenWizard wizard) {
            outputSettingsPanel = new OutputSettingsPanel(wizard);
            return outputSettingsPanel;
        }

        @Override
        void onWizardCancelled(TweenWizard wizard) {

        }

        @Override
        void onMovingToTheNext(TweenWizard wizard) {
            TweenOutputType type = outputSettingsPanel.getTweenOutputType();
            TweenAnimation animation = wizard.getAnimation();
            animation.setOutputType(type);

            File output = outputSettingsPanel.getOutput();
            type.checkFile(output);
            animation.setOutput(output);

            animation.setNumFrames(outputSettingsPanel.getNumFrames());
            animation.setMillisBetweenFrames(outputSettingsPanel.getMillisBetweenFrames());
            animation.setInterpolation(outputSettingsPanel.getInterpolation());

        }
    };

    abstract String getHeaderText();

    abstract TweenWizardState getNext();

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

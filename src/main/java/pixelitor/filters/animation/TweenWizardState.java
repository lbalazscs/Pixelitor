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
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.ParamSetState;

import javax.swing.*;
import java.awt.FlowLayout;

/**
 * The states of the keyframe animation export dialog
 */
public enum TweenWizardState {
    SELECT_FILTER {
        JComboBox<FilterWithParametrizedGUI> filtersCB;

        @Override
        String getHelpMessage() {
            return "<html> Here you can create a basic keyframe animation <br>based on the settings of a filter.";
        }

        @Override
        TweenWizardState getNext() {
            return INITIAL_FILTER_SETTINGS;
        }

        @Override
        JPanel getPanel(final TweenWizard wizard) {
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
            wizard.setFilter(filter);
        }
    }, INITIAL_FILTER_SETTINGS {
        @Override
        String getHelpMessage() {
            return "<html> Select the <b>initial</b> settings for the filter";
        }

        @Override
        TweenWizardState getNext() {
            return FINAL_FILTER_SETTINGS;
        }

        @Override
        JPanel getPanel(TweenWizard wizard) {
            FilterWithParametrizedGUI filter = wizard.getFilter();
            ImageComponents.getActiveComp().getActiveImageLayer().startPreviewing();
            AdjustPanel adjustPanel = filter.getAdjustPanel();
            filter.startDialogSession();

            return adjustPanel;
        }

        @Override
        void onWizardCancelled(TweenWizard wizard) {
            FilterWithParametrizedGUI filter = wizard.getFilter();
            ImageComponents.getActiveComp().getActiveImageLayer().cancelPreviewing();
            filter.endDialogSession();
        }

        @Override
        void onMovingToTheNext(TweenWizard wizard) {
            ParamSet paramSet = wizard.getFilter().getParamSet();
            ParamSetState initialState = paramSet.copyState();
            wizard.setInitialState(initialState);
        }
    }, FINAL_FILTER_SETTINGS {
        @Override
        String getHelpMessage() {
            return "<html> Select the <b>final</b> settings for the filter.";
        }

        @Override
        TweenWizardState getNext() {
            return SELECT_DURATION;
        }

        @Override
        JPanel getPanel(TweenWizard wizard) {
            FilterWithParametrizedGUI filter = wizard.getFilter();
            AdjustPanel adjustPanel = filter.getAdjustPanel();
            return adjustPanel;
        }

        @Override
        void onWizardCancelled(TweenWizard wizard) {
            FilterWithParametrizedGUI filter = wizard.getFilter();
            ImageComponents.getActiveComp().getActiveImageLayer().cancelPreviewing();
            filter.endDialogSession();
        }

        @Override
        void onMovingToTheNext(TweenWizard wizard) {
            // cancel the previewing
            onWizardCancelled(wizard);

            // save final state
            ParamSet paramSet = wizard.getFilter().getParamSet();
            wizard.setFinalState(paramSet.copyState());
        }
    }, SELECT_DURATION {
        DurationPanel durationPanel;

        @Override
        String getHelpMessage() {
            return "<html> Select the duration of the animation";
        }

        @Override
        TweenWizardState getNext() {
            return null;
        }

        @Override
        JPanel getPanel(TweenWizard wizard) {
            durationPanel = new DurationPanel(wizard);
            return durationPanel;
        }

        @Override
        void onWizardCancelled(TweenWizard wizard) {
        }

        @Override
        void onMovingToTheNext(TweenWizard wizard) {
            wizard.setNumFrames(durationPanel.getNumFrames());
            wizard.setMillisBetweenFrames(durationPanel.getMillisBetweenFrames());
            wizard.setInterpolation(durationPanel.getInterpolation());
        }
    };

    abstract String getHelpMessage();

    abstract TweenWizardState getNext();

    abstract JPanel getPanel(TweenWizard wizard);

    /**
     * Called if the wizard was cancelled while in this state
     */
    abstract void onWizardCancelled(TweenWizard wizard);

    /**
     * Called if next was pressed while in this state before moving to the next
     */
    abstract void onMovingToTheNext(TweenWizard wizard);
}

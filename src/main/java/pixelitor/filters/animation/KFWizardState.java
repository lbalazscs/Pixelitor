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

import javax.swing.*;
import java.awt.FlowLayout;

/**
 * The states of the keyframe animation export dialog
 */
public enum KFWizardState {
    SELECT_FILTER {
        JComboBox<FilterWithParametrizedGUI> filtersCB;

        @Override
        String getHelpMessage() {
            return "<html> Here you can create a basic keyframe animation <br>based on the settings of a filter.";
        }

        @Override
        KFWizardState getNext() {
            return INITIAL_FILTER_SETTINGS;
        }

        @Override
        JPanel getPanel(final KFWizard wizard) {
            JPanel p = new JPanel(new FlowLayout());
            p.add(new JLabel("Select Filter:"));
            filtersCB = new JComboBox<>(FilterUtils.getAnimationFiltersSorted());
            p.add(filtersCB);
            return p;
        }

        @Override
        void onWizardCancelled(KFWizard wizard) {
        }

        @Override
        void onMovingToTheNext(KFWizard wizard) {
            FilterWithParametrizedGUI filter = (FilterWithParametrizedGUI) filtersCB.getSelectedItem();
            wizard.setFilter(filter);
        }
    }, INITIAL_FILTER_SETTINGS {
        @Override
        String getHelpMessage() {
            return "<html> Select the <b>initial</b> settings for the filter";
        }

        @Override
        KFWizardState getNext() {
            return FINAL_FILTER_SETTINGS;
        }

        @Override
        JPanel getPanel(KFWizard wizard) {
            FilterWithParametrizedGUI filter = wizard.getFilter();
            ImageComponents.getActiveComp().getActiveImageLayer().startPreviewing();
            AdjustPanel adjustPanel = filter.getAdjustPanel();
            filter.startDialogSession();

            return adjustPanel;
        }

        @Override
        void onWizardCancelled(KFWizard wizard) {
            FilterWithParametrizedGUI filter = wizard.getFilter();
            ImageComponents.getActiveComp().getActiveImageLayer().cancelPreviewing();
            filter.endDialogSession();
        }

        @Override
        void onMovingToTheNext(KFWizard wizard) {
            ParamSet paramSet = wizard.getFilter().getParamSet();
            wizard.setInitialState(paramSet.getState());
        }
    }, FINAL_FILTER_SETTINGS {
        @Override
        String getHelpMessage() {
            return "<html> Select the <b>final</b> settings for the filter.";
        }

        @Override
        KFWizardState getNext() {
            return SELECT_DURATION;
        }

        @Override
        JPanel getPanel(KFWizard wizard) {
            FilterWithParametrizedGUI filter = wizard.getFilter();
            AdjustPanel adjustPanel = filter.getAdjustPanel();
            return adjustPanel;
        }

        @Override
        void onWizardCancelled(KFWizard wizard) {
            FilterWithParametrizedGUI filter = wizard.getFilter();
            ImageComponents.getActiveComp().getActiveImageLayer().cancelPreviewing();
            filter.endDialogSession();
        }

        @Override
        void onMovingToTheNext(KFWizard wizard) {
            // cancel the previewing
            onWizardCancelled(wizard);

            // save final state
            ParamSet paramSet = wizard.getFilter().getParamSet();
            wizard.setFinalState(paramSet.getState());
        }
    }, SELECT_DURATION {
        @Override
        String getHelpMessage() {
            return "<html> Select the duration of the animation";
        }

        @Override
        KFWizardState getNext() {
            return null;
        }

        @Override
        JPanel getPanel(KFWizard wizard) {
            JPanel p = new JPanel();
            p.add(new JLabel("SELECT_DURATION"));
            return p;
        }

        @Override
        void onWizardCancelled(KFWizard wizard) {
        }

        @Override
        void onMovingToTheNext(KFWizard wizard) {

        }
    };

    abstract String getHelpMessage();

    abstract KFWizardState getNext();

    abstract JPanel getPanel(KFWizard wizard);

    /**
     * Called if the wizard was cancelled while in this state
     */
    abstract void onWizardCancelled(KFWizard wizard);

    /**
     * Called if next was pressed while in this state before moving to the next
     */
    abstract void onMovingToTheNext(KFWizard wizard);
}

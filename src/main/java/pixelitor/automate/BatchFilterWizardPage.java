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

package pixelitor.automate;

import org.jdesktop.swingx.VerticalLayout;
import pixelitor.ImageComponents;
import pixelitor.filters.Filter;
import pixelitor.filters.FilterUtils;
import pixelitor.filters.gui.AdjustPanel;
import pixelitor.filters.gui.FilterWithGUI;

import javax.swing.*;
import java.awt.FlowLayout;

public enum BatchFilterWizardPage implements WizardPage {
    SELECT_FILTER_AND_DIRS {
        private OpenSaveDirsPanel openSaveDirsPanel;
        private JComboBox<Filter> filtersCB;

        @Override
        public String getHeaderText(Wizard wizard) {
            return "<html> Apply a filter to every image in a folder.";
        }

        @Override
        public WizardPage getNext() {
            Filter filter = (Filter) filtersCB.getSelectedItem();
            if (filter instanceof FilterWithGUI) {
                return FILTER_GUI;
            } else {
                return null;
            }
        }

        @Override
        public JComponent getPanel(Wizard wizard) {
            JPanel p = new JPanel(new FlowLayout());
            p.add(new JLabel("Select Filter:"));
            if (filtersCB == null) {
                filtersCB = new JComboBox<>(FilterUtils.getAllFiltersSorted());
                filtersCB.setName("filtersCB");
            }
            p.add(filtersCB);

            JPanel main = new JPanel(new VerticalLayout());
            main.add(p);
            if (openSaveDirsPanel == null) {
                openSaveDirsPanel = new OpenSaveDirsPanel(false);
            }
            main.add(openSaveDirsPanel);

            return main;
        }

        @Override
        public void onWizardCancelled(Wizard wizard) {

        }

        @Override
        public void onMovingToTheNext(Wizard wizard) {
            Filter filter = (Filter) filtersCB.getSelectedItem();
            BatchFilterConfig config = getConfig(wizard);
            config.setFilter(filter);
            openSaveDirsPanel.saveValues();
        }
    }, FILTER_GUI {
        @Override
        public String getHeaderText(Wizard wizard) {
            return "<html> Select filter settings.";
        }

        @Override
        public WizardPage getNext() {
            return null;
        }

        @Override
        public JComponent getPanel(Wizard wizard) {
            // we get here only if the chosen filter is a filter with GUI
            FilterWithGUI filter = (FilterWithGUI) getConfig(wizard).getFilter();

            ImageComponents.getActiveImageLayer().get().startPreviewing();
            AdjustPanel adjustPanel = filter.createAdjustPanel();

            return adjustPanel;
        }

        @Override
        public void onWizardCancelled(Wizard wizard) {
            // we get here only if the chosen filter is a filter with GUI
//            FilterWithGUI filter = (FilterWithGUI) getConfig(wizard).getFilter();
            ImageComponents.getActiveImageLayer().get().cancelPressedInDialog();
        }

        @Override
        public void onMovingToTheNext(Wizard wizard) {
            // cancel the previewing
            onWizardCancelled(wizard);
        }
    };

    private static BatchFilterConfig getConfig(Wizard wizard) {
        // all wizards here can be casted to BatchFilterWizard
        return ((BatchFilterWizard) wizard).getConfig();
    }
}

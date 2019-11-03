/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.Filter;
import pixelitor.filters.FilterAction;
import pixelitor.filters.FilterUtils;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.io.OutputFormat;
import pixelitor.layers.Drawable;

import javax.swing.*;
import java.awt.FlowLayout;

/**
 * A page in the batch filter wizard
 */
public enum BatchFilterWizardPage implements WizardPage {
    SELECT_FILTER_AND_DIRS {
        private OpenSaveDirsPanel openSaveDirsPanel;
        private JComboBox<FilterAction> filtersCB;

        @Override
        public String getHeaderText(Wizard wizard) {
            return "<html> Apply a filter to every image in a folder.";
        }

        @Override
        public WizardPage getNext() {
            FilterAction selectedItem = (FilterAction) filtersCB.getSelectedItem();
            Filter filter = selectedItem.getFilter();
            if (filter instanceof FilterWithGUI) {
                return FILTER_GUI;
            } else {
                return null;
            }
        }

        @Override
        public JComponent getPanel(Wizard wizard, Drawable dr) {
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
                openSaveDirsPanel = new OpenSaveDirsPanel(
                        false, OutputFormat.getLastUsed());
            }
            main.add(openSaveDirsPanel);

            return main;
        }

        @Override
        public void onWizardCanceled(Drawable dr) {

        }

        @Override
        public void onMovingToTheNext(Wizard wizard, Drawable dr) {
            FilterAction selectedItem = (FilterAction) filtersCB.getSelectedItem();
            Filter filter = selectedItem.getFilter();

            ((BatchFilterWizard) wizard).setFilter(filter);

            openSaveDirsPanel.rememberValues();
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
        public JComponent getPanel(Wizard wizard, Drawable dr) {
            // we get here only if the chosen filter is a filter with GUI
            FilterWithGUI filter = (FilterWithGUI) ((BatchFilterWizard) wizard).getFilter();

            dr.startPreviewing();

            return filter.createGUI(dr);
        }

        @Override
        public void onWizardCanceled(Drawable dr) {
            // we get here only if the chosen filter is a filter with GUI
            dr.onFilterDialogCanceled();
        }

        @Override
        public void onMovingToTheNext(Wizard wizard, Drawable dr) {
            // cancel the previewing
            onWizardCanceled(dr);
        }
    }
}

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

package pixelitor.automate;

import org.jdesktop.swingx.VerticalLayout;
import pixelitor.filters.Filter;
import pixelitor.filters.FilterAction;
import pixelitor.filters.FilterUtils;
import pixelitor.filters.gui.AdjustPanel;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.layers.ImageLayer;

import javax.swing.*;
import java.awt.FlowLayout;

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
        public JComponent getPanel(Wizard wizard, ImageLayer layer) {
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
        public void onWizardCancelled(ImageLayer layer) {

        }

        @Override
        public void onMovingToTheNext(Wizard wizard, ImageLayer layer) {
            FilterAction selectedItem = (FilterAction) filtersCB.getSelectedItem();
            Filter filter = selectedItem.getFilter();
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
        public JComponent getPanel(Wizard wizard, ImageLayer layer) {
            // we get here only if the chosen filter is a filter with GUI
            FilterWithGUI filter = (FilterWithGUI) getConfig(wizard).getFilter();

            layer.startPreviewing();
            AdjustPanel adjustPanel = filter.createAdjustPanel(layer);

            return adjustPanel;
        }

        @Override
        public void onWizardCancelled(ImageLayer layer) {
            // we get here only if the chosen filter is a filter with GUI
//            FilterWithGUI filter = (FilterWithGUI) getConfig(wizard).getFilter();
            layer.cancelPressedInDialog();
        }

        @Override
        public void onMovingToTheNext(Wizard wizard, ImageLayer layer) {
            // cancel the previewing
            onWizardCancelled(layer);
        }
    };

    private static BatchFilterConfig getConfig(Wizard wizard) {
        // all wizards here can be casted to BatchFilterWizard
        return ((BatchFilterWizard) wizard).getConfig();
    }
}

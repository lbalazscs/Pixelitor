/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters;

import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.layers.Filterable;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.function.Supplier;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static java.awt.FlowLayout.LEFT;
import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.FilterContext.PREVIEWING;

/**
 * The GUI for the Random Filter
 */
public class RandomFilterGUI extends FilterGUI {
    private final Filterable layer;
    private final RandomFilterSource filterSource;

    private final JPanel settingsContainer; // holds the specific GUI of the selected filter
    private JPanel currentFilterPanel;

    private final JPanel northPanel;
    private final JButton backButton;
    private final JButton forwardButton;

    protected RandomFilterGUI(Filterable layer) {
        super(null, layer); // the actual filter will be determined later
        this.layer = layer;
        filterSource = new RandomFilterSource();

        setLayout(new BorderLayout());
        northPanel = new JPanel(new FlowLayout(LEFT));

        backButton = addButton("Back", filterSource::getPrevious);
        forwardButton = addButton("Forward", filterSource::getNext);
        addButton("Next Random Filter", filterSource::selectNewFilter);

        add(northPanel, NORTH);
        settingsContainer = new JPanel();
        add(settingsContainer, CENTER);

        // Pass false to avoid double preview calculation on startup.
        // The initial preview is triggered when the dialog becomes visible.
        showFilter(filterSource.selectNewFilter(), false);
        updateEnabled();
    }

    private JButton addButton(String text, Supplier<Filter> filterSupplier) {
        JButton button = new JButton(text);
        button.addActionListener(e -> {
            // trigger the preview when navigating
            showFilter(filterSupplier.get(), true);
            updateEnabled();
        });
        northPanel.add(button);
        return button;
    }

    private void updateEnabled() {
        backButton.setEnabled(filterSource.hasPrevious());
        forwardButton.setEnabled(filterSource.hasNext());
    }

    private void showFilter(Filter newFilter, boolean triggerPreview) {
        if (currentFilterPanel != null) {
            settingsContainer.remove(currentFilterPanel);
        }

        filter = newFilter;
        String filterName = newFilter.getName();
        settingsContainer.setBorder(createTitledBorder(filterName));

        if (newFilter instanceof FilterWithGUI newFilterWithGUI) {
            if (filterSource.getCurrentFilter() != null) { // there was a filter before
                // need to clear the preview of the previous filters
                // so that the image position selectors show the original image
                layer.stopPreviewing(); // stop the current one
                layer.startPreviewing(); // start the new one
            }
            FilterGUI filterGUI = newFilterWithGUI.createGUI(layer, true);
            settingsContainer.add(filterGUI);
            currentFilterPanel = filterGUI;

            // trigger the preview if requested (i.e., on button clicks)
            if (triggerPreview) {
                filterGUI.startPreview(true);
            }
        } else {
            currentFilterPanel = null;
            // only run if requested (avoids double run on startup)
            if (triggerPreview) {
                layer.startFilter(filter, PREVIEWING);
            }
        }

        settingsContainer.revalidate();
        settingsContainer.repaint();
    }
}

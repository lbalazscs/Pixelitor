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

package pixelitor.filters;

import pixelitor.ChangeReason;
import pixelitor.filters.gui.AdjustPanel;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.gui.ImageComponents;
import pixelitor.layers.ImageLayer;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

public class RandomFilterAdjustPanel extends AdjustPanel {
    private final JPanel realSettingsPanel;
    private final ImageLayer layer;
    private JPanel lastFilterPanel;
    private final RandomFilterSource filterSource;
    private final JPanel northPanel;
    private final JButton backButton;
    private final JButton forwardButton;

    protected RandomFilterAdjustPanel(ImageLayer layer) {
        super(null, layer); // the actual filter will be determined bellow
        this.layer = layer;
        filterSource = new RandomFilterSource();

        setLayout(new BorderLayout());
        northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        backButton = createButton("Back", e -> showFilter(filterSource.getPrevious()));
        forwardButton = createButton("Forward", e -> showFilter(filterSource.getNext()));
        createButton("Next Random Filter", e -> showFilter(filterSource.getRandom()));

        add(northPanel, BorderLayout.NORTH);
        realSettingsPanel = new JPanel();
        add(realSettingsPanel, BorderLayout.CENTER);

        showFilter(filterSource.getRandom());
        updateEnabled();
    }

    private JButton createButton(String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.addActionListener(e -> {
            listener.actionPerformed(e);
            updateEnabled();
        });
        northPanel.add(button);
        return button;
    }

    private void updateEnabled() {
        backButton.setEnabled(filterSource.hasPrevious());
        forwardButton.setEnabled(filterSource.hasNext());
    }

    private void showFilter(Filter newFilter) {
        if (lastFilterPanel != null) {
            realSettingsPanel.remove(lastFilterPanel);
        }

        filter = newFilter;
        String filterName = newFilter.getListName();
        realSettingsPanel.setBorder(BorderFactory.createTitledBorder(filterName));
        if (newFilter instanceof FilterWithGUI) {
            if (filterSource.getLastFilter() != null) { // there was a filter before
                // need to clear the preview of the previous filters
                // so that the image position selectors show the original image
                ImageLayer imageLayer = ImageComponents.getActiveImageLayerOrMaskOrNull();
                imageLayer.stopPreviewing(); // stop the last one
                imageLayer.startPreviewing(); // start the new one
            }
            AdjustPanel adjustPanel = ((FilterWithGUI) newFilter).createAdjustPanel(layer);
            realSettingsPanel.add(adjustPanel);
            adjustPanel.revalidate();
            lastFilterPanel = adjustPanel;
        } else {
            lastFilterPanel = null;
            filter.execute(layer, ChangeReason.OP_PREVIEW);
        }
    }
}

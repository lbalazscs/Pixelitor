/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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
import pixelitor.ImageComponents;
import pixelitor.filters.gui.AdjustPanel;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.layers.ImageLayer;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RandomFilterAdjustPanel extends AdjustPanel {
    private JPanel realSettingsPanel;
    private JPanel lastFilterPanel;
    private Filter lastFilter;

    protected RandomFilterAdjustPanel() {
        super(null); // the actual filter will be determined bellow
        setLayout(new BorderLayout());
        JButton nextRandomButton = new JButton("Next Random Filter");
        nextRandomButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nextRandomFilter();
            }
        });
        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        northPanel.add(nextRandomButton);
        add(northPanel, BorderLayout.NORTH);
        realSettingsPanel = new JPanel();
        add(realSettingsPanel, BorderLayout.CENTER);

        nextRandomFilter();
    }

    private void nextRandomFilter() {
        //noinspection InstanceVariableUsedBeforeInitialized
        if (lastFilterPanel != null) {
            realSettingsPanel.remove(lastFilterPanel);
        }
        Filter newFilter;
        do {
            newFilter = FilterUtils.getRandomFilter();
        } while (newFilter == op ||
                (newFilter instanceof Fade) ||
                (newFilter instanceof RandomFilter) ||
                (newFilter instanceof RepeatLastOp)
                );

        op = newFilter;
        String filterName = newFilter.getListName();
        realSettingsPanel.setBorder(BorderFactory.createTitledBorder(filterName));
        if (newFilter instanceof FilterWithGUI) {
            //noinspection InstanceVariableUsedBeforeInitialized
            if (lastFilter != null) { // there was a filter before
                // need to clear the preview of the previous filters
                // so that the image position selectors show the original image
                ImageLayer imageLayer = ImageComponents.getActiveImageLayer().get();
                imageLayer.cancelPressedInDialog(); // cancel the last one
                imageLayer.startPreviewing(); // start the new one
            }
            AdjustPanel adjustPanel = ((FilterWithGUI) newFilter).createAdjustPanel();
            realSettingsPanel.add(adjustPanel);
            adjustPanel.revalidate();
            lastFilterPanel = adjustPanel;
        } else {
            lastFilterPanel = null;
            op.execute(ChangeReason.OP_PREVIEW);
        }
        lastFilter = newFilter;
    }
}

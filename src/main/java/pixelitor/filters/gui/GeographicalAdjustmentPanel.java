/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
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
package pixelitor.filters.gui;

import pixelitor.filters.FilterWithParametrizedGUI;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

/**
 * An adjustment panel, where the components can be added in "geographical" places (north-west etc.)
 */
public class GeographicalAdjustmentPanel extends ParametrizedAdjustPanel {
    private boolean addLabels;

    public GeographicalAdjustmentPanel(FilterWithParametrizedGUI filter, boolean addLabels) {
        super(filter);
        this.addLabels = addLabels;
    }

    @Override
    protected void setupGUI(ParamSet params, Object otherInfo) {
        setLayout(new BorderLayout(5, 5));
        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));
        JPanel nonGeoControls = new JPanel();
        GridLayout layout;
        if (addLabels) {
            layout = new GridLayout(2, 4, 5, 5);
        } else {
            layout = new GridLayout(2, 2, 5, 5);
        }
        JPanel geoPanel = new JPanel(layout);
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        int added = 0;
        for (GUIParam param : params) {
            JComponent control = param.createGUI();

            if (control instanceof JButton) {
                buttonsPanel.add(control);
            } else {
                String labelText = param.getName() + ':';
                if (added < 4) {
                    if (addLabels) {
                        geoPanel.add(new JLabel(labelText));
                    }
                    geoPanel.add(control);
                } else {
                    nonGeoControls.add(new JLabel(labelText)); // these always need a label
                    nonGeoControls.add(control);
                }

            }
            added++;
        }
        controlPanel.add(geoPanel, BorderLayout.CENTER);
        controlPanel.add(nonGeoControls, BorderLayout.SOUTH);

        add(controlPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);

    }
}

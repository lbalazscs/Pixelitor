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

package pixelitor.filters.gui;

import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.ImageLayer;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

public class ChannelMixerAdjustments extends ParametrizedAdjustPanel {
    public ChannelMixerAdjustments(FilterWithParametrizedGUI filter, ImageLayer layer, Action[] actions) {
        super(filter, layer, actions, ShowOriginal.YES);
    }

    @Override
    protected void setupGUI(ParamSet params, Object otherInfo, ShowOriginal addShowOriginal) {
        JPanel upperPanel = new JPanel(new FlowLayout());
        JPanel leftPanel = GUIUtils.arrangeParamsInVerticalGridBag(params.getParamList());
        JPanel rightPanel = createPresetsPanel((Action[]) otherInfo);
        upperPanel.add(leftPanel);
        upperPanel.add(rightPanel);

        JPanel buttonsPanel = createFilterActionsPanel(params.getActionList(), addShowOriginal, 5);

        setLayout(new BorderLayout());
        add(upperPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);
    }

    private static JPanel createPresetsPanel(Action[] actions) {
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createTitledBorder("Presets"));
        for (Action action : actions) {
            JComponent b = new JButton(action);
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            rightPanel.add(b);
        }
        return rightPanel;
    }
}
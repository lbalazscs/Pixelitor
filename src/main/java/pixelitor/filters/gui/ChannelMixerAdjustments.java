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

package pixelitor.filters.gui;

import pixelitor.filters.FilterWithParametrizedGUI;

import javax.swing.*;
import java.awt.Component;
import java.awt.FlowLayout;

public class ChannelMixerAdjustments extends ParametrizedAdjustPanel {

    public ChannelMixerAdjustments(FilterWithParametrizedGUI filter, Action[] actions) {
        super(filter, actions, ShowOriginal.YES);
    }

    @Override
    protected void setupGUI(ParamSet params, Object otherInfo, ShowOriginal addShowOriginal) {
        Action[] actions = (Action[]) otherInfo;

        JPanel leftPanel = new JPanel();
        setupControlsInColumn(leftPanel, params, addShowOriginal);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createTitledBorder("Presets"));

        for (Action action : actions) {
            JComponent b = new JButton(action);
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            rightPanel.add(b);
        }
        setLayout(new FlowLayout());
        add(leftPanel);
        add(rightPanel);
    }
}
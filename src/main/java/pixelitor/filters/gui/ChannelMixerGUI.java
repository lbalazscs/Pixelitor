/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.ChannelMixer;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Filterable;

import javax.swing.*;
import java.awt.BorderLayout;

import static java.awt.BorderLayout.*;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.BoxLayout.Y_AXIS;

/**
 * The GUI for the "Channel Mixer"
 */
public class ChannelMixerGUI extends ParametrizedFilterGUI {
    public ChannelMixerGUI(ParametrizedFilter filter, Filterable layer,
                           Action[] actions, boolean reset) {
        super(filter, layer, true, reset, actions);
    }

    @Override
    protected void setupGUI(ParamSet paramSet,
                            boolean addShowOriginal,
                            Object otherInfo) {
        var upperPanel = new JPanel(new BorderLayout());
        var leftPanel = GUIUtils.arrangeVertically(paramSet);
        var rightPanel = createPresetsPanel((Action[]) otherInfo);

        // This is a right place to do this, because when this code is
        // called, the default adjustment listener is already set.
        ((ChannelMixer) this.filter).replaceAdjustmentListeners();

        upperPanel.add(leftPanel, CENTER);
        upperPanel.add(rightPanel, EAST);

        var buttonsPanel = createFilterActionsPanel(
            paramSet.getActions(), addShowOriginal, 5);

        setLayout(new BorderLayout());
        add(upperPanel, CENTER);
        add(buttonsPanel, SOUTH);
    }

    private static JPanel createPresetsPanel(Action[] actions) {
        var rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, Y_AXIS));
        rightPanel.setBorder(createTitledBorder(DialogMenuBar.BUILT_IN_PRESETS));
        for (Action action : actions) {
            JComponent b = new JButton(action);
            b.setAlignmentX(LEFT_ALIGNMENT);
            rightPanel.add(b);
        }
        return rightPanel;
    }
}
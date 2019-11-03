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

package pixelitor.filters.gui;

import pixelitor.filters.ChannelMixer;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Drawable;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;

import static javax.swing.BorderFactory.createTitledBorder;

/**
 * The GUI for the "Channel Mixer"
 */
public class ChannelMixerGUI extends ParametrizedFilterGUI {
    public ChannelMixerGUI(ParametrizedFilter filter, Drawable dr, Action[] actions) {
        super(filter, dr, ShowOriginal.YES, actions);
    }

    @Override
    protected void setupGUI(ParamSet params,
                            ShowOriginal addShowOriginal,
                            Object otherInfo) {
        JPanel upperPanel = new JPanel(new BorderLayout());
        JPanel leftPanel = GUIUtils.arrangeParamsVertically(params.getParams());
        JPanel rightPanel = createPresetsPanel((Action[]) otherInfo);
        JCheckBox monochromeCB = new JCheckBox("Convert to Black and White", false);
        monochromeCB.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        monochromeCB.addChangeListener(e ->
                ((ChannelMixer) filter).setMonochrome(monochromeCB.isSelected()));
        params.setBeforeResetAction(() -> monochromeCB.setSelected(false));
        upperPanel.add(monochromeCB, BorderLayout.NORTH);
        upperPanel.add(leftPanel, BorderLayout.CENTER);
        upperPanel.add(rightPanel, BorderLayout.EAST);

        JPanel buttonsPanel = createFilterActionsPanel(
                params.getActions(), addShowOriginal, 5);

        setLayout(new BorderLayout());
        add(upperPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);
    }

    private static JPanel createPresetsPanel(Action[] actions) {
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(createTitledBorder("Presets"));
        for (Action action : actions) {
            JComponent b = new JButton(action);
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            rightPanel.add(b);
        }
        return rightPanel;
    }
}
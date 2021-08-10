/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import java.awt.FlowLayout;

import static java.awt.BorderLayout.*;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.BoxLayout.Y_AXIS;

/**
 * The GUI for the "Channel Mixer"
 */
public class ChannelMixerGUI extends ParametrizedFilterGUI {
    public ChannelMixerGUI(ParametrizedFilter filter, Drawable dr,
                           Action[] actions, boolean reset) {
        super(filter, dr, true, reset, actions);
    }

    @Override
    protected void setupGUI(ParamSet paramSet,
                            boolean addShowOriginal,
                            Object otherInfo) {
        var upperPanel = new JPanel(new BorderLayout());
        var leftPanel = GUIUtils.arrangeVertically(paramSet);
        var rightPanel = createPresetsPanel((Action[]) otherInfo);

        var monochromeCB = new JCheckBox("Allow only Black and White", false);
        monochromeCB.setToolTipText("Link the sliders so that the image always stays black and white");
        monochromeCB.addChangeListener(e1 ->
            ((ChannelMixer) filter).setMonochrome(monochromeCB.isSelected()));

        var autoNormalizeCB = new JCheckBox("Auto-Normalize", true);
        autoNormalizeCB.setToolTipText("Preserve brightness by ensuring that the sum of percentages is around 100%");
        autoNormalizeCB.addChangeListener(e ->
            ((ChannelMixer) filter).setAutoNormalize(autoNormalizeCB.isSelected()));

        paramSet.setBeforeResetAllAction(() ->
            beforeResetAll(monochromeCB, autoNormalizeCB));

        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        northPanel.add(autoNormalizeCB);
        northPanel.add(monochromeCB);
        upperPanel.add(northPanel, NORTH);
        upperPanel.add(leftPanel, CENTER);
        upperPanel.add(rightPanel, EAST);

        var buttonsPanel = createFilterActionsPanel(
            paramSet.getActions(), addShowOriginal, 5);

        setLayout(new BorderLayout());
        add(upperPanel, CENTER);
        add(buttonsPanel, SOUTH);
    }

    private void beforeResetAll(JCheckBox monochromeCB, JCheckBox autoNormalizeCB) {
        monochromeCB.setSelected(false);

        if (!autoNormalizeCB.isSelected()) {
            // prevent triggering the filter twice by pretending that it's already true
            ChannelMixer cm = (ChannelMixer) this.filter;
            cm.temporarilyEnableNormalization(true);
            autoNormalizeCB.setSelected(true);

            // has to be called here, because of the previous trick
            cm.enablePresets();
        }
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
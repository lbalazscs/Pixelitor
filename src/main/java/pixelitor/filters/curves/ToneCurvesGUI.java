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

package pixelitor.filters.curves;

import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.levels.Channel;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Filterable;

import javax.swing.*;
import java.awt.FlowLayout;

import static java.awt.FlowLayout.LEFT;
import static javax.swing.BoxLayout.PAGE_AXIS;

/**
 * The GUI for the tone curve filter
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurvesGUI extends FilterGUI {
    private final ToneCurvesPanel curvesPanel;

    public ToneCurvesGUI(ToneCurvesFilter filter, Filterable layer) {
        super(filter, layer);
        setLayout(new BoxLayout(this, PAGE_AXIS));
        ToneCurves curves = filter.getCurves();

        add(createChannelPanel(curves.getActiveChannel()));

        curvesPanel = new ToneCurvesPanel(curves);
        curvesPanel.addActionListener(e -> settingsChanged(false));
        add(curvesPanel);

        add(createButtonsPanel(layer, curvesPanel));
    }

    private JPanel createChannelPanel(Channel activeChannel) {
        JPanel channelPanel = new JPanel(new FlowLayout(LEFT));

        channelPanel.add(new JLabel("Channel:"));
        channelPanel.add(createChannelsCombo(activeChannel));
        channelPanel.add(GUIUtils.createResetChannelButton(e ->
            curvesPanel.resetActiveCurve()));

        return channelPanel;
    }

    private JComboBox<Channel> createChannelsCombo(Channel activeChannel) {
        var channelTypeCB = GUIUtils.createComboBox(Channel.values());
        channelTypeCB.setSelectedItem(activeChannel);
        channelTypeCB.addActionListener(e -> curvesPanel.setActiveCurve(
            (Channel) channelTypeCB.getSelectedItem()));
        return channelTypeCB;
    }

    private static JPanel createButtonsPanel(Filterable layer, ToneCurvesPanel curvesPanel) {
        JPanel buttonsPanel = new JPanel(new FlowLayout(LEFT));
        buttonsPanel.add(createShowOriginalCB(layer));
        buttonsPanel.add(GUIUtils.createResetAllButton(e -> curvesPanel.reset()));
        return buttonsPanel;
    }

    private static JCheckBox createShowOriginalCB(Filterable layer) {
        JCheckBox showOriginalCB = new JCheckBox("Show Original");
        showOriginalCB.setName("show original");
        showOriginalCB.addActionListener(e -> layer.setShowOriginal(showOriginalCB.isSelected()));
        return showOriginalCB;
    }

    public void stateChanged() {
        curvesPanel.stateChanged();
    }

    public ToneCurves getCurves() {
        return curvesPanel.toneCurves;
    }
}

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

package pixelitor.filters.curves;

import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.util.Channel;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Filterable;

import javax.swing.*;
import java.awt.FlowLayout;

import static java.awt.FlowLayout.LEFT;
import static javax.swing.BoxLayout.PAGE_AXIS;

/**
 * The {@link FilterGUI} for the {@link ToneCurvesFilter}.
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
        curvesPanel.addActionListener(e -> startPreview(false));
        add(curvesPanel);

        add(createButtonsPanel(layer));
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
        var channelCB = GUIUtils.createComboBox(Channel.values());
        channelCB.setSelectedItem(activeChannel);
        channelCB.addActionListener(e -> curvesPanel.setActiveCurve(
            (Channel) channelCB.getSelectedItem()));
        return channelCB;
    }

    private JPanel createButtonsPanel(Filterable layer) {
        JPanel buttonsPanel = new JPanel(new FlowLayout(LEFT));
        buttonsPanel.add(createShowOriginalCB(layer));
        buttonsPanel.add(GUIUtils.createResetAllButton(e -> curvesPanel.resetAllCurves()));
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

/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.ChannelSelectorPanel;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Filterable;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static java.awt.BorderLayout.SOUTH;

/**
 * The {@link FilterGUI} for the {@link ToneCurvesFilter}.
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurvesGUI extends FilterGUI {
    private final ToneCurvesPanel curvesPanel;
    private final ToneCurves curves;
    private final ChannelSelectorPanel channelSelectorPanel;

    public ToneCurvesGUI(ToneCurvesFilter filter, Filterable layer) {
        super(filter, layer);
        setLayout(new BorderLayout());
        this.curves = filter.getCurves();

        curvesPanel = new ToneCurvesPanel(curves);
        curvesPanel.addActionListener(e -> startPreview(false));

        channelSelectorPanel = new ChannelSelectorPanel(curvesPanel::setActiveCurve);
        channelSelectorPanel.addResetButton(e -> curvesPanel.resetActiveCurve());
        channelSelectorPanel.addColorSpaceChangedListener(colorSpace -> {
            curves.setColorSpace(colorSpace);
            curvesPanel.stateChanged();
        });

        add(channelSelectorPanel, NORTH);
        add(curvesPanel, CENTER);
        add(createButtonsPanel(layer), SOUTH);
    }

    private JPanel createButtonsPanel(Filterable layer) {
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        buttonsPanel.add(createShowOriginalCB(layer));
        buttonsPanel.add(GUIUtils.createRandomizeSettingsButton(e ->
            curvesPanel.randomize()));
        buttonsPanel.add(GUIUtils.createResetAllButton(e ->
            curvesPanel.resetAllCurves()));

        return buttonsPanel;
    }

    private static JCheckBox createShowOriginalCB(Filterable layer) {
        JCheckBox showOriginalCB = new JCheckBox("Show Original");
        showOriginalCB.setName("show original");
        showOriginalCB.addActionListener(e -> layer.setShowOriginal(showOriginalCB.isSelected()));
        return showOriginalCB;
    }

    public void stateChanged() {
        // synchronize the UI dropdown to reflect the filter's
        // backend data model (necessary when loading a preset)
        channelSelectorPanel.setColorSpaceUI(curves.getColorSpace());

        // paint the updated tone curves and dispatch standard events
        curvesPanel.stateChanged();
    }

    public ToneCurves getCurves() {
        return curves;
    }
}

/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.brushes;

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.utils.Lazy;

import javax.swing.*;
import java.awt.GridBagLayout;

public class ConnectBrushSettings implements BrushSettings {
    private static final boolean RESET_DEFAULT = false;

    private final Lazy<JPanel> configPanel = Lazy.of(this::createConfigPanel);
    private JCheckBox resetForEachStroke;

    private final EnumComboBoxModel<Style> styleModel = new EnumComboBoxModel<>(Style.class);

    private ConnectBrush brush;
    private final RangeParam densityModel = new RangeParam("Line Density (%)", 1, 50, 100);
    private final RangeParam widthModel = new RangeParam("Line Width (px)", 1, 1, 10);

    @Override
    public JPanel getConfigPanel() {
        return configPanel.get();
    }

    public boolean deleteHistoryForEachStroke() {
        if (resetForEachStroke == null) { // not configured
            return RESET_DEFAULT;
        }
        return resetForEachStroke.isSelected();
    }

    public Style getStyle() {
        return styleModel.getSelectedItem();
    }

    private JPanel createConfigPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagHelper gbh = new GridBagHelper(p);

        JComboBox<Style> styleCombo = new JComboBox<>(styleModel);
        gbh.addLabelWithControl("Length: ", styleCombo);

        SliderSpinner densitySlider = SliderSpinner.simpleFrom(densityModel);
        gbh.addLabelWithControl(densityModel.getName() + ":", densitySlider);

        SliderSpinner widthSlider = SliderSpinner.simpleFrom(widthModel);
        gbh.addLabelWithControl(widthModel.getName() + ":", widthSlider);

        resetForEachStroke = new JCheckBox("", RESET_DEFAULT);
        gbh.addLabelWithControl("Reset History for Each Stroke: ", resetForEachStroke);

        JButton resetHistoryNowButton = new JButton("Reset History Now");
        gbh.addOnlyControl(resetHistoryNowButton);
        resetHistoryNowButton.addActionListener(e -> brush.deleteHistory());

        return p;
    }

    public void setBrush(ConnectBrush brush) {
        this.brush = brush;
    }

    public double getDensity() {
        return densityModel.getValueAsPercentage();
    }

    public float getLineWidth() {
        return widthModel.getValueAsFloat();
    }

    public enum Style {
        NORMAL("Normal", 0.0),
        CHROME("Chrome", -0.2),
        FUR("Fur", 0.2);
//        LONG_FUR("Long Fur", 0.5);

        private final String guiName;
        private final double offset;

        Style(String guiName, double offset) {
            this.guiName = guiName;
            this.offset = offset;
        }

        public double getOffset() {
            return offset;
        }

        @Override
        public String toString() {
            return guiName;
        }
    }
}

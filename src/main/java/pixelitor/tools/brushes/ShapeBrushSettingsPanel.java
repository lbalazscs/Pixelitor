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

package pixelitor.tools.brushes;

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.filters.gui.RangeParam;
import pixelitor.tools.ShapeType;
import pixelitor.utils.GridBagHelper;

import javax.swing.*;
import java.awt.GridBagLayout;

import static pixelitor.utils.SliderSpinner.TextPosition.NONE;

public class ShapeBrushSettingsPanel extends JPanel {
    public static final ShapeType SHAPE_SELECTED_BY_DEFAULT = ShapeType.ARROW;
    public static final double DEFAULT_SPACING_RATIO = 2.3;

    public ShapeBrushSettingsPanel(ShapeDabsBrushSettings settings) {
        super(new GridBagLayout());

        GridBagHelper gbh = new GridBagHelper(this);
        EnumComboBoxModel<ShapeType> typeModel = new EnumComboBoxModel<>(ShapeType.class);
        JComboBox shapeTypeCB = new JComboBox(typeModel);
        shapeTypeCB.setSelectedItem(SHAPE_SELECTED_BY_DEFAULT);
        shapeTypeCB.addActionListener(
                e -> {
                    ShapeType shapeType = (ShapeType) shapeTypeCB.getSelectedItem();
                    settings.setShapeType(shapeType);
                });


        gbh.addLabelWithControl("Shape:", shapeTypeCB);

        RangeParam spacingSelector = new RangeParam("Spacing", 1, 1000, (int) Math.round(DEFAULT_SPACING_RATIO * 100), false, NONE);
        gbh.addLabelWithControl("Spacing (radius %):", spacingSelector.createGUI());
        spacingSelector.setAdjustmentListener(
                () -> settings.changeSpacing(new RadiusRatioSpacing(spacingSelector.getValueAsPercentage())));
    }

}

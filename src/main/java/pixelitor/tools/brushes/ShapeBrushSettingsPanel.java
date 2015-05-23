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
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.tools.ShapeType;
import pixelitor.utils.GridBagHelper;

import javax.swing.*;
import java.awt.GridBagLayout;

import static pixelitor.utils.SliderSpinner.TextPosition.NONE;

public class ShapeBrushSettingsPanel extends JPanel {
    public static final ShapeType SHAPE_SELECTED_BY_DEFAULT = ShapeType.ARROW;
    public static final double DEFAULT_SPACING_RATIO = 2.3;
    private final ShapeDabsBrushSettings settings;
    private final BooleanParam angleAware;
    private final RangeParam angleScattering;

    public ShapeBrushSettingsPanel(ShapeDabsBrushSettings settings) {
        super(new GridBagLayout());
        this.settings = settings;

        GridBagHelper gbh = new GridBagHelper(this);
        EnumComboBoxModel<ShapeType> typeModel = new EnumComboBoxModel<>(ShapeType.class);
        JComboBox shapeTypeCB = new JComboBox(typeModel);
        shapeTypeCB.setSelectedItem(SHAPE_SELECTED_BY_DEFAULT);
        shapeTypeCB.addActionListener(
                e -> {
                    ShapeType shapeType = (ShapeType) shapeTypeCB.getSelectedItem();
                    settings.setShapeType(shapeType);
                });

        gbh.addLabelWithControlNoFill("Shape:", shapeTypeCB);

        RangeParam spacingSelector = new RangeParam("", 1, 1000, (int) Math.round(DEFAULT_SPACING_RATIO * 100), true, NONE);
        gbh.addLabelWithControlNoFill("Spacing (radius %):", spacingSelector.createGUI());
        spacingSelector.setAdjustmentListener(
                () -> settings.changeSpacing(new RadiusRatioSpacing(spacingSelector.getValueAsPercentage())));

        angleScattering = new RangeParam("", 0, 180, 0, true, NONE);
        gbh.addLabelWithControlNoFill("  Angle Scattering (degrees):", angleScattering.createGUI());
        angleScattering.setAdjustmentListener(this::changeAngleSettings);

        angleAware = new BooleanParam("", true);
        gbh.addLabelWithControlNoFill("Angle Follows Movement:", angleAware.createGUI());
        angleAware.setAdjustmentListener(this::changeAngleSettings);
    }

    private void changeAngleSettings() {
        boolean angleAwareChecked = angleAware.isChecked();
        float angleScatteringInRadians = angleScattering.getValueInRadians();
        AngleSettings angleSettings = new AngleSettings(angleAwareChecked, angleScatteringInRadians);
        settings.changeAngleSettings(angleSettings);
    }
}

/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.filters.gui.AddDefaultButton;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.tools.ShapeType;

import javax.swing.*;
import java.awt.GridBagLayout;

import static pixelitor.gui.utils.SliderSpinner.TextPosition.NONE;

public class BrushSettingsPanel extends JPanel {
    public static final ShapeType SHAPE_SELECTED_BY_DEFAULT = ShapeType.ARROW;
    public static final double DEFAULT_SPACING_RATIO = 2.3;
    private final ShapeDabsBrushSettings settings;
    private BooleanParam angleAware;
    private RangeParam angleJitter;
    private final GridBagHelper gbh;

    public BrushSettingsPanel(ShapeDabsBrushSettings settings) {
        super(new GridBagLayout());
        this.settings = settings;

        gbh = new GridBagHelper(this);
        addShapeTypeSelector(settings);
        addSpacingSelector(settings);
        addAngleSettingsSelector();
    }

    protected void addAngleSettingsSelector() {
        angleJitter = new RangeParam("", 0, 0, 180, AddDefaultButton.YES, NONE);
        gbh.addLabelWithControlNoFill("  Angle Jitter (degrees):", angleJitter.createGUI());
        angleJitter.setAdjustmentListener(this::changeAngleSettings);

        angleAware = new BooleanParam("", true);
        gbh.addLabelWithControlNoFill("Angle Follows Movement:", angleAware.createGUI());
        angleAware.setAdjustmentListener(this::changeAngleSettings);
    }

    protected void addSpacingSelector(DabsBrushSettings settings) {
        RangeParam spacingSelector = new RangeParam("", 1, (int) Math.round(DEFAULT_SPACING_RATIO * 100), 1000,
                AddDefaultButton.YES, NONE);
        gbh.addLabelWithControlNoFill("Spacing (radius %):", spacingSelector.createGUI());
        spacingSelector.setAdjustmentListener(
                () -> settings.changeSpacing(new RadiusRatioSpacing(spacingSelector.getValueAsPercentage())));
    }

    protected void addShapeTypeSelector(ShapeDabsBrushSettings settings) {
        EnumComboBoxModel<ShapeType> typeModel = new EnumComboBoxModel<>(ShapeType.class);
        JComboBox shapeTypeCB = new JComboBox(typeModel);
        shapeTypeCB.setSelectedItem(SHAPE_SELECTED_BY_DEFAULT);
        shapeTypeCB.addActionListener(
                e -> {
                    ShapeType shapeType = (ShapeType) shapeTypeCB.getSelectedItem();
                    settings.setShapeType(shapeType);
                });

        gbh.addLabelWithControlNoFill("Shape:", shapeTypeCB);
    }

    private void changeAngleSettings() {
        boolean angleAwareChecked = angleAware.isChecked();
        float angleJitterRadians = angleJitter.getValueInRadians();
        AngleSettings angleSettings = new AngleSettings(angleAwareChecked, angleJitterRadians);
        settings.changeAngleSettings(angleSettings);
    }
}

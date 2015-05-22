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

package pixelitor.tools.shapestool;

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.PixelitorWindow;
import pixelitor.filters.gui.RangeParam;
import pixelitor.tools.ShapeType;
import pixelitor.tools.StrokeType;
import pixelitor.utils.OKDialog;
import pixelitor.utils.SliderSpinner;

import javax.swing.*;
import java.awt.GridLayout;

import static pixelitor.utils.SliderSpinner.TextPosition.BORDER;

public class StrokeSettingsDialog extends OKDialog {
    private JComboBox<ShapeType> shapeTypeCB;
    private JComboBox<StrokeType> strokeTypeCB;

    public StrokeSettingsDialog(RangeParam strokeWidthParam,
                                EnumComboBoxModel<BasicStrokeCap> capModel,
                                EnumComboBoxModel<BasicStrokeJoin> joinModel,
                                EnumComboBoxModel<StrokeType> strokeTypeModel,
                                ButtonModel dashedModel) {
        super(PixelitorWindow.getInstance(), "Stroke Settings", "Close");

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        SliderSpinner strokeWidthSlider = new SliderSpinner(strokeWidthParam, BORDER, false);
        p.add(strokeWidthSlider);
        p.add(strokeWidthSlider);

        JPanel capJoinPanel = createCapJoinPanel(capModel, joinModel);
        p.add(capJoinPanel);

        JPanel strokeTypePanel = createStrokeTypePanel(strokeTypeModel, dashedModel);
        p.add(strokeTypePanel);

        setupGUI(p, false);
    }

    private static JPanel createCapJoinPanel(
            EnumComboBoxModel<BasicStrokeCap> capModel,
            EnumComboBoxModel<BasicStrokeJoin> joinModel) {
        JPanel capJoinPanel = new JPanel();
        capJoinPanel.setBorder(BorderFactory.createTitledBorder("Line Endpoints"));
        capJoinPanel.setLayout(new GridLayout(2, 2, 5, 5));

        capJoinPanel.add(new JLabel("Endpoint Cap:", JLabel.RIGHT));
        JComboBox<BasicStrokeCap> capCB = new JComboBox(capModel);
        capCB.setToolTipText("The shape of the endpoints of the lines");
        capJoinPanel.add(capCB);

        capJoinPanel.add(new JLabel("Corner Join:", JLabel.RIGHT));
        JComboBox<BasicStrokeJoin> joinCB = new JComboBox(joinModel);
        joinCB.setToolTipText("The way lines connect at the corners");
        capJoinPanel.add(joinCB);
        return capJoinPanel;
    }

    private JPanel createStrokeTypePanel(EnumComboBoxModel<StrokeType> strokeTypeModel, ButtonModel dashedModel) {
        JPanel strokeTypePanel = new JPanel();
        strokeTypePanel.setBorder(BorderFactory.createTitledBorder("Stroke Type"));

        strokeTypePanel.setLayout(new GridLayout(3, 2, 5, 5));

        EnumComboBoxModel<ShapeType> typeModel = new EnumComboBoxModel<>(ShapeType.class);
        typeModel.setSelectedItem(ShapeType.KIWI);

        shapeTypeCB = new JComboBox(typeModel);

        strokeTypeCB = new JComboBox(strokeTypeModel);
        strokeTypeCB.addActionListener(e -> setWhetherShapeTypeCBIsEnabled());

        setWhetherShapeTypeCBIsEnabled();

        shapeTypeCB.addActionListener(e -> StrokeType.SHAPE.setShapeType(typeModel.getSelectedItem()));

        strokeTypePanel.add(new JLabel("Line Type:", JLabel.RIGHT));
        strokeTypePanel.add(strokeTypeCB);

        strokeTypePanel.add(new JLabel("Shape:", JLabel.RIGHT));
        strokeTypePanel.add(shapeTypeCB);

        strokeTypePanel.add(new JLabel("Dashed:", JLabel.RIGHT));

        JCheckBox dashedCB = new JCheckBox();
        dashedCB.setModel(dashedModel);
        strokeTypePanel.add(dashedCB);
        return strokeTypePanel;
    }

    private void setWhetherShapeTypeCBIsEnabled() {
        if (strokeTypeCB.getSelectedItem() == StrokeType.SHAPE) {
            shapeTypeCB.setEnabled(true);
        } else {
            shapeTypeCB.setEnabled(false);
        }
    }
}

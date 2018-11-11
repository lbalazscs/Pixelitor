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

package pixelitor.tools.shapes;

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.filters.gui.EffectsParam;
import pixelitor.filters.gui.StrokeParam;
import pixelitor.filters.gui.StrokeSettings;
import pixelitor.filters.painters.AreaEffects;
import pixelitor.utils.Lazy;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.Stroke;

import static pixelitor.tools.shapes.TwoPointBasedPaint.NONE;
import static pixelitor.tools.shapes.TwoPointBasedPaint.RADIAL_GRADIENT;

public class ShapeSettings {
    private boolean regenerate = true;

    private final EnumComboBoxModel<ShapeType> typeModel
            = new EnumComboBoxModel<>(ShapeType.class);
    private final EnumComboBoxModel<ShapesTarget> targetModel
            = new EnumComboBoxModel<>(ShapesTarget.class);
    private final EnumComboBoxModel<TwoPointBasedPaint> fillPaintModel
            = new EnumComboBoxModel<>(TwoPointBasedPaint.class);
    private final EnumComboBoxModel<TwoPointBasedPaint> strokePaintModel
            = new EnumComboBoxModel<>(TwoPointBasedPaint.class);

    private final StrokeParam strokeParam = new StrokeParam("");

    // During a single mouse drag, only one stroke should be created
    // This is particularly important for "random shape"
    private final Lazy<Stroke> stroke = Lazy.of(strokeParam::createStroke);
    private final EffectsParam effectsParam = new EffectsParam("");
    private final ShapesTool tool;

    public ShapeSettings(ShapesTool tool) {
        this.tool = tool;
        fillPaintModel.setSelectedItem(RADIAL_GRADIENT);

        strokeParam.setAdjustmentListener(this::guiChanged);
        effectsParam.setAdjustmentListener(this::guiChanged);
    }

    private void guiChanged() {
        if (regenerate) {
            tool.regenerateShape();
        }
        tool.updateEnabledState();
    }

    public JComboBox<TwoPointBasedPaint> createFillPaintCombo() {
        JComboBox cb = new JComboBox<>(fillPaintModel);
        cb.setMaximumRowCount(fillPaintModel.getSize());
        cb.addActionListener(e -> guiChanged());
        return cb;
    }

    public JComboBox<TwoPointBasedPaint> createStrokePaintCombo() {
        JComboBox cb = new JComboBox<>(strokePaintModel);
        cb.setMaximumRowCount(strokePaintModel.getSize());
        cb.addActionListener(e -> guiChanged());
        return cb;
    }

    public JComboBox<ShapeType> createShapeTypeCombo() {
        JComboBox<ShapeType> shapeTypeCB = new JComboBox<>(typeModel);
        // make sure all values are visible without a scrollbar
        shapeTypeCB.setMaximumRowCount(typeModel.getSize());
        shapeTypeCB.addActionListener(e -> guiChanged());
        return shapeTypeCB;
    }

    public JComboBox<ShapesTarget> createTargetCombo() {
        JComboBox<ShapesTarget> actionCB = new JComboBox<>(targetModel);
        actionCB.addActionListener(e -> guiChanged());
        return actionCB;
    }

    public JDialog buildEffectsDialog(JDialog owner) {
        return effectsParam.buildDialog(owner, false);
    }

    public ShapesTarget getSelectedTarget() {
        return targetModel.getSelectedItem();
    }

    public ShapeType getSelectedType() {
        return typeModel.getSelectedItem();
    }

    public TwoPointBasedPaint getSelectedFillPaint() {
        return fillPaintModel.getSelectedItem();
    }

    public TwoPointBasedPaint getSelectedStrokePaint() {
        return strokePaintModel.getSelectedItem();
    }

    public boolean hasStrokePaint() {
        return getSelectedStrokePaint() != NONE;
    }

    public AreaEffects getEffects() {
        return effectsParam.getEffects();
    }

    public Stroke getStroke() {
        return stroke.get();
    }

    public StrokeSettings getStrokeSettings() {
        return (StrokeSettings) strokeParam.copyState();
    }

    public void invalidateStroke() {
        stroke.invalidate();
    }

    public JDialog createStrokeSettingsDialog() {
        return strokeParam.createSettingsDialog();
    }

    public StrokeParam getStrokeParam() {
        return strokeParam;
    }

    public void restoreFrom(StyledShape styledShape) {
        // as this is used as part of undo/redo, don't regenerate the shape
        regenerate = false;
        try {
            // the shape target cannot change for a styled shape edit
            typeModel.setSelectedItem(styledShape.getShapeType());
            fillPaintModel.setSelectedItem(styledShape.getFillPaint());
            strokePaintModel.setSelectedItem(styledShape.getStrokePaint());
            strokeParam.setState(styledShape.getStrokeSettings());
            effectsParam.setEffects(styledShape.getEffects());
        } finally {
            regenerate = true;
        }
    }

    public void addToDebugNode(DebugNode node) {
        node.addString("Type", getSelectedType().toString());
        node.addString("Target", getSelectedTarget().toString());
        node.addString("Fill", getSelectedFillPaint().toString());
        node.addString("Stroke", getSelectedStrokePaint().toString());
        strokeParam.addDebugNodeInfo(node);
    }
}

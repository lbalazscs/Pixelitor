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

package pixelitor.tools;

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.colors.FgBgColors;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.layers.Drawable;
import pixelitor.tools.brushes.Brush;
import pixelitor.tools.brushes.CopyBrushType;
import pixelitor.tools.brushes.SmudgeBrush;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Cursors;

import javax.swing.*;
import java.awt.Graphics2D;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static pixelitor.gui.utils.SliderSpinner.LabelPosition.WEST;

/**
 * The smudge tool.
 */
public class SmudgeTool extends AbstractBrushTool {
    private static final String FINGER_PAINTING_PRESET_KEY = "Finger Painting";

    private EnumComboBoxModel<CopyBrushType> brushModel;

    private final RangeParam strengthParam = new RangeParam("Strength", 1, 60, 100);
    private SmudgeBrush smudgeBrush;
    private JCheckBox fingerPaintingCB;

    public SmudgeTool() {
        super("Smudge", 'K',
            "<b>click and drag</b> to smudge. " +
                "<b>Click</b> and <b>Shift-click</b> to smudge along a line.",
            Cursors.HAND, false);

        drawTarget = DrawTarget.DIRECT;
    }

    @Override
    protected Brush createCoreBrush() {
        smudgeBrush = new SmudgeBrush(getRadius(), CopyBrushType.HARD);
        return smudgeBrush;
    }

    @Override
    public void initSettingsPanel(ResourceBundle resources) {
        brushModel = settingsPanel.addCopyBrushTypeSelector(
            CopyBrushType.HARD, smudgeBrush::typeChanged);

        addRadiusSelector();
        addStrengthSelector();
        addFingerPaintingSelector();

        settingsPanel.addSeparator();

        addLazyMouseDialogButton();
    }

    private void addStrengthSelector() {
        SliderSpinner strengthSelector = new SliderSpinner(
            strengthParam, WEST, false);
        settingsPanel.add(strengthSelector);
    }

    private void addFingerPaintingSelector() {
        fingerPaintingCB = new JCheckBox();
        settingsPanel.addWithLabel("Finger Painting:", fingerPaintingCB, "fingerPaintingCB");
        fingerPaintingCB.setName("fingerPaintingCB");
        fingerPaintingCB.addActionListener(
            _ -> smudgeBrush.setFingerPainting(fingerPaintingCB.isSelected()));
    }

    @Override
    protected void strokeStarting(Drawable dr, PPoint start, boolean lineConnect) {
        smudgeBrush.setSourceImage(dr.getCanvasSizedSubImage());
        if (!lineConnect) {
            smudgeBrush.initStroke(start, (float) strengthParam.getPercentage());
        }
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        super.saveStateTo(preset);

        preset.put(CopyBrushType.PRESET_KEY, brushModel.getSelectedItem().name());
        strengthParam.saveStateTo(preset);
        preset.putBoolean(FINGER_PAINTING_PRESET_KEY, fingerPaintingCB.isSelected());
        FgBgColors.saveStateTo(preset);
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        super.loadUserPreset(preset);

        brushModel.setSelectedItem(preset.getEnum(CopyBrushType.PRESET_KEY, CopyBrushType.class));
        strengthParam.loadStateFrom(preset);
        fingerPaintingCB.setSelected(preset.getBoolean(FINGER_PAINTING_PRESET_KEY));
        FgBgColors.loadStateFrom(preset, false);
    }

    @Override
    public Consumer<Graphics2D> createIconPainter() {
        return ToolIcons::paintSmudgeIcon;
    }
}

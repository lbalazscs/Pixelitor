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

package pixelitor.tools;

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.colors.FgBgColors;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.layers.Drawable;
import pixelitor.tools.brushes.*;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Cursors;

import javax.swing.*;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static pixelitor.gui.utils.SliderSpinner.LabelPosition.WEST;

/**
 * The smudge tool.
 */
public class SmudgeTool extends AbstractBrushTool {
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
    protected void initBrushVariables() {
        smudgeBrush = new SmudgeBrush(getRadius(), CopyBrushType.HARD);
        affectedArea = new AffectedArea();
        brush = new AffectedAreaTracker(smudgeBrush, affectedArea);
    }

    @Override
    protected void updateLazyMouseEnabledState() {
        if (lazyMouseEnabled.isChecked()) {
            lazyMouseBrush = new LazyMouseBrush(smudgeBrush);
            brush = new AffectedAreaTracker(lazyMouseBrush, affectedArea);
            lazyMouse = true;
        } else {
            brush = new AffectedAreaTracker(smudgeBrush, affectedArea);
            lazyMouseBrush = null;
            lazyMouse = false;
        }
    }

    @Override
    public void initSettingsPanel(ResourceBundle resources) {
        brushModel = settingsPanel.addCopyBrushTypeSelector(
            CopyBrushType.HARD, smudgeBrush::typeChanged);

        addSizeSelector();
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
            e -> smudgeBrush.setFingerPainting(fingerPaintingCB.isSelected()));
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        Drawable dr = e.getComp().getActiveDrawableOrThrow();

        // We could also pass the full image and the translation
        // and the smudge brush could always adjust the last sampling point
        // with the translation.
        BufferedImage sourceImage = dr.getCanvasSizedSubImage();

        // initialize the smudge brush state before starting the stroke
        boolean lineConnect = e.isShiftDown() && smudgeBrush.isStrokeInitialized();
        if (!lineConnect) {
            // initialize source image, start point, and strength for a new stroke segment
            initStroke(sourceImage, e);
        }
        // else: for shift-click line connect, reuse the existing source/strength

        super.mousePressed(e);
    }

    private void initStroke(BufferedImage sourceImage, PPoint startPoint) {
        smudgeBrush.initStroke(sourceImage, startPoint, (float) strengthParam.getPercentage());
    }

    @Override
    protected Symmetry getSymmetry() {
        throw new UnsupportedOperationException("no symmetry");
    }

    @Override
    protected void prepareProgrammaticBrushStroke(Drawable dr, PPoint start) {
        super.prepareProgrammaticBrushStroke(dr, start);

        BufferedImage sourceImg = dr.getCanvasSizedSubImage();
        initStroke(sourceImg, start);
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        super.saveStateTo(preset);

        preset.put("Brush Type", brushModel.getSelectedItem().toString());
        strengthParam.saveStateTo(preset);
        preset.putBoolean("Finger Painting", fingerPaintingCB.isSelected());
        FgBgColors.saveStateTo(preset);
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        super.loadUserPreset(preset);

        brushModel.setSelectedItem(preset.getEnum("Brush Type", CopyBrushType.class));
        strengthParam.loadStateFrom(preset);
        fingerPaintingCB.setSelected(preset.getBoolean("Finger Painting"));
        FgBgColors.loadStateFrom(preset);
    }

    @Override
    public Consumer<Graphics2D> createIconPainter() {
        return ToolIcons::paintSmudgeIcon;
    }
}

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

package pixelitor.tools;

import pixelitor.ImageDisplay;
import pixelitor.filters.gui.RangeParam;
import pixelitor.tools.brushes.BrushAffectedArea;
import pixelitor.tools.brushes.CopyBrushType;
import pixelitor.tools.brushes.SmudgeBrush;
import pixelitor.utils.SliderSpinner;

import javax.swing.*;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static pixelitor.utils.SliderSpinner.TextPosition.WEST;

/**
 * The Smudge Tool
 */
public class SmudgeTool extends DirectBrushTool {
    public SmudgeTool() {
        super('u', "Smudge", "smudge_tool_icon.png",
                "click and drag to smudge. Click and Shift-click to smudge along a line.", Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private final RangeParam strengthParam = new RangeParam("Strength", 1, 100, 60);
    private SmudgeBrush smudgeBrush;
    private JCheckBox fingerPaintingCB;

    @Override
    protected void initBrushVariables() {
        smudgeBrush = new SmudgeBrush(getRadius(), CopyBrushType.HARD);
        brush = new BrushAffectedArea(smudgeBrush);
        brushAffectedArea = (BrushAffectedArea) brush;
    }

    @Override
    public void initSettingsPanel() {
        settingsPanel.addCopyBrushTypeSelector(
                CopyBrushType.HARD,
                smudgeBrush::typeChanged);

        addSizeSelector();
        addStrengthSelector();
        addFingerPaintingSelector();
    }

    private void addStrengthSelector() {
        SliderSpinner strengthSelector = new SliderSpinner(strengthParam, WEST, false);
        settingsPanel.add(strengthSelector);
    }

    private void addFingerPaintingSelector() {
        fingerPaintingCB = new JCheckBox();
        settingsPanel.addWithLabel("Finger Painting:", fingerPaintingCB);
        fingerPaintingCB.setName("fingerPaintingCB");
        fingerPaintingCB.addActionListener(
                e -> smudgeBrush.setFingerPainting(fingerPaintingCB.isSelected()));
    }

    @Override
    public void mousePressed(MouseEvent e, ImageDisplay ic) {
        BufferedImage sourceImage = ic.getComp().getActiveImageLayer().getImage();
        int x = userDrag.getStartX();
        int y = userDrag.getStartY();
        if (!e.isShiftDown()) { // not a line-click
            smudgeBrush.setSource(sourceImage, x, y, strengthParam.getValueAsPercentage());
        }
        super.mousePressed(e, ic);
    }
}

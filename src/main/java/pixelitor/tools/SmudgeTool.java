/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.layers.Drawable;
import pixelitor.tools.brushes.AffectedArea;
import pixelitor.tools.brushes.AffectedAreaTracker;
import pixelitor.tools.brushes.CopyBrushType;
import pixelitor.tools.brushes.LazyMouseBrush;
import pixelitor.tools.brushes.SmudgeBrush;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Cursors;

import javax.swing.*;
import java.awt.image.BufferedImage;

import static pixelitor.gui.utils.SliderSpinner.TextPosition.WEST;

/**
 * The Smudge Tool
 */
public class SmudgeTool extends AbstractBrushTool {
    public SmudgeTool() {
        super("Smudge", 'K', "smudge_tool_icon.png",
                "<b>click and drag</b> to smudge. " +
                        "<b>Click</b> and <b>Shift-click</b> to smudge along a line.",
            Cursors.HAND, false);

        drawDestination = DrawDestination.DIRECT;
    }

    private final RangeParam strengthParam = new RangeParam("Strength", 1, 60, 100);
    private SmudgeBrush smudgeBrush;
    private JCheckBox fingerPaintingCB;

    @Override
    protected void initBrushVariables() {
        smudgeBrush = new SmudgeBrush(getRadius(), CopyBrushType.HARD);
        affectedArea = new AffectedArea();
        brush = new AffectedAreaTracker(smudgeBrush, affectedArea);
    }

    @Override
    protected void setLazyBrush() {
        if (lazyMouseCB.isSelected()) {
            lazyMouseBrush = new LazyMouseBrush(smudgeBrush);
            brush = new AffectedAreaTracker(
                    lazyMouseBrush,
                    affectedArea);
            lazyMouse = true;
        } else {
            brush = new AffectedAreaTracker(smudgeBrush, affectedArea);
            lazyMouseBrush = null;
            lazyMouse = false;
        }
    }

    @Override
    public void initSettingsPanel() {
        settingsPanel.addCopyBrushTypeSelector(
                CopyBrushType.HARD,
                smudgeBrush::typeChanged);

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

        if (e.isShiftDown()) {
            // if we *start* with a shift-click, then we must initialize,
            // but otherwise don't re-initialize for shift-clicks
            if (!smudgeBrush.firstPointWasInitialized()) {
                initStroke(sourceImage, e);
            }
        } else {
            initStroke(sourceImage, e);
        }

        super.mousePressed(e);
    }

    private void initStroke(BufferedImage sourceImage, PPoint p) {
        smudgeBrush.setupFirstPoint(sourceImage, p, strengthParam.getValueAsPercentage());
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
}

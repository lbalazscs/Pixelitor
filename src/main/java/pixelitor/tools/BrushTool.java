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

package pixelitor.tools;

import pixelitor.colors.ColorUtils;
import pixelitor.colors.FgBgColors;
import pixelitor.layers.Drawable;
import pixelitor.utils.Cursors;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;

/**
 * The brush tool
 */
public class BrushTool extends BlendingModeBrushTool {
    private Color drawingColor;

    public BrushTool() {
        super('b', "Brush", "brush_tool_icon.png",
                "<b>click</b> or <b>drag</b> to draw with the current brush, <b>Shift-click</b> to draw lines, <b>right-click</b> or <b>right-drag</b> to draw with the background color.",
                Cursors.CROSSHAIR
        );
    }

    @Override
    public void initSettingsPanel() {
        addTypeSelector();
        addBrushSettingsButton();
        settingsPanel.addSeparator();
        addSizeSelector();
        addSymmetryCombo();

        settingsPanel.addSeparator();

        addBlendingModePanel();
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        setupDrawingColor(e);
        super.mousePressed(e);
    }

    @Override
    protected void initializeGraphics(Graphics2D g) {
        // reinitialize the color for each stroke
        g.setColor(drawingColor);
    }

    @Override
    protected void prepareProgrammaticBrushStroke(Drawable dr, PPoint start) {
        super.prepareProgrammaticBrushStroke(dr, start);
        graphics.setColor(FgBgColors.getFG());
    }

    private void setupDrawingColor(PMouseEvent e) {
        if (e.isRight()) {
            drawingColor = FgBgColors.getBG();
        } else if (e.isMiddle()) {
            // TODO we never get here because isAltDown is always true for middle-button events, even if Alt is not pressed?
            // See source comment in java.awt.Event for ALT_MASK
            Color fg = FgBgColors.getFG();
            Color bg = FgBgColors.getBG();
            if (e.isControlDown()) {
                drawingColor = ColorUtils.calcHSBAverage(fg, bg);
            } else {
                drawingColor = ColorUtils.calcRGBAverage(fg, bg);
            }
        } else {
            drawingColor = FgBgColors.getFG();
        }
    }

    @Override
    public void trace(Drawable dr, Shape shape) {
        drawingColor = FgBgColors.getFG();
        super.trace(dr, shape);
    }
}
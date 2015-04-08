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

import pixelitor.Composition;
import pixelitor.ImageDisplay;
import pixelitor.layers.ImageLayer;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 * The brush tool
 */
public class BrushTool extends TmpLayerBrushTool {
    private Color drawingColor;

    public BrushTool() {
        super('b', "Brush", "brush_tool_icon.gif", "click and drag to draw with the current brush, Shift-click to draw lines, right-click to draw with the background color");
    }

    @Override
    public void initSettingsPanel() {

        super.initSettingsPanel();

        toolSettingsPanel.addSeparator();

        addBlendingModePanel();
    }

    @Override
    public void mousePressed(MouseEvent e, ImageDisplay ic) {
        setupDrawingColor(e);
        super.mousePressed(e, ic);
    }

    @Override
    protected void createGraphics(Composition comp, ImageLayer layer) {
        super.createGraphics(comp, layer);

        // reinitialize the color for each stroke
        graphics.setColor(drawingColor);
    }

    @Override
    public void drawBrushStrokeProgrammatically(Composition comp, Point startingPoint, Point endPoint) {
        graphics.setColor(FgBgColorSelector.getFG());

        super.drawBrushStrokeProgrammatically(comp, startingPoint, endPoint);

        comp.getActiveImageLayer().mergeTmpDrawingImageDown();
    }

    private void setupDrawingColor(MouseEvent e) {
        int button = e.getButton();

        if(button == MouseEvent.BUTTON3) {
            drawingColor = FgBgColorSelector.getBG();
        } else if(button == MouseEvent.BUTTON2) {
            // TODO we never get here because isAltDown is always true for middle-button events, even if Alt is not pressed?
            Color fg = FgBgColorSelector.getFG();
            Color bg = FgBgColorSelector.getBG();
            if(e.isControlDown()) {
                drawingColor = ImageUtils.getHSBAverageColor(fg, bg);
            } else {
                drawingColor = ImageUtils.getRGBAverageColor(fg, bg);
            }
        } else {
            drawingColor = FgBgColorSelector.getFG();
        }
    }
}
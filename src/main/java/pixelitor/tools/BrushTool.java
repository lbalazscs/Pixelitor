/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.tools;

import pixelitor.Composition;
import pixelitor.layers.ImageLayer;
import pixelitor.utils.BlendingModePanel;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.image.BufferedImage;

/**
 * The brush tool
 */
public class BrushTool extends AbstractBrushTool {
    private BlendingModePanel blendingModePanel;

    public BrushTool() {
        super('b', "Brush", "brush_tool_icon.gif", "click and drag to draw with the current brush, Shift-click to draw lines, right-click to draw with the background color");
    }

    @Override
    public void initSettingsPanel() {

        super.initSettingsPanel();

        toolSettingsPanel.addSeparator();

        blendingModePanel = new BlendingModePanel(true);
        toolSettingsPanel.add(blendingModePanel);
    }

    @Override
    protected void initDrawingGraphics(ImageLayer layer) {
        g = layer.createTmpDrawingLayer(blendingModePanel.getComposite(), respectSelection).getGraphics();
        brushes.setDrawingGraphics(g);
    }

    @Override
    public void setupGraphics(Graphics2D g, Paint p) {
        if (p != null) {
            g.setPaint(p);
        } else {  // can happen, if multiple mouse buttons are pressed, and there is a mouse up event during dragging
            g.setColor(FgBgColorSelector.getFG());
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }


    @Override
    void mergeTmpLayer(Composition comp) {
        if (g != null) {
            ImageLayer imageLayer = comp.getActiveImageLayer();
            imageLayer.mergeTmpDrawingImageDown();
        }
    }

    @Override
    public void drawBrushStrokeProgrammatically(Composition comp, Point startingPoint, Point endPoint) {
        super.drawBrushStrokeProgrammatically(comp, startingPoint, endPoint);
        comp.getActiveImageLayer().mergeTmpDrawingImageDown();
    }

    @Override
    BufferedImage getFullUntouchedImage(Composition comp) {
        BufferedImage retVal = comp.getActiveImageLayer().getBufferedImage();
        if (retVal == null) {
            throw new IllegalStateException();
        }
        return retVal;
    }
}
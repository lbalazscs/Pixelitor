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
import pixelitor.layers.ImageLayer;
import pixelitor.utils.BlendingModePanel;
import pixelitor.utils.ImageUtils;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * The brush tool
 */
public class BrushTool extends AbstractBrushTool {
    protected BlendingModePanel blendingModePanel;

    public BrushTool() {
        super('b', "Brush", "brush_tool_icon.gif", "click and drag to draw with the current brush, Shift-click to draw lines, right-click to draw with the background color");
    }

    // for subclasses
    protected BrushTool(char activationKeyChar, String name, String iconFileName, String toolMessage) {
        super(activationKeyChar, name, iconFileName, toolMessage);
    }

    @Override
    public void initSettingsPanel() {

        super.initSettingsPanel();

        toolSettingsPanel.addSeparator();

        blendingModePanel = new BlendingModePanel(true);
        toolSettingsPanel.add(blendingModePanel);
    }

    @Override
    protected void initDrawingGraphics(Composition comp, ImageLayer layer) {
        drawingGraphics = layer.createTmpDrawingLayer(blendingModePanel.getComposite(), respectSelection).getGraphics();
        brush.setTarget(comp, drawingGraphics);
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
        if(drawingGraphics != null) {
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
        BufferedImage retVal = comp.getActiveImageLayer().getImage();
        if (retVal == null) {
            throw new IllegalStateException();
        }
        return retVal;
    }

    @Override
    protected Paint getPaint(MouseEvent e) {
        Paint p;
        int button = e.getButton();

        if(button == MouseEvent.BUTTON3) {
            p = FgBgColorSelector.getBG();
        } else if(button == MouseEvent.BUTTON2) {
            // we never get here because isAltDown is always true for middle-button events, even if Alt is not pressed
            Color fg = FgBgColorSelector.getFG();
            Color bg = FgBgColorSelector.getBG();
            if(e.isControlDown()) {
                p = ImageUtils.getHSBAverageColor(fg, bg);
            } else {
                p = ImageUtils.getRGBAverageColor(fg, bg);
            }
        } else {
            p = FgBgColorSelector.getFG();
        }
        return p;
    }
}
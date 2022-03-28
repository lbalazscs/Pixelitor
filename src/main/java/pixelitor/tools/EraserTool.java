/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.utils.Cursors;

import javax.swing.*;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

import static java.awt.AlphaComposite.DST_OUT;
import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_MITER;

/**
 * The eraser tool.
 */
public class EraserTool extends AbstractBrushTool {
    public EraserTool() {
        super("Eraser", 'E',
            "<b>click and drag</b> to erase pixels. <b>Shift-click</b> to erase lines.",
            Cursors.CROSSHAIR, true);
        drawDestination = DrawDestination.DIRECT;
    }

    @Override
    public void initSettingsPanel() {
        addTypeSelector();
        addBrushSettingsButton();

        settingsPanel.addSeparator();
        addSizeSelector();
        addSymmetryCombo();
        addLazyMouseDialogButton();
    }

    @Override
    protected void initGraphics(Graphics2D g) {
        // the color does not matter as long as AlphaComposite.DST_OUT is used
        g.setComposite(AlphaComposite.getInstance(DST_OUT));
    }

    @Override
    public Icon createIcon() {
        return new EraserToolIcon();
    }

    private static class EraserToolIcon extends Tool.ToolIcon {
        @Override
        public void paintIcon(Graphics2D g) {
            // based on eraser_tool.svg

            // thick
            Path2D shape = new Path2D.Float();
            shape.moveTo(1.8047796, 10.327721);
            shape.lineTo(1.8047796, 18.188522);
            shape.lineTo(10.771456, 24.084148);
            shape.lineTo(26.34516, 15.24072);
            shape.lineTo(26.34516, 7.871212);
            shape.lineTo(17.850412, 4.432114);
            shape.closePath();

            g.setStroke(new BasicStroke(1.5f, CAP_BUTT, JOIN_MITER, 4));
            g.draw(shape);

            // thin
            shape = new Path2D.Float();
            shape.moveTo(10.771456, 24.084148);
            shape.lineTo(10.771456, 15.193945);
            shape.lineTo(1.332849, 10.327721);
            shape.lineTo(10.771456, 15.193945);
            shape.lineTo(26.34516, 7.871212);

            g.setStroke(new BasicStroke(1.0f, CAP_BUTT, JOIN_MITER, 4));
            g.draw(shape);
        }
    }
}
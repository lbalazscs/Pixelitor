/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.utils.VectorIcon;
import pixelitor.utils.Cursors;

import java.awt.Graphics2D;
import java.awt.geom.Path2D;

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
    protected void initBrushStroke() {
        brushStroke.setTransparentDrawing();
    }

    @Override
    public VectorIcon createIcon() {
        return new EraserToolIcon();
    }

    private static class EraserToolIcon extends ToolIcon {
        @Override
        public void paintIcon(Graphics2D g) {
            // based on eraser_tool.svg
            Path2D shape = new Path2D.Double();

            shape.moveTo(16.78125, 1.6210938);
            shape.curveTo(15.8157015, 1.6210741, 14.889705, 2.004681, 14.207031, 2.6875);
            shape.lineTo(14.205078, 2.6875);
            shape.lineTo(1.6855469, 15.207031);
            shape.curveTo(0.2648747, 16.628397, 0.2648747, 18.93215, 1.6855469, 20.353516);
            shape.lineTo(6.236328, 24.904297);
            shape.curveTo(6.9190016, 25.587116, 7.8449984, 25.970722, 8.810547, 25.970703);
            shape.lineTo(13.830078, 25.970703);
            shape.curveTo(13.835288, 25.970064, 13.840496, 25.969412, 13.845703, 25.96875);
            shape.lineTo(22.435547, 25.96875);
            shape.curveTo(22.924255, 25.96875, 23.316406, 25.574646, 23.316406, 25.085938);
            shape.lineTo(23.316406, 25.064453);
            shape.curveTo(23.316406, 24.575745, 22.924255, 24.18164, 22.435547, 24.18164);
            shape.lineTo(17.126953, 24.18164);
            shape.lineTo(26.414062, 14.894531);
            shape.curveTo(27.096909, 14.21187, 27.480543, 13.2858715, 27.480543, 12.3203125);
            shape.curveTo(27.480543, 11.3547535, 27.096909, 10.428755, 26.414062, 9.746094);
            shape.lineTo(19.355469, 2.6875);
            shape.curveTo(18.672796, 2.004681, 17.7468, 1.6210741, 16.78125, 1.6210938);
            shape.closePath();

            shape.moveTo(16.78125, 3.4414062);
            shape.curveTo(17.264025, 3.4413965, 17.727022, 3.6332, 18.06836, 3.9746094);
            shape.lineTo(25.126953, 11.033203);
            shape.curveTo(25.468376, 11.374534, 25.660192, 11.837533, 25.660192, 12.3203125);
            shape.curveTo(25.660192, 12.803092, 25.468376, 13.266091, 25.126953, 13.607422);
            shape.lineTo(16.695312, 22.039062);
            shape.lineTo(7.0625, 12.40625);
            shape.lineTo(15.494141, 3.9746094);
            shape.curveTo(15.835477, 3.6332, 16.298475, 3.4413965, 16.78125, 3.4414062);
            shape.closePath();

            shape.moveTo(5.7753906, 13.693359);
            shape.lineTo(15.408203, 23.326172);
            shape.lineTo(15.119141, 23.617188);
            shape.curveTo(14.777804, 23.958597, 14.314806, 24.1504, 13.832031, 24.15039);
            shape.lineTo(8.810547, 24.15039);
            shape.curveTo(8.328449, 24.149883, 7.8662486, 23.958118, 7.5253906, 23.617188);
            shape.lineTo(2.9746094, 19.066406);
            shape.curveTo(2.2649238, 18.355835, 2.2649238, 17.204712, 2.9746094, 16.49414);
            shape.lineTo(5.7753906, 13.693359);
            shape.closePath();

            g.fill(shape);
        }
    }
}
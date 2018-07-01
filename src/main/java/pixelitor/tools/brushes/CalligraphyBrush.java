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

package pixelitor.tools.brushes;

import pixelitor.tools.shapes.StrokeType;
import pixelitor.tools.util.PPoint;

import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.geom.Line2D;

/**
 * The calligraphy brush based on CalligraphyStroke
 */
public class CalligraphyBrush extends StrokeBrush {
    private static final Stroke pointStroke = new BasicStroke(1.2f);

    public CalligraphyBrush(int radius) {
        super(radius, StrokeType.CALLIGRAPHY);
    }

    @Override
    protected void drawStartShape(PPoint p) {
        // TODO these calculations could be simpler

        float projectedShift = diameter / 1.4142f;
        float projectedStart = (diameter - projectedShift) / 2.0f;
        float projectedEnd = projectedStart + projectedShift;

        double x = p.getImX();
        double y = p.getImY();

        double startX = x + projectedStart - radius;
        double startY = y + projectedEnd - radius;
        double endX = x + projectedEnd - radius;
        double endY = y + projectedStart - radius;

        targetG.setStroke(pointStroke);

        // for some reasons (rounding errors previously?) these ones have to be added and subtracted
//        targetG.drawLine(startX + 1, startY - 1, endX - 1, endY + 1);
        // TODO is this necessary after the PPoint refactoring?
        Line2D.Double line = new Line2D.Double(startX + 1, startY - 1, endX - 1, endY + 1);
        targetG.draw(line);

        if (currentStroke != null) {
            targetG.setStroke(currentStroke);
        }
    }

    @Override
    public void drawLine(PPoint start, PPoint end) {
        super.drawLine(start, end);

        // for some reason this must be called, otherwise gaps remain
        // TODO still true?
        drawStartShape(start);
    }
}

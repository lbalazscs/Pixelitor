/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import pixelitor.tools.StrokeType;

import java.awt.BasicStroke;
import java.awt.Stroke;

/**
 *
 */
public class CalligraphyBrush extends StrokeBrush {
    private static final Stroke pointStroke = new BasicStroke(1.2f);

    public CalligraphyBrush(int radius) {
        super(radius, StrokeType.CALLIGRAPHY);
    }

    @Override
    protected void drawShape(double x, double y) {
        // TODO these calculations could be simpler

        float projectedShift = diameter / 1.4142f;
        float projectedStart = (diameter - projectedShift) / 2.0f;
        float projectedEnd = projectedStart + projectedShift;

        int startX = (int) (x + projectedStart) - radius;
        int startY = (int) (y + projectedEnd) - radius;
        int endX = (int) (x + projectedEnd) - radius;
        int endY = (int) (y + projectedStart) - radius;

        targetG.setStroke(pointStroke);

        // for some reasons (rounding errors previously?) these ones have to be added and subtracted
        targetG.drawLine(startX + 1, startY - 1, endX - 1, endY + 1);

        if (currentStroke != null) {
            targetG.setStroke(currentStroke);
        }
    }

    @Override
    public void drawLine(double startX, double startY, double endX, double endY) {
        super.drawLine(startX, startY, endX, endY);

        // for some reason this must be called, otherwise gaps remain
        // TODO still true?
        drawShape(startX, startY);
    }
}

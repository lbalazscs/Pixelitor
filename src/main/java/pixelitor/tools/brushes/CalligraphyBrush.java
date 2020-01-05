/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import com.bric.awt.CalligraphyStroke;
import pixelitor.tools.shapes.StrokeType;
import pixelitor.tools.util.PPoint;

import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.geom.Line2D;

/**
 * The calligraphy brush based on {@link CalligraphyStroke}
 */
public class CalligraphyBrush extends StrokeBrush {
    private static final Stroke pointStroke = new BasicStroke(2.0f);
    private final CalligraphyBrushSettings settings;

    public CalligraphyBrush(double radius, CalligraphyBrushSettings settings) {
        super(radius, StrokeType.CALLIGRAPHY);
        this.settings = settings;
    }

    @Override
    public void startAt(PPoint p) {
        currentStroke = createStroke((float) (2 * radius));

        super.startAt(p);
    }

    @Override
    protected Stroke createStroke(float thickness) {
        double angle = settings.getAngle();
        return new CalligraphyStroke(thickness, (float) angle);
    }

    @Override
    protected void drawStartShape(PPoint p) {
        double angle = settings.getAngle();

        // 2.0 is an experimentally found value for the best gap-filling...
        double dx = (radius - 2.0) * Math.cos(angle);
        double dy = (radius - 2.0) * Math.sin(angle);

        double x = p.getImX();
        double y = p.getImY();

        double startX = x + dx;
        double startY = y + dy;
        double endX = x - dx;
        double endY = y - dy;

        targetG.setStroke(pointStroke);

        var line = new Line2D.Double(startX, startY, endX, endY);

        targetG.draw(line);

        if (currentStroke != null) {
            targetG.setStroke(currentStroke);
        }
    }

    @Override
    public void drawLine(PPoint start, PPoint end) {
        super.drawLine(start, end);

        // for some reason this must be called, otherwise gaps remain
        drawStartShape(start);
    }
}

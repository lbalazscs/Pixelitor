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

package pixelitor.utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * Static shape-related utility methods
 */
public class Shapes {
    private static final Stroke BIG_STROKE = new BasicStroke(3);
    private static final Stroke SMALL_STROKE = new BasicStroke(1);

    private Shapes() {
        // do not instantiate
    }

    public static void drawVisible(Graphics2D g, Shape shape) {
        assert shape != null;
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        // black at the edges
        g.setStroke(BIG_STROKE);
        g.setColor(BLACK);
        g.draw(shape);

        // white in the middle
        g.setStroke(SMALL_STROKE);
        g.setColor(WHITE);
        g.draw(shape);
    }

    public static void fillVisible(Graphics2D g, Shape shape) {
        assert shape != null;

        fillVisible(g, shape, Color.WHITE);
    }

    public static void fillVisible(Graphics2D g, Shape shape, Color c) {
        assert shape != null;

        // black at the edges
        g.setStroke(BIG_STROKE);
        g.setColor(BLACK);
        g.draw(shape);

        // the given color in the middle
        g.setStroke(SMALL_STROKE);
        g.setColor(c);
        g.fill(shape);
    }

    public static void drawGradientArrow(Graphics2D g,
                                         double startX, double startY,
                                         double endX, double endY) {
        Line2D line = new Line2D.Double(startX, startY, endX, endY);
        Shapes.drawVisible(g, line);

        double angle = Math.atan2(endY - startY, endX - startX);

        double backAngle1 = 2.8797926 + angle;
        double backAngle2 = 3.4033926 + angle;
        int arrowRadius = 20;

        double arrowEnd1X = endX + (arrowRadius * Math.cos(backAngle1));
        double arrowEnd1Y = endY + (arrowRadius * Math.sin(backAngle1));
        double arrowEnd2X = endX + (arrowRadius * Math.cos(backAngle2));
        double arrowEnd2Y = endY + (arrowRadius * Math.sin(backAngle2));

        GeneralPath.Double arrowHead = new Path2D.Double();
        arrowHead.moveTo(endX, endY);
        arrowHead.lineTo(arrowEnd1X, arrowEnd1Y);
        arrowHead.lineTo(arrowEnd2X, arrowEnd2Y);
        arrowHead.closePath();
        Shapes.fillVisible(g, arrowHead);
    }
}

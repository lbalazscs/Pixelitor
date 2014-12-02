/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
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
package pixelitor.filters.gui;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Line2D;

/**
 * An abstract superclass for angle selectors and elevation angle selectors
 */
public abstract class AbstractAngleSelectorComponent extends JComponent implements MouseListener, MouseMotionListener {
    AngleParam model;
    static final int SIZE = 50;
    static final Stroke ARROW_STROKE = new BasicStroke(1.7f);
//    static final Color ARROW_COLOR = new Color(48, 76, 111);
    static final Color ARROW_COLOR = new Color(45, 66, 85);

    int cx;
    int cy;

    AbstractAngleSelectorComponent(AngleParam angleParam) {
        this.model = angleParam;

        Dimension sizeDim = new Dimension(SIZE + 1, SIZE + 1);
        setSize(sizeDim);
        setMinimumSize(sizeDim);
        setPreferredSize(sizeDim);

        addMouseListener(this);
        addMouseMotionListener(this);
    }

    private void updateAngle(int x, int y, boolean trigger) {
        double angle = Math.atan2(y - cy, x - cx);
        repaint();
        model.setValueInRadians(angle, trigger);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // will be handled in mouseReleased
    }

    @Override
    public void mousePressed(MouseEvent e) {
        updateAngle(e.getX(), e.getY(), false);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        updateAngle(e.getX(), e.getY(), true);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        updateAngle(e.getX(), e.getY(), false);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    static void drawArrow(Graphics2D g2, double angle, float startX, float startY, float endX, float endY) {
        g2.setColor(ARROW_COLOR);
        g2.setStroke(ARROW_STROKE);

        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        Line2D.Float line = new Line2D.Float(startX, startY, endX, endY);
        g2.draw(line);

        double backAngle1 = 2.8797926 + angle;
        double backAngle2 = 3.4033926 + angle;
        int arrowRadius = 10;

        float arrowEnd1X = (float) (endX + (arrowRadius * Math.cos(backAngle1)));
        float arrowEnd1Y = (float) (endY + (arrowRadius * Math.sin(backAngle1)));
        float arrowEnd2X = (float) (endX + (arrowRadius * Math.cos(backAngle2)));
        float arrowEnd2Y = (float) (endY + (arrowRadius * Math.sin(backAngle2)));

        Line2D.Float line1 = new Line2D.Float(endX, endY, arrowEnd1X, arrowEnd1Y);
        Line2D.Float line2 = new Line2D.Float(endX, endY, arrowEnd2X, arrowEnd2Y);

        g2.draw(line1);
        g2.draw(line2);
    }

}

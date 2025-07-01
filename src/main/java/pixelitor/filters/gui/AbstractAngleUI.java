/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Line2D;

import static java.awt.Color.BLACK;
import static java.awt.Color.GRAY;
import static java.awt.RenderingHints.KEY_STROKE_CONTROL;
import static java.awt.RenderingHints.VALUE_STROKE_PURE;

/**
 * Abstract base class for angle selectors ({@link AngleUI})
 * and elevation angle selectors ({@link ElevationAngleUI})
 */
public abstract class AbstractAngleUI extends JComponent implements MouseListener, MouseMotionListener {
    protected static final int SELECTOR_SIZE = 50;
    private static final Stroke ARROW_STROKE = new BasicStroke(1.7f);
    private static final Color ENABLED_ARROW_COLOR = new Color(45, 66, 85);
    private static final Color DISABLED_ARROW_COLOR = new Color(160, 160, 160);
    private static final Color ENABLED_ARROW_DARK_COLOR = new Color(181, 181, 181);
    private static final Color DISABLED_ARROW_DARK_COLOR = new Color(95, 95, 95);

    protected final AngleParam model;
    private boolean enabled = true;

    protected int centerX;
    protected int centerY;

    AbstractAngleUI(AngleParam model) {
        this.model = model;

        var size = new Dimension(SELECTOR_SIZE + 1, SELECTOR_SIZE + 1);
        setSize(size);
        setMinimumSize(size);
        setPreferredSize(size);

        addMouseListener(this);
        addMouseMotionListener(this);
    }

    void setupOuterColor(Graphics2D g, boolean darkTheme) {
        if (enabled) {
            g.setColor(darkTheme
                ? ENABLED_ARROW_DARK_COLOR
                : BLACK);
        } else {
            g.setColor(darkTheme
                ? DISABLED_ARROW_DARK_COLOR
                : GRAY);
        }
    }

    void setupArrowColor(Graphics2D g, boolean darkTheme) {
        if (enabled) {
            g.setColor(darkTheme
                ? ENABLED_ARROW_DARK_COLOR
                : ENABLED_ARROW_COLOR);
        } else {
            g.setColor(darkTheme
                ? DISABLED_ARROW_DARK_COLOR
                : DISABLED_ARROW_COLOR);
        }
    }

    static void drawArrow(Graphics2D g2, double angle, float startX, float startY, float endX, float endY) {
        g2.setStroke(ARROW_STROKE);
        g2.setRenderingHint(KEY_STROKE_CONTROL, VALUE_STROKE_PURE);

        // draw main arrow line
        g2.draw(new Line2D.Float(startX, startY, endX, endY));

        // the arrowhead angles are calculated relative to the main arrow's angle
        double backAngle1 = 2.8797926 + angle;
        double backAngle2 = 3.4033926 + angle;
        int arrowRadius = 10;

        // draw the first line of the arrow head
        float arrowEnd1X = (float) (endX + arrowRadius * Math.cos(backAngle1));
        float arrowEnd1Y = (float) (endY + arrowRadius * Math.sin(backAngle1));
        g2.draw(new Line2D.Float(endX, endY, arrowEnd1X, arrowEnd1Y));

        // draw the second line of the arrow head
        float arrowEnd2X = (float) (endX + arrowRadius * Math.cos(backAngle2));
        float arrowEnd2Y = (float) (endY + arrowRadius * Math.sin(backAngle2));
        g2.draw(new Line2D.Float(endX, endY, arrowEnd2X, arrowEnd2Y));
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        super.setEnabled(enabled);
        repaint();
    }

    private void updateAngle(int mouseX, int mouseY, boolean trigger) {
        if (!enabled) {
            return;
        }
        double angle = Math.atan2(mouseY - centerY, mouseX - centerX);

        // try to update the selector UI before a potential execution of the filter
        repaint();

        model.setValue(angle, trigger);
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
}

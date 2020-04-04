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

package pixelitor.filters.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Line2D;

import static java.awt.Color.BLACK;
import static java.awt.RenderingHints.KEY_STROKE_CONTROL;
import static java.awt.RenderingHints.VALUE_STROKE_PURE;

/**
 * An abstract superclass for angle selectors ({@link AngleUI})
 * and elevation angle selectors ({@link ElevationAngleUI})
 */
public abstract class AbstractAngleUI extends JComponent implements MouseListener, MouseMotionListener {
    protected static final int SIZE = 50;
    private static final Stroke ARROW_STROKE = new BasicStroke(1.7f);
    protected static final Color ENABLED_ARROW_COLOR = new Color(45, 66, 85);
    protected static final Color DISABLED_ARROW_COLOR = new Color(160, 160, 160);
    private static final Color ENABLED_ARROW_DARK_COLOR = new Color(181, 181, 181);
    private static final Color DISABLED_ARROW_DARK_COLOR = new Color(95, 95, 95);

    protected final AngleParam model;
    protected boolean enabled = true;

    protected int cx;
    protected int cy;

    AbstractAngleUI(AngleParam angleParam) {
        model = angleParam;

        var size2D = new Dimension(SIZE + 1, SIZE + 1);
        setSize(size2D);
        setMinimumSize(size2D);
        setPreferredSize(size2D);

        addMouseListener(this);
        addMouseMotionListener(this);
    }

    void setupOuterColor(Graphics2D g, boolean darkTheme) {
        if (enabled) {
            if (darkTheme) {
                g.setColor(ENABLED_ARROW_DARK_COLOR);
            } else {
                g.setColor(BLACK);
            }
        } else {
            if (darkTheme) {
                g.setColor(DISABLED_ARROW_DARK_COLOR);
            } else {
                g.setColor(Color.GRAY);
            }
        }
    }

    protected void setupArrowColor(Graphics2D g, boolean darkTheme) {
        if (enabled) {
            if (darkTheme) {
                g.setColor(ENABLED_ARROW_DARK_COLOR);
            } else {
                g.setColor(ENABLED_ARROW_COLOR);
            }
        } else {
            if (darkTheme) {
                g.setColor(DISABLED_ARROW_DARK_COLOR);
            } else {
                g.setColor(DISABLED_ARROW_COLOR);
            }
        }
    }

    static void drawArrow(Graphics2D g2, double angle, float startX, float startY, float endX, float endY) {
        g2.setStroke(ARROW_STROKE);

        g2.setRenderingHint(KEY_STROKE_CONTROL, VALUE_STROKE_PURE);

        g2.draw(new Line2D.Float(startX, startY, endX, endY));

        double backAngle1 = 2.8797926 + angle;
        double backAngle2 = 3.4033926 + angle;
        int arrowRadius = 10;

        float arrowEnd1X = (float) (endX + arrowRadius * Math.cos(backAngle1));
        float arrowEnd1Y = (float) (endY + arrowRadius * Math.sin(backAngle1));
        g2.draw(new Line2D.Float(endX, endY, arrowEnd1X, arrowEnd1Y));

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

    private void updateAngle(int x, int y, boolean trigger) {
        if (!enabled) {
            return;
        }
        double angle = Math.atan2(y - cy, x - cx);
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

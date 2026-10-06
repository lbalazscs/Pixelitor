/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

/**
 * A component that displays a portion of the complex plane and
 * allows users to select a complex number by clicking or dragging.
 */
public class ComplexNumberSelector extends JComponent {
    private static final Color BG_COLOR = new Color(248, 249, 250);
    private static final Color GRID_COLOR = new Color(225, 228, 232);
    private static final Color AXIS_COLOR = new Color(110, 115, 125);
    private static final Color BORDER_COLOR = Color.GRAY;
    private static final Font AXIS_LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private static final int LABEL_GAP = 4;
    private static final int TICK_HALF_LEN = 2;

    private final ComplexNumberParam model;
    private final double xMin;
    private final double xMax;
    private final double yMin;
    private final double yMax;

    public ComplexNumberSelector(ComplexNumberParam model) {
        this.model = Objects.requireNonNull(model);
        this.xMin = model.getMinRe();
        this.xMax = model.getMaxRe();
        this.yMin = model.getMinIm();
        this.yMax = model.getMaxIm();

        setPreferredSize(new Dimension(ComplexNumberParamGUI.DEFAULT_SIZE, ComplexNumberParamGUI.DEFAULT_SIZE));

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                updatePosition(e, true);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                updatePosition(e, true);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                updatePosition(e, false);
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    private void updatePosition(MouseEvent e, boolean isAdjusting) {
        if (!isEnabled()) {
            return;
        }

        int width = getWidth();
        int height = getHeight();

        // clamp mouse coordinates inside canvas bounds
        int clampedX = Math.clamp(e.getX(), 0, width - 1);
        int clampedY = Math.clamp(e.getY(), 0, height - 1);

        double real = pixelXToReal(clampedX, width);
        double imag = pixelYToImag(clampedY, height);

        model.setValue(real, imag, true, isAdjusting, true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();
        Graphics2D g2 = (Graphics2D) g.create();

        try {
            // background
            g2.setColor(isEnabled() ? BG_COLOR : Color.LIGHT_GRAY);
            g2.fillRect(0, 0, width, height);

            if (!isEnabled()) {
                g2.setColor(BORDER_COLOR);
                g2.drawRect(0, 0, width - 1, height - 1);
                return;
            }

            drawGrid(g2, width, height);

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            drawAxes(g2, height, width);
            drawLabelsAndTicks(g2, width, height);

            // paint outer border
            g2.setColor(BORDER_COLOR);
            g2.drawRect(0, 0, width - 1, height - 1);

            // paint crosshair indicator from model state
            int markerX = realToPixelX(model.getRe(), width);
            int markerY = imagToPixelY(model.getIm(), height);
            CrosshairPainter.paint(g2, markerX, markerY, width, height);
        } finally {
            g2.dispose();
        }
    }

    private void drawGrid(Graphics g, int width, int height) {
        // faint grid lines at integer coordinates
        g.setColor(GRID_COLOR);
        int startX = (int) Math.ceil(xMin);
        int endX = (int) Math.floor(xMax);
        int startY = (int) Math.ceil(yMin);
        int endY = (int) Math.floor(yMax);

        for (int i = startX; i <= endX; i++) {
            if (i != 0) { // axes drawn separately
                int px = realToPixelX(i, width);
                g.drawLine(px, 0, px, height - 1);
            }
        }

        for (int i = startY; i <= endY; i++) {
            if (i != 0) {
                int py = imagToPixelY(i, height);
                g.drawLine(0, py, width - 1, py);
            }
        }
    }

    private void drawAxes(Graphics g, int height, int width) {
        // real and imaginary axes with arrows
        int arrowLength = 7;
        int arrowHalfWidth = 3;

        // real axis (horizontal, y = 0)
        if (yMin <= 0 && yMax >= 0) {
            int y0 = imagToPixelY(0.0, height);
            g.setColor(AXIS_COLOR);
            g.drawLine(0, y0, width - 1, y0);

            // right arrow tip at positive end
            int[] xPoints = {width - 1, width - 1 - arrowLength, width - 1 - arrowLength};
            int[] yPoints = {y0, y0 - arrowHalfWidth, y0 + arrowHalfWidth};
            g.fillPolygon(xPoints, yPoints, 3);
        }

        // imaginary axis (vertical, x = 0)
        if (xMin <= 0 && xMax >= 0) {
            int x0 = realToPixelX(0.0, width);
            g.setColor(AXIS_COLOR);
            g.drawLine(x0, 0, x0, height - 1);

            // top arrow tip at positive end
            int[] xPoints = {x0, x0 - arrowHalfWidth, x0 + arrowHalfWidth};
            int[] yPoints = {0, arrowLength, arrowLength};
            g.fillPolygon(xPoints, yPoints, 3);
        }
    }

    private void drawLabelsAndTicks(Graphics g, int width, int height) {
        int startX = (int) Math.ceil(xMin);
        int endX = (int) Math.floor(xMax);
        int startY = (int) Math.ceil(yMin);
        int endY = (int) Math.floor(yMax);

        // tick marks and coordinate numbers at integer positions
        g.setFont(AXIS_LABEL_FONT);
        FontMetrics fm = g.getFontMetrics(AXIS_LABEL_FONT);

        // draw numbers along the real axis
        if (yMin <= 0 && yMax >= 0) {
            int y0 = imagToPixelY(0.0, height);
            for (int i = startX; i <= endX; i++) {
                if (i != 0) {
                    int px = realToPixelX(i, width);
                    g.drawLine(px, y0 - TICK_HALF_LEN, px, y0 + TICK_HALF_LEN); // tick mark

                    String text = Integer.toString(i);
                    int textWidth = fm.stringWidth(text);
                    int textX = Math.max(2, Math.min(width - textWidth - 2, px - textWidth / 2));
                    boolean fitsBelowAxis = (y0 + fm.getHeight() + LABEL_GAP) <= height - 2;
                    int textY = fitsBelowAxis
                        ? y0 + fm.getAscent() + LABEL_GAP
                        : y0 - LABEL_GAP;

                    g.drawString(text, textX, textY);
                }
            }
        }

        // draw numbers along the imaginary axis
        if (xMin <= 0 && xMax >= 0) {
            int x0 = realToPixelX(0.0, width);
            for (int i = startY; i <= endY; i++) {
                if (i != 0) {
                    int py = imagToPixelY(i, height);
                    g.drawLine(x0 - TICK_HALF_LEN, py, x0 + TICK_HALF_LEN, py); // tick mark

                    String text = Integer.toString(i);
                    int textWidth = fm.stringWidth(text);
                    boolean fitsLeftOfAxis = (x0 - textWidth - LABEL_GAP) >= 2;
                    int textX = fitsLeftOfAxis
                        ? x0 - textWidth - LABEL_GAP
                        : x0 + LABEL_GAP + 1;
                    int textY = Math.max(fm.getAscent() + 1, Math.min(height - fm.getDescent() - 2, py + fm.getAscent() / 2 - 1));

                    g.drawString(text, textX, textY);
                }
            }
        }
    }

    private int realToPixelX(double re, int width) {
        return (int) Math.round((re - xMin) / (xMax - xMin) * (width - 1));
    }

    private int imagToPixelY(double im, int height) {
        // invert Y axis: mathematical y increases upwards, screen y increases downwards
        return (int) Math.round((yMax - im) / (yMax - yMin) * (height - 1));
    }

    private double pixelXToReal(int px, int width) {
        int w = Math.max(1, width - 1);
        return xMin + (px / (double) w) * (xMax - xMin);
    }

    private double pixelYToImag(int py, int height) {
        int h = Math.max(1, height - 1);
        return yMax - (py / (double) h) * (yMax - yMin);
    }
}

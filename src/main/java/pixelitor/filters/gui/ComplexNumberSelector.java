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

import pixelitor.colors.Colors;
import pixelitor.filters.util.ComplexBounds;
import pixelitor.utils.Cursors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.Objects;

/**
 * A component that displays a portion of the complex plane and
 * allows users to select a complex number by clicking or dragging.
 */
public class ComplexNumberSelector extends JComponent {
    public static final int DEFAULT_SIZE = 100;

    private static final int LABEL_GAP = 4;
    private static final int TICK_HALF_LEN = 2;
    private static final int EDGE_MARGIN = 2;
    private static final int ARROW_LENGTH = 7;
    private static final int ARROW_HALF_WIDTH = 3;

    private Color bgColor;
    private Color gridColor;
    private Color axisColor;
    private Color borderColor;
    private Color disabledBgColor;
    private Color disabledBorderColor;
    private Font axisLabelFont;

    private final ComplexNumberParam model;
    private ComplexBounds complexBounds;

    private boolean reAxisVisible;
    private boolean imAxisVisible;

    // tick positions in the complex space
    private int firstReTick;
    private int lastReTick;
    private int firstImTick;
    private int lastImTick;

    public ComplexNumberSelector(ComplexNumberParam model) {
        this.model = Objects.requireNonNull(model);
        applyBounds(model.getBounds());

        updateTheme();
        setPreferredSize(new Dimension(DEFAULT_SIZE, DEFAULT_SIZE));
        setOpaque(true);
        setCursor(Cursors.CROSSHAIR);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                updateModelFromMouse(e, true);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                updateModelFromMouse(e, true);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                updateModelFromMouse(e, false);
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    public void updateBounds() {
        applyBounds(model.getBounds());
        repaint();
    }

    private void applyBounds(ComplexBounds newBounds) {
        this.complexBounds = Objects.requireNonNull(newBounds);
        reAxisVisible = complexBounds.containsIm(0);
        imAxisVisible = complexBounds.containsRe(0);
        firstReTick = (int) Math.ceil(complexBounds.minRe());
        lastReTick = (int) Math.floor(complexBounds.maxRe());
        firstImTick = (int) Math.ceil(complexBounds.minIm());
        lastImTick = (int) Math.floor(complexBounds.maxIm());
    }

    @Override
    public void updateUI() {
        super.updateUI();
        updateTheme();
        repaint();
    }

    private void updateTheme() {
        axisLabelFont = UIManager.getFont("Label.font");
        if (axisLabelFont == null) {
            axisLabelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        }
        setFont(axisLabelFont);

        bgColor = Colors.getLafBg();
        disabledBgColor = Colors.getLafDisabledBg();
        setBackground(bgColor);

        axisColor = Colors.getLafFg();
        setForeground(axisColor);

        borderColor = Colors.getLafBorder();
        disabledBorderColor = Colors.getLafDisabledBorder();

        // faint grid lines derived from the background and axis (foreground) colors
        gridColor = Colors.interpolateRgb(bgColor, axisColor, 0.15);
    }

    private void updateModelFromMouse(MouseEvent e, boolean isAdjusting) {
        if (!isEnabled() || !SwingUtilities.isLeftMouseButton(e)) {
            return;
        }

        int width = getWidth();
        int height = getHeight();

        // clamp mouse coordinates to the component bounds
        int clampedX = Math.clamp(e.getX(), 0, width - 1);
        int clampedY = Math.clamp(e.getY(), 0, height - 1);

        double re = pixelXToRe(clampedX, width);
        double im = pixelYToIm(clampedY, height);

        model.setValue(re, im, true, isAdjusting, true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();
        Graphics2D g2 = (Graphics2D) g.create();

        try {
            // background
            g2.setColor(isEnabled() ? bgColor : disabledBgColor);
            g2.fillRect(0, 0, width, height);

            if (!isEnabled()) {
                g2.setColor(disabledBorderColor);
                g2.drawRect(0, 0, width - 1, height - 1);
                return;
            }

            drawGrid(g2, width, height);

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            drawAxes(g2, width, height);

            if (reAxisVisible) {
                drawReAxisTicksAndLabels(g2, width, height);
            }
            if (imAxisVisible) {
                drawImAxisTicksAndLabels(g2, width, height);
            }

            // paint outer border
            g2.setColor(borderColor);
            g2.drawRect(0, 0, width - 1, height - 1);

            if (model.shouldDrawCircle()) {
                // Newton fractal roots attract when |a − 1| < 1.
                int x = reToPixelX(0, width);
                int y = imToPixelY(1, height);
                double w = 2 * width / complexBounds.width();
                double h = 2 * height / complexBounds.height();
                g2.setColor(Color.RED);
                g2.draw(new Ellipse2D.Double(x, y, w, h));
            }

            // paint the crosshair indicator based on the model state
            int markerX = reToPixelX(model.getRe(), width);
            int markerY = imToPixelY(model.getIm(), height);
            CrosshairPainter.paint(g2, markerX, markerY, width, height);
        } finally {
            g2.dispose();
        }
    }

    private void drawGrid(Graphics g, int width, int height) {
        // faint grid lines at integer coordinates
        g.setColor(gridColor);

        for (int i = firstReTick; i <= lastReTick; i++) {
            if (i != 0) { // axes drawn separately
                int px = reToPixelX(i, width);
                g.drawLine(px, 0, px, height - 1);
            }
        }

        for (int i = firstImTick; i <= lastImTick; i++) {
            if (i != 0) {
                int py = imToPixelY(i, height);
                g.drawLine(0, py, width - 1, py);
            }
        }
    }

    private void drawAxes(Graphics g, int width, int height) {
        g.setColor(axisColor);

        // real axis (horizontal, y = 0)
        if (reAxisVisible) {
            int y0 = imToPixelY(0.0, height);
            g.drawLine(0, y0, width - 1, y0);

            // right arrow tip at positive end
            int[] xPoints = {width - 1, width - 1 - ARROW_LENGTH, width - 1 - ARROW_LENGTH};
            int[] yPoints = {y0, y0 - ARROW_HALF_WIDTH, y0 + ARROW_HALF_WIDTH};
            g.fillPolygon(xPoints, yPoints, 3);
        }

        // imaginary axis (vertical, x = 0)
        if (imAxisVisible) {
            int x0 = reToPixelX(0.0, width);
            g.drawLine(x0, 0, x0, height - 1);

            // top arrow tip at positive end
            int[] xPoints = {x0, x0 - ARROW_HALF_WIDTH, x0 + ARROW_HALF_WIDTH};
            int[] yPoints = {0, ARROW_LENGTH, ARROW_LENGTH};
            g.fillPolygon(xPoints, yPoints, 3);
        }
    }

    private void drawReAxisTicksAndLabels(Graphics g, int width, int height) {
        g.setColor(axisColor);
        g.setFont(axisLabelFont);
        FontMetrics fm = g.getFontMetrics();

        int y0 = imToPixelY(0.0, height);
        boolean fitsBelowAxis = y0 + fm.getHeight() + LABEL_GAP <= height - EDGE_MARGIN;
        int textY = fitsBelowAxis ? y0 + fm.getAscent() + LABEL_GAP : y0 - LABEL_GAP;

        for (int i = firstReTick; i <= lastReTick; i++) {
            if (i == 0) {
                continue;
            }
            int px = reToPixelX(i, width);
            g.drawLine(px, y0 - TICK_HALF_LEN, px, y0 + TICK_HALF_LEN);

            String text = Integer.toString(i);
            int textWidth = fm.stringWidth(text);
            int textX = Math.max(EDGE_MARGIN, Math.min(width - textWidth - EDGE_MARGIN, px - textWidth / 2));
            g.drawString(text, textX, textY);
        }
    }

    private void drawImAxisTicksAndLabels(Graphics g, int width, int height) {
        g.setColor(axisColor);
        g.setFont(axisLabelFont);
        FontMetrics fm = g.getFontMetrics();

        int x0 = reToPixelX(0.0, width);

        int maxLabelWidth = 0;
        for (int i = firstImTick; i <= lastImTick; i++) {
            if (i != 0) {
                maxLabelWidth = Math.max(maxLabelWidth, fm.stringWidth(Integer.toString(i)));
            }
        }

        boolean fitsLeftOfAxis = (x0 - maxLabelWidth - LABEL_GAP) >= EDGE_MARGIN;
        int rightTextX = x0 + LABEL_GAP + 1;

        int ascent = fm.getAscent();
        int descent = fm.getDescent();
        int halfAscent = ascent / 2 - 1;
        int minY = ascent + 1;
        int maxY = height - descent - EDGE_MARGIN;

        for (int i = firstImTick; i <= lastImTick; i++) {
            if (i == 0) {
                continue;
            }
            int py = imToPixelY(i, height);
            g.drawLine(x0 - TICK_HALF_LEN, py, x0 + TICK_HALF_LEN, py);

            String text = Integer.toString(i);
            int textX = fitsLeftOfAxis
                ? x0 - fm.stringWidth(text) - LABEL_GAP
                : rightTextX;
            int textY = Math.max(minY, Math.min(maxY, py + halfAscent));

            g.drawString(text, textX, textY);
        }
    }

    private int reToPixelX(double re, int width) {
        return (int) Math.round(
            (re - complexBounds.minRe()) / complexBounds.width() * (width - 1));
    }

    private int imToPixelY(double im, int height) {
        // inverts the Y axis to increase downwards
        return (int) Math.round(
            (complexBounds.maxIm() - im) / complexBounds.height() * (height - 1));
    }

    private double pixelXToRe(int px, int width) {
        int w = Math.max(1, width - 1);
        return complexBounds.minRe() + (px / (double) w) * complexBounds.width();
    }

    private double pixelYToIm(int py, int height) {
        int h = Math.max(1, height - 1);
        return complexBounds.maxIm() - (py / (double) h) * complexBounds.height();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setCursor(enabled ? Cursors.CROSSHAIR : Cursors.DEFAULT);
    }
}

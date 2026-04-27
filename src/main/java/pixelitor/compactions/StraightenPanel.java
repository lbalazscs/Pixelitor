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

package pixelitor.compactions;

import pixelitor.Composition;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.utils.Shapes;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.function.DoubleConsumer;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;

/**
 * Dialog panel for the straighten action.
 */
public class StraightenPanel extends JPanel implements ChangeListener {
    private final JSpinner angleSpinner;
    private final ReferenceLinePanel referenceLinePanel;
    private boolean updatingAngleFromLine = false;

    private StraightenPanel(Composition comp) {
        setLayout(new BorderLayout());

        referenceLinePanel = new ReferenceLinePanel(comp.getCompositeImage(), this::setAngleFromLine);
        add(referenceLinePanel, BorderLayout.CENTER);

        JPanel controlsPanel = new JPanel(new GridBagLayout());
        var gbh = new GridBagHelper(controlsPanel);

        angleSpinner = new JSpinner(new SpinnerNumberModel(0.0, -180.0, 180.0, 0.1));
        angleSpinner.addChangeListener(this);
        gbh.addLabelAndControl("Angle (degrees):", angleSpinner);

        gbh.addFullRow(new JLabel("<html>Draw a line that should become horizontal.<br>"
            + "The angle is calculated automatically, and you can still fine-tune it."));

        add(controlsPanel, BorderLayout.SOUTH);
    }

    private double getAngleDegrees() {
        return ((Number) angleSpinner.getValue()).doubleValue();
    }

    private void setAngleFromLine(double angleDegrees) {
        updatingAngleFromLine = true;
        angleSpinner.setValue(angleDegrees);
        updatingAngleFromLine = false;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (!updatingAngleFromLine) {
            referenceLinePanel.updateLineAngle(getAngleDegrees());
        }
    }

    public static void showInDialog(Composition comp) {
        StraightenPanel panel = new StraightenPanel(comp);
        new DialogBuilder()
            .title(Straighten.NAME)
            .content(panel)
            .okAction(() ->
                new Straighten(panel.getAngleDegrees()).process(comp))
            .show();
    }

    private static class ReferenceLinePanel extends JPanel {
        private static final int MAX_PREVIEW_WIDTH = 540;
        private static final int MAX_PREVIEW_HEIGHT = 340;
        private static final int PADDING = 8;

        private final BufferedImage image;
        private final DoubleConsumer angleConsumer;
        private Point2D.Double start;
        private Point2D.Double end;

        private ReferenceLinePanel(BufferedImage image, DoubleConsumer angleConsumer) {
            this.image = image;
            this.angleConsumer = angleConsumer;
            setBorder(BorderFactory.createTitledBorder("Reference Line"));

            Insets insets = getInsets(); // get it after border is applied
            int prefWidth = Math.min(MAX_PREVIEW_WIDTH, image.getWidth() + PADDING * 2 + insets.left + insets.right);
            int prefHeight = Math.min(MAX_PREVIEW_HEIGHT, image.getHeight() + PADDING * 2 + insets.top + insets.bottom);
            setPreferredSize(new Dimension(prefWidth, prefHeight));

            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    start = toImageCoords(e.getPoint());
                    end = start;
                    repaint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (start == null) {
                        return;
                    }

                    end = toImageCoords(e.getPoint());
                    updateAngleFromCurrentLine();
                    repaint();
                }
            };
            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

                Rectangle imageRect = getImageRect();
                g2.drawImage(image, imageRect.x, imageRect.y, imageRect.width, imageRect.height, null);

                if (start != null && end != null) {
                    Point2D.Double drawStart = toComponentCoords(start);
                    Point2D.Double drawEnd = toComponentCoords(end);
                    Shapes.drawVisibly(g2, new Line2D.Double(drawStart, drawEnd));
                }
            } finally {
                g2.dispose();
            }
        }

        private Rectangle getImageRect() {
            Insets insets = getInsets();
            int availableWidth = getWidth() - insets.left - insets.right - PADDING * 2;
            int availableHeight = getHeight() - insets.top - insets.bottom - PADDING * 2;

            if (availableWidth <= 0 || availableHeight <= 0) {
                return new Rectangle(insets.left + PADDING, insets.top + PADDING, 1, 1);
            }

            double sx = availableWidth / (double) image.getWidth();
            double sy = availableHeight / (double) image.getHeight();
            double scale = Math.min(sx, sy);

            int drawWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int drawHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));

            int drawX = insets.left + PADDING + (availableWidth - drawWidth) / 2;
            int drawY = insets.top + PADDING + (availableHeight - drawHeight) / 2;

            return new Rectangle(drawX, drawY, drawWidth, drawHeight);
        }

        private Point2D.Double toImageCoords(Point p) {
            Rectangle rect = getImageRect();
            double x = (p.x - rect.x) * (double) image.getWidth() / rect.width;
            double y = (p.y - rect.y) * (double) image.getHeight() / rect.height;
            return new Point2D.Double(
                Math.max(0, Math.min(image.getWidth(), x)),
                Math.max(0, Math.min(image.getHeight(), y))
            );
        }

        private Point2D.Double toComponentCoords(Point2D.Double p) {
            Rectangle rect = getImageRect();
            double x = rect.x + (p.x / image.getWidth()) * rect.width;
            double y = rect.y + (p.y / image.getHeight()) * rect.height;
            return new Point2D.Double(x, y);
        }

        private void updateAngleFromCurrentLine() {
            if (start == null || end == null || start.equals(end)) {
                return;
            }
            double dx = end.x - start.x;
            double dy = end.y - start.y;
            double degrees = -Math.toDegrees(Math.atan2(dy, dx));
            angleConsumer.accept(Math.round(degrees * 100.0) / 100.0);
        }

        /**
         * Rotates an already drawn line around is current midpoint.
         */
        public void updateLineAngle(double degrees) {
            if (start == null || end == null || start.equals(end)) {
                return;
            }

            double cx = (start.x + end.x) / 2.0;
            double cy = (start.y + end.y) / 2.0;
            double length = start.distance(end);

            double rads = -Math.toRadians(degrees);

            // the new differences from the midpoint
            double halfLength = length / 2.0;
            double dx = halfLength * Math.cos(rads);
            double dy = halfLength * Math.sin(rads);

            start.setLocation(cx - dx, cy - dy);
            end.setLocation(cx + dx, cy + dy);

            repaint();
        }
    }
}

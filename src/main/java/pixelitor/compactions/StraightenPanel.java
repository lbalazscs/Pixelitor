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

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
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
        setLayout(new GridBagLayout());
        var gbh = new GridBagHelper(this);

        referenceLinePanel = new ReferenceLinePanel(comp.getCompositeImage(), this::setAngleFromLine);
        gbh.addFullRow(referenceLinePanel);

        angleSpinner = new JSpinner(new SpinnerNumberModel(0.0, -180.0, 180.0, 0.1));
        angleSpinner.addChangeListener(this);
        gbh.addLabelAndControl("Angle (degrees):", angleSpinner);

        gbh.addFullRow(new JLabel("<html>Draw a line that should become horizontal.<br>"
            + "The angle is calculated automatically, and you can still fine-tune it."));
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
            referenceLinePanel.repaint();
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
        private final java.util.function.DoubleConsumer angleConsumer;
        private Point start;
        private Point end;

        private ReferenceLinePanel(BufferedImage image, java.util.function.DoubleConsumer angleConsumer) {
            this.image = image;
            this.angleConsumer = angleConsumer;
            setBorder(BorderFactory.createTitledBorder("Reference Line"));

            int prefWidth = Math.min(MAX_PREVIEW_WIDTH, image.getWidth() + PADDING * 2);
            int prefHeight = Math.min(MAX_PREVIEW_HEIGHT, image.getHeight() + PADDING * 2);
            setPreferredSize(new java.awt.Dimension(prefWidth, prefHeight));

            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    Point p = clampToImage(e.getPoint());
                    start = p;
                    end = p;
                    repaint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (start == null) {
                        return;
                    }

                    end = clampToImage(e.getPoint());
                    updateAngleFromCurrentLine();
                    repaint();
                }
            };
            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            try {
                g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

                Rectangle imageRect = getImageRect();
                g2.drawImage(image, imageRect.x, imageRect.y, imageRect.width, imageRect.height, null);

                if (start != null && end != null) {
                    g2.setColor(WHITE);
                    g2.setStroke(new java.awt.BasicStroke(3.0f));
                    g2.draw(new Line2D.Double(start, end));

                    g2.setColor(RED);
                    g2.setStroke(new java.awt.BasicStroke(1.5f));
                    g2.draw(new Line2D.Double(start, end));
                }
            } finally {
                g2.dispose();
            }
        }

        private Rectangle getImageRect() {
            int availableWidth = getWidth() - PADDING * 2;
            int availableHeight = getHeight() - PADDING * 2;
            if (availableWidth <= 0 || availableHeight <= 0) {
                return new Rectangle(PADDING, PADDING, 1, 1);
            }

            double sx = availableWidth / (double) image.getWidth();
            double sy = availableHeight / (double) image.getHeight();
            double scale = Math.min(sx, sy);

            int drawWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int drawHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));
            int drawX = (getWidth() - drawWidth) / 2;
            int drawY = (getHeight() - drawHeight) / 2;

            return new Rectangle(drawX, drawY, drawWidth, drawHeight);
        }

        private Point clampToImage(Point input) {
            Rectangle imageRect = getImageRect();
            int x = Math.max(imageRect.x, Math.min(imageRect.x + imageRect.width, input.x));
            int y = Math.max(imageRect.y, Math.min(imageRect.y + imageRect.height, input.y));
            return new Point(x, y);
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
    }
}

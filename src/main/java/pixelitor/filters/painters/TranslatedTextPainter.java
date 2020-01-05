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

package pixelitor.filters.painters;

import org.jdesktop.swingx.painter.TextPainter;
import org.jdesktop.swingx.painter.effects.AreaEffect;
import pixelitor.Canvas;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;

import static java.awt.RenderingHints.KEY_FRACTIONALMETRICS;
import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_GASP;

/**
 * A TextPainter that can have an extra translation (so that text
 * layers can be moved with the move tool).
 * It also supports the rotation of the text.
 */
public class TranslatedTextPainter extends TextPainter {
    private static final long serialVersionUID = -2064757977654857961L;

    private int translationX = 0;
    private int translationY = 0;

    private transient RotatedRectangle rotatedRect;
    private double rotation = 0;
    private Rectangle boundingBox = new Rectangle();

    /**
     * Return last painted bounding box for rendered text
     * Note that this is not pixel perfect rect
     * If text was not rendered yet, returned rectangle is empty
     */
    public Rectangle getBoundingBox() {
        return rotatedRect != null ? rotatedRect.getBoundingBox() : boundingBox;
    }

    /**
     * Return last painted bounding shape for rendered text (rect or rotated rect)
     * Note that this is not pixel perfect shape
     * If text was not rendered yet, returned shape is empty
     */
    public Shape getBoundingShape() {
        return rotatedRect != null ? rotatedRect.asShape() : boundingBox;
    }

    @Override
    protected Rectangle calculateLayout(int textWidth, int textHeight, int canvasWidth, int canvasHeight) {
        if (rotation == 0) {
            Rectangle layout = super.calculateLayout(textWidth, textHeight, canvasWidth, canvasHeight);
            rotatedRect = null;

            // support the Move tool
            layout.translate(translationX, translationY);

            return layout;
        }

        // first calculate a rotated rectangle starting at 0, 0
        rotatedRect = new RotatedRectangle(0, 0, textWidth, textHeight, rotation);
        Rectangle rotatedBounds = rotatedRect.getBoundingBox();

        // use the rotated bounds to calculate the correct layout
        Rectangle layout = super.calculateLayout(rotatedBounds.width, rotatedBounds.height, canvasWidth, canvasHeight);

        // Also correct the rotatedRect, because it will be useful later.
        // Do this before translating the layout!
        int dx = layout.x - rotatedBounds.x;
        int dy = layout.y - rotatedBounds.y;
        rotatedRect.translate(dx + translationX, dy + translationY);

        // support the Move tool
        layout.translate(translationX, translationY);

        return layout;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPaint(Graphics2D g, Object component, int canvasWidth, int canvasHeight) {
        var origTransform = g.getTransform();
        String text = getText();

        FontMetrics metrics = setupGraphics(g, canvasWidth, canvasHeight, text);

        Paint paint = getFillPaint();
        if (paint != null) {
            g.setPaint(paint);
        }

        g.drawString(text, 0, (float) metrics.getAscent());

        // paint the effects on an explicitly transformed shape
        // instead of simply painting them on the transformed graphics
        // so that the direction of the drop shadow effect does not rotate
        var tx = g.getTransform();
        g.setTransform(origTransform);

        AreaEffect[] effects = getAreaEffects();
        if (effects.length != 0) {
            // provideShape must be called on an untransformed shape
            Shape shape = provideShape(g, component, canvasWidth, canvasHeight);
            Shape transformedShape = tx.createTransformedShape(shape);
            for (AreaEffect ef : effects) {
                ef.apply(g, transformedShape, canvasWidth, canvasHeight);
            }
        }
    }

    // sets up the given Graphics2D so that it is usable
    // from both doPaint and getTextShape
    private FontMetrics setupGraphics(Graphics2D g, int canvasWidth, int canvasHeight, String text) {
        g.setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_GASP);
        g.setFont(font);

        FontMetrics metrics = g.getFontMetrics(font);

        int textWidth = metrics.stringWidth(text);
        int textHeight = metrics.getHeight();
        boundingBox = calculateLayout(textWidth, textHeight, canvasWidth, canvasHeight);

        if (rotation != 0) {
            assert rotatedRect != null;

            double topLeftX = rotatedRect.getTopLeftX();
            double topLeftY = rotatedRect.getTopLeftY();
            g.translate(topLeftX, topLeftY);
            g.rotate(rotation, 0, 0);
        } else {
            assert rotatedRect == null;
            g.translate(boundingBox.x, boundingBox.y);
        }
        return metrics;
    }

    public Shape getTextShape(Canvas canvas) {
        // create this image just to get a Graphics2D somehow...
        BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = tmp.createGraphics();
        var imgOrigTransform = g2.getTransform();

        int canvasWidth = canvas.getImWidth();
        int canvasHeight = canvas.getImHeight();
        setupGraphics(g2, canvasWidth, canvasHeight, getText());
        var at = g2.getTransform();
        g2.setTransform(imgOrigTransform); // provideShape must be called with untransformed Graphics
        Shape shape = provideShape(g2, null, canvasWidth, canvasHeight);

        g2.dispose();
        tmp.flush();

        return at.createTransformedShape(shape);
    }

    @Override
    protected String calculateText(Object component) {
        return getText();
    }

    @Override
    protected Font calculateFont(Object component) {
        return getFont();
    }

    public void setTranslation(int translationX, int translationY) {
        this.translationX = translationX;
        this.translationY = translationY;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    public int getTx() {
        return translationX;
    }

    public int getTy() {
        return translationY;
    }
}

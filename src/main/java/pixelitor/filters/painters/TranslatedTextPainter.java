/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

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
        g.setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_GASP);
        g.setFont(font);

        // it is important to get the shape for the effects
        // before transforming the graphics
        Shape shape = null;
        AreaEffect[] effects = getAreaEffects();
        if (effects.length != 0) {
            shape = provideShape(g, component, canvasWidth, canvasHeight);
        }

        String text = getText();
        FontMetrics metrics = g.getFontMetrics(font);

        int textWidth = metrics.stringWidth(text);
        int textHeight = metrics.getHeight();
        boundingBox = calculateLayout(textWidth, textHeight, canvasWidth, canvasHeight);

        AffineTransform origTX = g.getTransform();

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

        Paint paint = getFillPaint();
        if (paint != null) {
            g.setPaint(paint);
        }

        g.drawString(text, (float) 0, (float) metrics.getAscent());

        // explicitly transform the shape and paint it with the original transform
        // instead of simply painting the shape on the transformed graphics
        // so that the direction of the drop shadow effect does not rotate
        AffineTransform tx = g.getTransform();
        g.setTransform(origTX);

        if (shape != null) { // has effects
            Shape transformedShape = tx.createTransformedShape(shape);
            for (AreaEffect ef : effects) {
                ef.apply(g, transformedShape, canvasWidth, canvasHeight);
            }
        }
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

    public int getTX() {
        return translationX;
    }

    public int getTY() {
        return translationY;
    }
}

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

package pixelitor.filters.painters;

import org.jdesktop.swingx.painter.TextPainter;
import org.jdesktop.swingx.painter.effects.AreaEffect;
import pixelitor.Canvas;
import pixelitor.compactions.Flip;
import pixelitor.utils.QuadrantAngle;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.DebugNodes;
import pixelitor.utils.debug.Debuggable;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.Serial;

import static java.awt.RenderingHints.KEY_FRACTIONALMETRICS;
import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_GASP;

/**
 * A {@link TextPainter} that can have an extra translation (so that text
 * layers can be moved with the move tool).
 * It also supports the rotation of the text.
 */
public class TransformedTextPainter extends TextPainter implements Debuggable {
    @Serial
    private static final long serialVersionUID = -2064757977654857961L;

    private int translationX = 0;
    private int translationY = 0;
    private double rotation = 0;

    private transient RotatedRectangle rotatedRect;
    private transient Rectangle boundingBox;
    private transient Shape transformedShape;

    private AffineTransform extraTransform;

    /**
     * Return the last painted bounding box for the rendered text.
     * Note that this is an approximation.
     */
    public Rectangle getBoundingBox() {
        return rotatedRect != null ? rotatedRect.getBoundingBox() : boundingBox;
    }

    /**
     * Return last painted shape of the rendered text's bounding box.
     */
    public Shape getBoundingShape() {
        return rotatedRect != null ? rotatedRect.asShape() : boundingBox;
    }

    @Override
    protected Rectangle calculateLayout(int textWidth, int textHeight, int width, int height) {
        if (rotation == 0) {
            Rectangle layout = super.calculateLayout(textWidth, textHeight, width, height);
            rotatedRect = null;

            // support the Move tool
            layout.translate(translationX, translationY);

            return layout;
        }

        // first calculate a rotated rectangle starting at 0, 0
        rotatedRect = new RotatedRectangle(0, 0, textWidth, textHeight, rotation);
        Rectangle rotatedBounds = rotatedRect.getBoundingBox();

        // use the rotated bounds to calculate the correct layout
        Rectangle layout = super.calculateLayout(rotatedBounds.width, rotatedBounds.height, width, height);

        // support the Move tool
        layout.translate(translationX, translationY);

        // Also correct the rotatedRect, because it will be useful later.
        int dx = layout.x - rotatedBounds.x;
        int dy = layout.y - rotatedBounds.y;
        rotatedRect.translate(dx, dy);

        return layout;
    }

    @Override
    protected void doPaint(Graphics2D g, Object component, int canvasWidth, int canvasHeight) {
        paintText(g, component, canvasWidth, canvasHeight, true);
    }

    private void paintText(Graphics2D g, Object component, int width, int height, boolean updateLayout) {
        var origTransform = g.getTransform();
        String text = getText();

        FontMetrics metrics = g.getFontMetrics(font);
        if (updateLayout) {
            updateLayout(width, height, text, metrics);
        }
        setupGraphics(g);

        Paint paint = getFillPaint();
        if (paint != null) {
            g.setPaint(paint);
        }

        g.drawString(text, 0, (float) metrics.getAscent());

        // Paint the effects of an explicitly transformed shape
        // instead of simply painting them on the transformed graphics
        // so that the direction of the drop shadow effect doesn't rotate.
        var tx = g.getTransform();
        g.setTransform(origTransform);

        AreaEffect[] effects = getAreaEffects();
        if (effects.length != 0) {
            if (updateLayout) {
                //provideShape must be called with untransformed Graphics
                Shape shape = provideShape(g, component, width, height);
                transformedShape = tx.createTransformedShape(shape);
            }
            for (AreaEffect ef : effects) {
                ef.apply(g, transformedShape, width, height);
            }
        }
    }

    /**
     * Renders a possibly off-canvas image, without recalculating the layout.
     * (Recalculating the layout can cause rounding errors in the ORA export)
     */
    public BufferedImage renderRectangle(Rectangle bounds) {
        BufferedImage img = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.translate(-bounds.x, -bounds.y);
        paintText(g2, null, bounds.width, bounds.height, false);
        g2.dispose();
        return img;
    }

    /**
     * Sets up the given Graphics2D so that it is usable from both doPaint and
     * getTextShape. This method assumes that the text's location is already calculated.
     */
    private void setupGraphics(Graphics2D g) {
        g.setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_GASP);

        assert font != null;
        g.setFont(font);

        if (rotation == 0) {
            assert rotatedRect == null;
            g.translate(boundingBox.x, boundingBox.y);
        } else {
            assert rotatedRect != null;

            double topLeftX = rotatedRect.getTopLeftX();
            double topLeftY = rotatedRect.getTopLeftY();
            g.translate(topLeftX, topLeftY);
            g.rotate(rotation, 0, 0);
        }
        if (extraTransform != null) {
            g.transform(extraTransform);
        }
    }

    private void updateLayout(int width, int height, String text, FontMetrics metrics) {
        int textWidth = metrics.stringWidth(text);
        int textHeight = metrics.getHeight();
        boundingBox = calculateLayout(textWidth, textHeight, width, height);
    }

    public Shape getTextShape(Canvas canvas) {
        // This image is created just to get a Graphics2D somehow...
        BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = tmp.createGraphics();
        var imgOrigTransform = g2.getTransform();

        setupGraphics(g2);
        var at = g2.getTransform();
        g2.setTransform(imgOrigTransform); // provideShape must be called with untransformed Graphics
        Shape shape = provideShape(g2, null, canvas.getWidth(), canvas.getHeight());

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

    public void flip(Flip.Direction direction, Canvas canvas) {
        // TODO
//        AffineTransform flipTransform = direction.createTransform(boundingBox.width, boundingBox.height);
//        if (extraTransform == null) {
//            extraTransform = flipTransform;
//        } else {
//            extraTransform.preConcatenate(flipTransform);
//        }
    }

    public void rotate(QuadrantAngle angle, Canvas canvas) {
        // TODO
//        AffineTransform rotateTransform = angle.createTransform(boundingBox.width, boundingBox.height);
//        if (extraTransform == null) {
//            extraTransform = rotateTransform;
//        } else {
//            extraTransform.preConcatenate(rotateTransform);
//        }
    }

    public TransformedTextPainter copy(TextSettings settings) {
        var copy = new TransformedTextPainter();
        settings.configurePainter(copy);

        // also copy internal data
        copy.translationX = translationX;
        copy.translationY = translationY;
        copy.rotation = rotation;

        copy.rotatedRect = rotatedRect;
        copy.boundingBox = boundingBox;
        copy.transformedShape = transformedShape;
        copy.extraTransform = extraTransform;

        return copy;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key, this);

        node.addInt("translationX", translationX);
        node.addInt("translationY", translationY);
        node.addDouble("rotation", rotation);

        node.addNullableDebuggable("boundingBox", boundingBox, DebugNodes::createRectangleNode);
        node.addNullableDebuggable("rotatedRect", rotatedRect);
        node.addNullableDebuggable("extraTransform", extraTransform, DebugNodes::createTransformNode);
        node.addNullableProperty("transformedShape", transformedShape);

        return node;
    }
}

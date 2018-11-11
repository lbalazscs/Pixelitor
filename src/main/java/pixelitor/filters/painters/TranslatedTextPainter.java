/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

/**
 * A TextPainter that can have an extra translation (so that text
 * layers can be moved with the move tool).
 * It also supports the rotation of the text.
 */
public class TranslatedTextPainter extends TextPainter {
    private static final long serialVersionUID = -2064757977654857961L;

    private int translationX = 0;
    private int translationY = 0;

    private transient RotatedRectangle rotatedLayout;
    private double rotation = 0;
    private Rectangle boundingBox = new Rectangle();

    /**
     * Return last painted bounding box for rendered text
     * Note that this is not pixel perfect rect
     * If text was not rendered yet, returned rectangle is empty
     */
    public Rectangle getBoundingBox()
    {
        return rotatedLayout != null? rotatedLayout.getBoundingBox() : boundingBox;
    }

    /**
     * Return last painted bounding shape for rendered text (rect or rotated rect)
     * Note that this is not pixel perfect shape
     * If text was not rendered yet, returned shape is empty
     */
    public Shape getBoundingShape()
    {
        return rotatedLayout != null? rotatedLayout.asShape() : boundingBox;
    }

    @Override
    protected Rectangle calculateLayout(int contentWidth, int contentHeight, int width, int height) {
        if (rotation == 0) {
            Rectangle layout = super.calculateLayout(contentWidth, contentHeight, width, height);
            rotatedLayout = null;

            // support the Move tool
            layout.translate(translationX, translationY);

            return layout;
        }

        // first calculate a rotated rectangle starting at 0, 0...
        rotatedLayout = new RotatedRectangle(0, 0, contentWidth, contentHeight, rotation);
        Rectangle bb = rotatedLayout.getBoundingBox();

        // ...then position it correctly and apply the corrections
        // and also support the Move tool
        Rectangle layout = super.calculateLayout(bb.width, bb.height, width, height);
        int dx = layout.x - bb.x;
        int dy = layout.y - bb.y;
        rotatedLayout.translate(dx + translationX, dy + translationY);
        layout.translate(translationX, translationY);
        return layout;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPaint(Graphics2D g, Object component, int width, int height) {
        Font font = calculateFont(component);
        if (font != null) {
            g.setFont(font);
        }

        Paint paint = getFillPaint();

        String text = calculateText(component);
        FontMetrics metrics = g.getFontMetrics(g.getFont());

        int tw = metrics.stringWidth(text);
        int th = metrics.getHeight();
        boundingBox = calculateLayout(tw, th, width, height);

        AffineTransform origTX = g.getTransform();

        if (rotatedLayout != null) {
            assert rotation != 0;

            double topLeftX = rotatedLayout.getTopLeftX();
            double topLeftY = rotatedLayout.getTopLeftY();
            g.translate(topLeftX, topLeftY);
            g.rotate(rotation, 0, 0);
        } else {
            assert rotation == 0;
            g.translate(boundingBox.x, boundingBox.y);
        }

        if (paint != null) {
            g.setPaint(paint);
        }

        g.drawString(text, (float) 0, (float) metrics.getAscent());
        if (getAreaEffects() != null) {
            Shape shape = provideShape(g, component, width, height);
            for (AreaEffect ef : getAreaEffects()) {
                ef.apply(g, shape, width, height);
            }
        }
        //g.translate(-res.x, -res.y);
        g.setTransform(origTX);
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

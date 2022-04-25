/*
 * $Id: TextPainter.java 4147 2012-02-01 17:13:24Z kschaefe $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.jdesktop.swingx.painter;

import org.jdesktop.swingx.painter.effects.AreaEffect;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.font.TextAttribute;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.io.Serial;
import java.util.Map;

import static java.awt.RenderingHints.*;
import static java.awt.font.TextAttribute.*;
import static org.jdesktop.swingx.painter.PainterUtils.getComponentFont;
import static org.jdesktop.swingx.painter.PainterUtils.getForegroundPaint;

/**
 * A painter which draws text. If the font, text, and paint are not provided they will be
 * obtained from the object being painted if it is a Swing text component.
 *
 * @author rbair
 */
@SuppressWarnings("nls")
public class TextPainter extends AbstractAreaPainter<Object> {
    private String text = "";
    protected Font font = null;

    // for compatibility with older pixelitor versions
    @Serial
    private static final long serialVersionUID = 8840747324227212590L;

    /**
     * Creates a new instance of TextPainter
     */
    public TextPainter() {
        this("");
    }

    /**
     * Create a new TextPainter which will paint the specified text
     *
     * @param text the text to paint
     */
    public TextPainter(String text) {
        this(text, null, null);
    }

    /**
     * Create a new TextPainter which will paint the specified text with the specified font.
     *
     * @param text the text to paint
     * @param font the font to paint the text with
     */
    public TextPainter(String text, Font font) {
        this(text, font, null);
    }

    /**
     * Create a new TextPainter which will paint the specified text with the specified paint.
     *
     * @param text  the text to paint
     * @param paint the paint to paint with
     */
    public TextPainter(String text, Paint paint) {
        this(text, null, paint);
    }

    /**
     * Create a new TextPainter which will paint the specified text with the specified font and paint.
     *
     * @param text  the text to paint
     * @param font  the font to paint the text with
     * @param paint the paint to paint with
     */
    public TextPainter(String text, Font font, Paint paint) {
        this.text = text;
        this.font = font;
        setFillPaint(paint);
    }

    /**
     * Set the font (and font size and style) to be used when drawing the text
     *
     * @param f the new font
     */
    public void setFont(Font f) {
        assert f != null;

        Font old = getFont();
        this.font = f;
        setDirty(true);
        firePropertyChange("font", old, getFont());
    }

    /**
     * gets the font (and font size and style) to be used when drawing the text
     *
     * @return the current font
     */
    public Font getFont() {
        return font;
    }

    /**
     * Sets the text to draw
     *
     * @param text the text to draw
     */
    public void setText(String text) {
        String old = getText();
        this.text = text == null ? "" : text;
        setDirty(true);
        firePropertyChange("text", old, getText());
    }

    /**
     * gets the text currently used to draw
     *
     * @return the text to be drawn
     */
    public String getText() {
        return text;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPaint(Graphics2D g, Object component, int width, int height) {
        g.setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_GASP);

        Font f = calculateFont(component);
        if (f != null) {
            g.setFont(f);
        }

        Paint paint = getForegroundPaint(getFillPaint(), component);
        String t = calculateText(component);

        // get the font metrics
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        //Rectangle2D rect = metrics.getStringBounds(text,g);

        int tw = metrics.stringWidth(t);
        int th = metrics.getHeight();
        Rectangle res = calculateLayout(tw, th, width, height);

        g.translate(res.x, res.y);

        if (isPaintStretched()) {
            paint = calculateSnappedPaint(paint, res.width, res.height);
        }

        if (paint != null) {
            g.setPaint(paint);
        }

        g.drawString(t, 0, metrics.getAscent());
        if (getAreaEffects() != null) {
            Shape shape = provideShape(g, component, width, height);
            for (AreaEffect ef : getAreaEffects()) {
                ef.apply(g, shape, width, height);
            }
        }
        g.translate(-res.x, -res.y);
    }

    protected String calculateText(Object component) {
        // prep the text
        String t = getText();
        //make components take priority if(text == null || text.trim().equals("")) {
        if (t != null && !t.trim().isEmpty()) {
            return t;
        }
        if (component instanceof JTextComponent) {
            t = ((JTextComponent) component).getText();
        }
        if (component instanceof JLabel) {
            t = ((JLabel) component).getText();
        }
        if (component instanceof AbstractButton) {
            t = ((AbstractButton) component).getText();
        }
        return t;
    }

    protected Font calculateFont(Object component) {
        // prep the various text attributes
        Font f = getComponentFont(getFont(), component);
        if (f == null) {
            f = new Font("Dialog", Font.PLAIN, 18);
        }
        return f;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Shape provideShape(Graphics2D g2, Object comp, int width, int height) {
//        Font f = calculateFont(comp);
        Font f = getFont();
//        String t = calculateText(comp);
        FontMetrics metrics = g2.getFontMetrics(f);

        FontRenderContext frc = g2.getFontRenderContext();

        Map<TextAttribute, ?> attributes = font.getAttributes();
        boolean hasKerning = KERNING_ON.equals(attributes.get(KERNING));
        boolean hasLigatures = LIGATURES_ON.equals(attributes.get(LIGATURES));

        GlyphVector vect;
        if (!hasKerning && !hasLigatures && font.getSize() <= 100) {
            // fix for issue #72: it seems that for some reason 100
            // is a magic number, under which the old way of getting
            // the shape works better
            vect = f.createGlyphVector(frc, text);
        } else {
            // partial fix for issue #64 (fixes kerning and ligatures),
            // also see https://community.oracle.com/thread/1289266
            char[] chars = text.toCharArray();
            vect = font.layoutGlyphVector(frc, chars, 0,
                chars.length, Font.LAYOUT_LEFT_TO_RIGHT);
        }

        Shape glyphsOutline = vect.getOutline(0.0f, metrics.getAscent());

        boolean hasUnderline = UNDERLINE_ON.equals(attributes.get(UNDERLINE));
        boolean hasStrikeThrough = STRIKETHROUGH_ON.equals(attributes.get(STRIKETHROUGH));

        if (!hasUnderline && !hasStrikeThrough) {
            // simple case: the glyphs contain all of the shape
            return glyphsOutline;
        }

        LineMetrics lineMetrics = font.getLineMetrics(text, frc);
        float ascent = lineMetrics.getAscent();
        Area combinedOutline = new Area(glyphsOutline);

        if (hasUnderline) {
            combinedOutline.add(
                createUnderlineShape(metrics, lineMetrics, ascent));
        }

        if (hasStrikeThrough) {
            combinedOutline.add(
                createStrikeThroughShape(metrics, lineMetrics, ascent));
        }

        return combinedOutline;
    }

    private Area createUnderlineShape(FontMetrics metrics, LineMetrics lineMetrics, float ascent) {
        float underlineOffset = lineMetrics.getUnderlineOffset();
        float underlineThickness = lineMetrics.getUnderlineThickness();
        Shape underLineShape = new Rectangle2D.Float(
            0.0f,
            ascent + underlineOffset - underlineThickness / 2.0f,
            metrics.stringWidth(text),
            underlineThickness);
        return new Area(underLineShape);
    }

    private Area createStrikeThroughShape(FontMetrics metrics, LineMetrics lineMetrics, float ascent) {
        float strikethroughOffset = lineMetrics.getStrikethroughOffset();
        float strikethroughThickness = lineMetrics.getStrikethroughThickness();
        Shape strikethroughShape = new Rectangle2D.Float(
            0.0f,
            ascent + strikethroughOffset - strikethroughThickness / 2.0f,
            metrics.stringWidth(text),
            strikethroughThickness);
        return new Area(strikethroughShape);
    }
}

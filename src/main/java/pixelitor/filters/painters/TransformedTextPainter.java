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

import org.jdesktop.swingx.painter.AbstractLayoutPainter.HorizontalAlignment;
import org.jdesktop.swingx.painter.AbstractLayoutPainter.VerticalAlignment;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.util.GraphicsUtilities;
import pixelitor.Canvas;
import pixelitor.Views;
import pixelitor.compactions.Flip;
import pixelitor.gui.utils.BoxAlignment;
import pixelitor.utils.QuadrantAngle;
import pixelitor.utils.Shapes;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.DebugNodes;
import pixelitor.utils.debug.Debuggable;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.font.TextAttribute;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.Objects;

import static java.awt.RenderingHints.*;
import static java.awt.font.TextAttribute.*;

/**
 * A {@link Painter} that can have an extra translation
 * (so that text layers can be moved with the move tool).
 * It also supports the rotation, scaling and shearing of the text.
 */
public class TransformedTextPainter implements Painter, Debuggable {
    private HorizontalAlignment horizontalAlignment = HorizontalAlignment.CENTER;
    private VerticalAlignment verticalAlignment = VerticalAlignment.CENTER;

    private String text = "";
    private Font font = null;
    private String[] lines;
    private Color color;
    private int effectsWidth;

    private AreaEffects effects;

    private int translationX = 0;
    private int translationY = 0;
    private double rotation = 0;

    private TransformedRectangle transformedRect;
    private Rectangle boundingBox;
    private Shape zeroShape;
    private Shape textShape;
    private float lineHeight;
    private double relLineHeight;
    private double sx;
    private double sy;
    private double shx;
    private double shy;

    //    private AffineTransform extraTransform;
    private SoftReference<BufferedImage> cachedImage;

    private boolean invalidLayout = true;
    private boolean invalidShape = true;
    private boolean invalidShapeTransform = true;

    // debug settings
    private static final boolean NO_CACHE = false;
    private static final boolean DEBUG_LAYOUT = false;

    @Override
    public void paint(Graphics2D g, Object object, int width, int height) {
        if (text.isBlank()) {
            return;
        }

        // must be called before updateLayout, even if we paint on the cached image
        optimizeGraphics(g);

        if (invalidLayout) {
            updateLayout(width, height, g);
            if (DEBUG_LAYOUT) {
                Shapes.draw(g, getBoundingBox(), Color.RED);
                if (transformedRect != null) {
                    Shapes.draw(g, transformedRect.asShape(), Color.BLUE);
                }
            }
        }

        if (NO_CACHE) {
            doPaint(g, g.getTransform());
            deOptimizeGraphics(g);
            return;
        }

        Rectangle imageBounds = getBoundingBox();
        if (imageBounds.isEmpty()) {
            // A zero-width bounding box can happen
            // for some fonts like "EmojiOne Color".
            return;
        }

        BufferedImage cache = cachedImage == null ? null : cachedImage.get();
        if (cache == null) {
            cache = GraphicsUtilities.createCompatibleTranslucentImage(imageBounds.width, imageBounds.height);
            Graphics2D cacheG = cache.createGraphics();
            optimizeGraphics(cacheG);

            AffineTransform origTransform = cacheG.getTransform();
            cacheG.translate(-imageBounds.x, -imageBounds.y);
            doPaint(cacheG, origTransform);

            cacheG.dispose();
            cachedImage = new SoftReference<BufferedImage>(cache);
        }

        deOptimizeGraphics(g);
        g.drawImage(cache, imageBounds.x, imageBounds.y, null);
    }

    private static void optimizeGraphics(Graphics2D g) {
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_GASP);
    }

    private static void deOptimizeGraphics(Graphics2D g) {
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_DEFAULT);
        g.setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_DEFAULT);
        g.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_DEFAULT);
    }

    private void clearCache() {
        BufferedImage cache = cachedImage == null ? null : cachedImage.get();
        if (cache != null) {
            cache.flush();
        }
        cachedImage = null;
    }

    /**
     * Return the last painted bounding box for the rendered text.
     * Note that this is an approximation.
     */
    public Rectangle getBoundingBox() {
        return transformedRect != null ? transformedRect.getBoundingBox() : boundingBox;
    }

    /**
     * Return last painted shape of the rendered text's bounding box.
     */
    public Shape getBoundingShape() {
        return transformedRect != null ? transformedRect.asShape() : boundingBox;
    }

    private Rectangle calcAlignment(int contentWidth, int contentHeight,
                                    int width, int height) {
        Rectangle rect = new Rectangle();

        rect.x = switch (horizontalAlignment) {
            case LEFT -> 0;
            case CENTER -> (width - contentWidth) / 2;
            case RIGHT -> width - contentWidth;
        };
        rect.y = switch (verticalAlignment) {
            case TOP -> 0;
            case CENTER -> (height - contentHeight) / 2;
            case BOTTOM -> height - contentHeight;
        };
        rect.width = contentWidth;
        rect.height = contentHeight;

        return rect;
    }

    private Rectangle calculateBoundingBox(int textWidth, int textHeight, int width, int height, Graphics2D g) {
        if (hasNoTransform()) {
            Rectangle layout = calcAlignment(textWidth, textHeight, width, height);
            transformedRect = null;

            // support the Move tool
            layout.translate(translationX, translationY);

            // The effect width is added only after considering
            // the alignment. This means that in non-centered positions
            // the effects don't fit into the canvas, but the text
            // itself isn't shifted as effects are added.
            if (effectsWidth != 0) {
                layout.grow(effectsWidth, effectsWidth);
            }

            return layout;
        }

        // first calculate a transformed rectangle starting at 0, 0
        transformedRect = new TransformedRectangle(0, 0, textWidth, textHeight, rotation, sx, sy, shx, shy);
        Rectangle transformedBounds = transformedRect.getBoundingBox();

        // use the transformed bounds to calculate the correct layout
        Rectangle layout = calcAlignment(transformedBounds.width, transformedBounds.height, width, height);

        // support the Move tool
        layout.translate(translationX, translationY);

        if (effectsWidth == 0) {
            // Also correct the transformed rectangle,
            // because it will be useful later.
            transformedRect.align(layout, transformedBounds);
            return layout;
        }
        // If we still didn't return, it means that we have both
        // transformation and effects. 

        // re-create the transformed rectangle to grow it
        transformedRect = new TransformedRectangle(
            -effectsWidth, -effectsWidth,
            textWidth + 2 * effectsWidth,
            textHeight + 2 * effectsWidth,
            rotation, sx, sy, shx, shy);
        transformedRect.align(layout, transformedBounds);

        layout.grow(effectsWidth, effectsWidth);

        return layout;
    }

    private boolean hasNoTransform() {
        return rotation == 0 && sx == 1.0 && sy == 1.0 && shx == 0 && shy == 0;
    }

    private void doPaint(Graphics2D g, AffineTransform origTransform) {
        g.setColor(color);

        if (isOnPath()) {
            g.fill(textShape);
            if (effects != null && effects.isNotEmpty()) {
                effects.drawOn(g, textShape);
            }
            return;
        }

        FontMetrics metrics = g.getFontMetrics(font);
        transformGraphics(g);

        // Draw the lines relative to the bounding box.
        // Use the ascent, because drawY is relative to the baseline.
        float drawY = (float) (metrics.getAscent() + effectsWidth);
        if (lines.length == 1) {
            g.drawString(text, effectsWidth, drawY);
        } else {
            for (String line : lines) {
                g.drawString(line, effectsWidth, drawY);
                drawY += lineHeight;
            }
        }

        // Paint the effects of an explicitly transformed shape
        // instead of simply painting them on the transformed graphics
        // so that the direction of the drop shadow effect doesn't rotate.
        var tx = g.getTransform();

        //provideShape must be called with untransformed Graphics
        g.setTransform(origTransform);

        if (effects != null && effects.isNotEmpty()) {
            if (invalidShape) {
                zeroShape = provideShape(g);
                invalidShape = false;
                invalidShapeTransform = true; // implied
            }
            if (invalidShapeTransform) {
                tx.translate(effectsWidth, effectsWidth);
                textShape = tx.createTransformedShape(zeroShape);
                invalidShapeTransform = false;
            }

            effects.drawOn(g, textShape);
        }
    }

    /**
     * Renders a possibly off-canvas image, without recalculating the layout.
     * (Recalculating the layout can cause rounding errors in the ORA export)
     */
    public BufferedImage renderArea(Rectangle area) {
        BufferedImage img = new BufferedImage(area.width, area.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.translate(-area.x, -area.y);
        doPaint(g2, g2.getTransform());
        g2.dispose();
        return img;
    }

    /**
     * Sets up the given Graphics2D so that it is usable from both doPaint and
     * getTextShape. This method assumes that the text's location is already calculated.
     */
    private void transformGraphics(Graphics2D g) {
        assert font != null;
        g.setFont(font);

        if (hasNoTransform()) {
            assert transformedRect == null;
            g.translate(boundingBox.x, boundingBox.y);
        } else {
            assert transformedRect != null;

            double topLeftX = transformedRect.getTopLeftX();
            double topLeftY = transformedRect.getTopLeftY();
            g.translate(topLeftX, topLeftY);
            if (rotation != 0) {
                g.rotate(rotation);
            }
            if (sx != 1.0 || sy != 1.0) {
                g.scale(sx, sy);
            }
            if (shx != 0 || shy != 0) {
                g.shear(-shx, -shy);
            }
        }
    }

    private void updateLayout(int width, int height, Graphics2D g) {
        if (isOnPath()) {
            renderOnPath(g);
            return;
        }

        FontMetrics metrics = g.getFontMetrics(font);
        int textWidth = 0;
        double fontLineHeight = metrics.getStringBounds(lines[0], g).getHeight();
        lineHeight = (float) (fontLineHeight * relLineHeight);

        // doesn't count the adjustment of the last line
        int textHeight = (int) (fontLineHeight + lineHeight * (lines.length - 1));

        for (String line : lines) {
            int lineWidth = metrics.stringWidth(line);
            if (lineWidth > textWidth) {
                textWidth = lineWidth;
            }
        }
        boundingBox = calculateBoundingBox(textWidth, textHeight, width, height, g);
        invalidLayout = false;
    }

    private void renderOnPath(Graphics2D g2) {
        g2.setFont(font);
        FontRenderContext frc = g2.getFontRenderContext();
        GlyphVector glyphVector = font.createGlyphVector(frc, text);

        Path2D path = Views.getActiveComp().getActivePath().toImageSpaceShape();
        GeneralPath result = new GeneralPath();
        PathIterator it = new FlatteningPathIterator(path.getPathIterator(null), 1);

        double[] points = new double[6];

        // the coordinates of the starting point of a path segment
        double moveX = 0, moveY = 0;

        // the coordinates of the last processed point
        double lastX = 0, lastY = 0;

        // the coordinates of the current point
        double thisX, thisY;

        int type;

        // the distance to be covered before the next shape is placed
        double thresholdDist = 0;

        int glyphIndex = 0;
        int numGlyphs = glyphVector.getNumGlyphs();
        AffineTransform at = new AffineTransform();

        double nextAdvance = 0;
        double sxa = Math.abs(sx);

        double tracking = 0.0;
        var map = font.getAttributes();
        Float trackingValue = (Float) map.get(TRACKING);
        if (trackingValue != null) {
            tracking = trackingValue * font.getSize();
        }

        while (glyphIndex < numGlyphs && !it.isDone()) {
            type = it.currentSegment(points);
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    moveX = lastX = points[0];
                    moveY = lastY = points[1];
                    result.moveTo(moveX, moveY);
                    nextAdvance = tracking + glyphVector.getGlyphMetrics(glyphIndex).getAdvance() * 0.5f;
                    thresholdDist = nextAdvance;
                    break;

                case PathIterator.SEG_CLOSE:
                    points[0] = moveX;
                    points[1] = moveY;
                    // Fall into....

                case PathIterator.SEG_LINETO:
                    thisX = points[0];
                    thisY = points[1];
                    double dx = thisX - lastX;
                    double dy = thisY - lastY;
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance >= thresholdDist) {
                        double angle = Math.atan2(dy, dx);
                        while (glyphIndex < numGlyphs && distance >= thresholdDist) {
                            Shape glyph = glyphVector.getGlyphOutline(glyphIndex);
                            Point2D p = glyphVector.getGlyphPosition(glyphIndex);
                            double px = p.getX();
                            double py = p.getY();
                            double x = lastX + thresholdDist * dx / distance;
                            double y = lastY + thresholdDist * dy / distance;
                            double advance = nextAdvance;
                            nextAdvance = glyphIndex < numGlyphs - 1 ? tracking + glyphVector.getGlyphMetrics(glyphIndex + 1).getAdvance() * 0.5f : 0;
                            at.setToTranslation(x, y);
                            at.rotate(angle + rotation);
                            if (sx != 1.0 || sy != 1.0) {
                                at.scale(sx, sy);
                            }
                            if (shx != 0 || shy != 0) {
                                at.shear(-shx, -shy);
                            }
                            at.translate(-px - advance, -py);
                            result.append(at.createTransformedShape(glyph), false);
                            thresholdDist += sxa * (advance + nextAdvance);
                            glyphIndex++;
                        }
                    }
                    thresholdDist -= distance;
                    lastX = thisX;
                    lastY = thisY;
                    break;
            }
            it.next();
        }

        textShape = result;
        Rectangle textShapeBounds = result.getBounds();
        textShapeBounds.grow(effectsWidth, effectsWidth);
        boundingBox = textShapeBounds;

        // text on path handles its own transformations: make sure
        // that a leftover transformed rectangle is not interfering
        // when calculating the rectangle covered by the cached image.
        transformedRect = null;
    }

    public Shape getTextShape() {
        if (isOnPath()) {
            return textShape;
        }

        // This image is created just to get a Graphics2D somehow...
        BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = tmp.createGraphics();
        var imgOrigTransform = g2.getTransform();

        transformGraphics(g2);
        var at = g2.getTransform();
        g2.setTransform(imgOrigTransform); // provideShape must be called with untransformed Graphics
        Shape shape = provideShape(g2);

        g2.dispose();
        tmp.flush();

        return at.createTransformedShape(shape);
    }

    public void setTranslation(int translationX, int translationY) {
        this.translationX = translationX;
        this.translationY = translationY;
        invalidLayout = true;
        invalidShapeTransform = true;
    }

    public void setRotation(double newRotation) {
        boolean change = this.rotation != newRotation;
        this.rotation = newRotation;
        if (change) {
            clearCache();
            invalidLayout = true;
            invalidShape = true;
        }
    }

    public void setAdvancedSettings(double newRelLineHeight,
                                    double sx, double sy,
                                    double shx, double shy) {
        boolean change = this.relLineHeight != newRelLineHeight
            || this.sx != sx || this.sy != sy
            || this.shx != shx || this.shy != shy;
        if (change) {
            this.relLineHeight = newRelLineHeight;
            this.sx = sx;
            this.sy = sy;
            this.shx = shx;
            this.shy = shy;
            clearCache();
            invalidLayout = true;
            invalidShape = true;
        }
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
        copy.lineHeight = lineHeight;
        copy.sx = sx;
        copy.sy = sy;
        copy.shx = shx;
        copy.shy = shy;

        copy.transformedRect = transformedRect;
        copy.boundingBox = boundingBox;
        copy.zeroShape = zeroShape;
        copy.textShape = textShape;
//        copy.extraTransform = extraTransform;

        return copy;
    }

    // Return the text shape relative to (0, 0)
    private Shape provideShape(Graphics2D g2) {
        FontMetrics metrics = g2.getFontMetrics(font);
        FontRenderContext frc = g2.getFontRenderContext();

        Map<TextAttribute, ?> attributes = font.getAttributes();
        boolean hasKerning = KERNING_ON.equals(attributes.get(KERNING));
        boolean hasLigatures = LIGATURES_ON.equals(attributes.get(LIGATURES));
        boolean hasStrikeThrough = STRIKETHROUGH_ON.equals(attributes.get(STRIKETHROUGH));
        boolean hasUnderline = UNDERLINE_ON.equals(attributes.get(UNDERLINE));

        if (lines.length == 1) {
            return getLineShape(lines[0], frc, metrics, hasKerning, hasLigatures, hasUnderline, hasStrikeThrough);
        }

        assert lines.length > 1;
        Area retVal = null;
        for (int i = 0; i < lines.length; i++) {
            Shape lineShape = getLineShape(lines[i], frc, metrics, hasKerning, hasLigatures, hasUnderline, hasStrikeThrough);
            if (i == 0) {
                retVal = new Area(lineShape);
            } else {
                lineShape = Shapes.translate(lineShape, 0, i * lineHeight);
                retVal.add(new Area(lineShape));
            }
        }
        return retVal;
    }

    private Shape getLineShape(String line, FontRenderContext frc, FontMetrics metrics, boolean hasKerning, boolean hasLigatures, boolean hasUnderline, boolean hasStrikeThrough) {
        GlyphVector glyphs;
        if (!hasKerning && !hasLigatures && font.getSize() <= 100) {
            // fix for issue #72: it seems that for some reason 100
            // is a magic number, under which the old way of getting
            // the shape works better
            glyphs = font.createGlyphVector(frc, line);
        } else {
            // partial fix for issue #64 (fixes kerning and ligatures),
            // also see https://community.oracle.com/thread/1289266
            char[] chars = line.toCharArray();
            glyphs = font.layoutGlyphVector(frc, chars, 0,
                chars.length, Font.LAYOUT_LEFT_TO_RIGHT);
        }

        Shape glyphsOutline = glyphs.getOutline(0.0f, metrics.getAscent());

        if (!hasUnderline && !hasStrikeThrough) {
            // simple case: the glyphs contain all of the shape
            return glyphsOutline;
        }

        LineMetrics lineMetrics = font.getLineMetrics(line, frc);
        float ascent = lineMetrics.getAscent();
        Area combinedOutline = new Area(glyphsOutline);
        int stringWidth = metrics.stringWidth(line);

        if (hasUnderline) {
            combinedOutline.add(
                createUnderlineShape(lineMetrics, ascent, stringWidth));
        }

        if (hasStrikeThrough) {
            combinedOutline.add(
                createStrikeThroughShape(lineMetrics, ascent, stringWidth));
        }

        return combinedOutline;
    }

    private static Area createUnderlineShape(LineMetrics lineMetrics, float ascent, int stringWidth) {
        float underlineOffset = lineMetrics.getUnderlineOffset();
        float underlineThickness = lineMetrics.getUnderlineThickness();
        Shape underLineShape = new Rectangle2D.Float(
            0.0f,
            ascent + underlineOffset - underlineThickness / 2.0f,
            stringWidth,
            underlineThickness);
        return new Area(underLineShape);
    }

    private static Area createStrikeThroughShape(LineMetrics lineMetrics, float ascent, int stringWidth) {
        float strikethroughOffset = lineMetrics.getStrikethroughOffset();
        float strikethroughThickness = lineMetrics.getStrikethroughThickness();
        Shape strikethroughShape = new Rectangle2D.Float(
            0.0f,
            ascent + strikethroughOffset - strikethroughThickness / 2.0f,
            stringWidth,
            strikethroughThickness);
        return new Area(strikethroughShape);
    }

    public void setAlignment(BoxAlignment newAlignment) {
        setAlignment(newAlignment.getHorizontal(), newAlignment.getVertical());
    }

    public void setAlignment(HorizontalAlignment newHorAlignment, VerticalAlignment newVerAlignment) {
        boolean change = this.horizontalAlignment != newHorAlignment
            || this.verticalAlignment != newVerAlignment;
        if (change) {
            this.horizontalAlignment = newHorAlignment;
            this.verticalAlignment = newVerAlignment;
            clearCache();
            invalidLayout = true;
            invalidShape = true;
        }
    }

    public void setColor(Color newColor) {
        boolean change = !newColor.equals(this.color);
        this.color = newColor;
        if (change) {
            clearCache();
        }
    }

    public void setText(String newText) {
        boolean change = !Objects.equals(text, newText);
        if (change) {
            this.text = newText == null ? "" : newText;
            lines = newText.split("\n");
            clearCache();
            invalidLayout = true;
            invalidShape = true;
        }
    }

    public void setFont(Font newFont) {
        boolean change = !newFont.equals(this.font);
        this.font = newFont;
        if (change) {
            clearCache();
            invalidLayout = true;
            invalidShape = true;
        }
    }

    public void setEffects(AreaEffects newEffects) {
        boolean change = newEffects != effects;
        if (change) {
            this.effects = newEffects;
            clearCache();
            invalidLayout = true;

            // the translation of the shape depends on the effect width
            invalidShapeTransform = true;

            if (effects != null && effects.isNotEmpty()) {
                effectsWidth = (int) Math.ceil(effects.getMaxEffectThickness());
            } else {
                effectsWidth = 0;
            }
        }
    }

    public boolean isOnPath() {
        return horizontalAlignment == null || verticalAlignment == null;
    }

    public void pathChanged() {
        assert isOnPath();
        clearCache();
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key, this);

        node.addInt("translationX", translationX);
        node.addInt("translationY", translationY);
        node.addDouble("rotation", rotation);
        node.addDouble("sx", sx);
        node.addDouble("sy", sy);
        node.addDouble("shx", shx);
        node.addDouble("shy", shy);

        node.addNullableDebuggable("boundingBox", boundingBox, DebugNodes::createRectangleNode);
        node.addNullableDebuggable("transformedRect", transformedRect);
        node.addNullableProperty("transformedShape", textShape);

        return node;
    }
}

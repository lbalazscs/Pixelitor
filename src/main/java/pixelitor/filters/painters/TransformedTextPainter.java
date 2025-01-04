/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import org.jdesktop.swingx.painter.TextPainter;
import org.jdesktop.swingx.util.GraphicsUtilities;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.colors.Colors;
import pixelitor.compactions.Flip;
import pixelitor.gui.utils.AlignmentSelector;
import pixelitor.gui.utils.BoxAlignment;
import pixelitor.utils.ImageUtils;
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

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_FRACTIONALMETRICS;
import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_DEFAULT;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT;
import static java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_GASP;
import static java.awt.font.TextAttribute.KERNING;
import static java.awt.font.TextAttribute.KERNING_ON;
import static java.awt.font.TextAttribute.LIGATURES;
import static java.awt.font.TextAttribute.LIGATURES_ON;
import static java.awt.font.TextAttribute.STRIKETHROUGH;
import static java.awt.font.TextAttribute.STRIKETHROUGH_ON;
import static java.awt.font.TextAttribute.TRACKING;
import static java.awt.font.TextAttribute.UNDERLINE;
import static java.awt.font.TextAttribute.UNDERLINE_ON;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

/**
 * A class similar to {@link TextPainter}, but it can have an extra
 * translation (so that text layers can be moved with the move tool).
 * It also supports the rotation, scaling and shearing of the text,
 * and text on path rendering.
 */
public class TransformedTextPainter implements Debuggable {
    private HorizontalAlignment horizontalAlignment = HorizontalAlignment.CENTER;
    private VerticalAlignment verticalAlignment = VerticalAlignment.CENTER;
    private int mlpAlignment = AlignmentSelector.LEFT;

    private Font font = null;
    private Color color;

    private String text = "";
    private String[] textLines;

    private int effectsWidth; // width added to accommodate effects

    private AreaEffects effects;

    private int translationX = 0;
    private int translationY = 0;
    private double rotation = 0;
    private double scaleX;
    private double scaleY;
    private double shearX;
    private double shearY;

    private TransformedRectangle transformedRect;
    private Rectangle boundingBox;
    private Shape baseShape;
    private Shape textShape;
    private float lineHeight;
    private double relLineHeight;

    private int origTextWidth;  // max width before rotation

    private SoftReference<BufferedImage> renderCache;

    private boolean invalidLayout = true;
    private boolean invalidShape = true;
    private boolean invalidShapeTransform = true;

    // debug settings
    private static final boolean DISABLE_CACHE = false;
    private static final boolean DEBUG_LAYOUT = false;

    public void paint(Graphics2D g, int width, int height, Composition comp) {
        if (text.isBlank()) {
            return;
        }

        // must be called before updateLayout, even if we paint on the cached image
        setOptimalRenderingHints(g);

        if (invalidLayout) {
            updateLayout(width, height, g, comp);
            if (DEBUG_LAYOUT) {
                Shapes.draw(getBoundingBox(), Color.RED, g);
                if (transformedRect != null) {
                    Shapes.draw(transformedRect.asShape(), Color.BLUE, g);
                }
            }
        }

        if (DISABLE_CACHE) {
            doPaint(g, g.getTransform());
            restoreDefaultRenderingHints(g);
            return;
        }

        Rectangle bounds = getBoundingBox();
        if (bounds.isEmpty()) {
            // A zero-width bounding box can happen
            // for some fonts like "EmojiOne Color".
            return;
        }

        BufferedImage cachedImg = renderCache == null ? null : renderCache.get();
        if (cachedImg == null) {
            // Create the cached image containing the rendered text and effects
            cachedImg = GraphicsUtilities.createCompatibleTranslucentImage(bounds.width, bounds.height);
            Graphics2D cacheG = cachedImg.createGraphics();

            setOptimalRenderingHints(cacheG);
            AffineTransform origTransform = cacheG.getTransform();
            cacheG.translate(-bounds.x, -bounds.y);
            doPaint(cacheG, origTransform);
            cacheG.dispose();

            renderCache = new SoftReference<>(cachedImg);
        }

        restoreDefaultRenderingHints(g);
        g.drawImage(cachedImg, bounds.x, bounds.y, null);
    }

    private static void setOptimalRenderingHints(Graphics2D g) {
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_GASP);
    }

    private static void restoreDefaultRenderingHints(Graphics2D g) {
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_DEFAULT);
        g.setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_DEFAULT);
        g.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_DEFAULT);
    }

    private void clearCache() {
        BufferedImage cache = renderCache == null ? null : renderCache.get();
        if (cache != null) {
            cache.flush();
        }
        renderCache = null;
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

    private Rectangle calcBoundingBox(int textWidth, int textHeight, int width, int height, Graphics2D g) {
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
        transformedRect = new TransformedRectangle(0, 0, textWidth, textHeight, rotation, scaleX, scaleY, shearX, shearY);
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
            rotation, scaleX, scaleY, shearX, shearY);
        transformedRect.align(layout, transformedBounds);

        layout.grow(effectsWidth, effectsWidth);

        return layout;
    }

    private boolean hasNoTransform() {
        return rotation == 0 && scaleX == 1.0 && scaleY == 1.0 && shearX == 0 && shearY == 0;
    }

    private void doPaint(Graphics2D g, AffineTransform origTransform) {
        g.setColor(color);

        if (isOnPath()) {
            g.fill(textShape);
            if (effects != null && effects.hasEnabledEffects()) {
                effects.apply(g, textShape);
            }
            return;
        }

        FontMetrics metrics = g.getFontMetrics(font);
        transformGraphics(g);

        // Draw the lines relative to the bounding box.
        // Use the ascent, because drawY is relative to the baseline.
        float drawY = (float) (metrics.getAscent() + effectsWidth);
        if (textLines.length == 1) {
            g.drawString(text, effectsWidth, drawY);
        } else {
            for (String line : textLines) {
                float drawX = switch (mlpAlignment) {
                    case AlignmentSelector.LEFT -> effectsWidth;
                    case AlignmentSelector.CENTER ->
                        (origTextWidth + 2 * effectsWidth) / 2.0f - metrics.stringWidth(line) / 2.0f;
                    case AlignmentSelector.RIGHT ->
                        (origTextWidth + 2 * effectsWidth) - metrics.stringWidth(line) - effectsWidth;
                    default -> throw new IllegalStateException("alignment: " + mlpAlignment);
                };
                g.drawString(line, drawX, drawY);
                drawY += lineHeight;
            }
        }

        // Paint the effects of an explicitly transformed shape
        // instead of simply painting them on the transformed graphics
        // so that the direction of the drop shadow effect doesn't rotate.
        var tx = g.getTransform();

        //provideShape must be called with untransformed Graphics
        g.setTransform(origTransform);

        if (effects != null && effects.hasEnabledEffects()) {
            if (invalidShape) {
                baseShape = calcUntransformedTextShape(g);
                invalidShape = false;
                invalidShapeTransform = true; // implied
            }
            if (invalidShapeTransform) {
                tx.translate(effectsWidth, effectsWidth);
                textShape = tx.createTransformedShape(baseShape);
                invalidShapeTransform = false;
            }

            effects.apply(g, textShape);
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
            if (scaleX != 1.0 || scaleY != 1.0) {
                g.scale(scaleX, scaleY);
            }
            if (shearX != 0 || shearY != 0) {
                g.shear(-shearX, -shearY);
            }
        }
    }

    private void updateLayout(int width, int height, Graphics2D g, Composition comp) {
        if (isOnPath()) {
            if (comp == null) { // the text filter, not a text layer
                comp = Views.getActiveComp();
            }
            renderOnPath(comp.getActivePath().toImageSpaceShape(), g);
            return;
        }

        FontMetrics metrics = g.getFontMetrics(font);
        int textWidth = 0;
        double fontLineHeight = metrics.getStringBounds(textLines[0], g).getHeight();
        lineHeight = (float) (fontLineHeight * relLineHeight);

        // doesn't count the adjustment of the last line
        int textHeight = (int) (fontLineHeight + lineHeight * (textLines.length - 1));

        for (String line : textLines) {
            int lineWidth = metrics.stringWidth(line);
            if (lineWidth > textWidth) {
                textWidth = lineWidth;
            }
        }
        boundingBox = calcBoundingBox(textWidth, textHeight, width, height, g);
        invalidLayout = false;

        origTextWidth = textWidth;
    }

    private void renderOnPath(Path2D path, Graphics2D g2) {
        g2.setFont(font);
        FontRenderContext frc = g2.getFontRenderContext();
        GlyphVector glyphVector = font.createGlyphVector(frc, text);

        textShape = distributeGlyphsAlongPath(glyphVector, path);

        Rectangle textShapeBounds = textShape.getBounds();
        textShapeBounds.grow(effectsWidth, effectsWidth);
        boundingBox = textShapeBounds;

        // text on path handles its own transformations: make sure
        // that a leftover transformed rectangle is not interfering
        // when calculating the rectangle covered by the cached image.
        transformedRect = null;
    }

    private double calcTextLength(GlyphVector glyphVector, double tracking) {
        double totalTextLength = 0;
        int numGlyphs = glyphVector.getNumGlyphs();

        for (int i = 0; i < numGlyphs; i++) {
            totalTextLength += glyphVector.getGlyphMetrics(i).getAdvance() * Math.abs(scaleX);
            if (i < numGlyphs - 1) {
                totalTextLength += tracking;
            }
        }
        return totalTextLength;
    }

    private Path2D distributeGlyphsAlongPath(GlyphVector glyphVector, Path2D path) {
        double pathLength = Shapes.calcPathLength(path);
        double tracking = calcTracking();
        double textLength = calcTextLength(glyphVector, tracking);

        // calculate the initial offset based on alignment
        int startGlyphIndex = 0;
        int numGlyphs = glyphVector.getNumGlyphs();

        double initialOffset = switch (mlpAlignment) {
            case AlignmentSelector.LEFT -> 0;
            case AlignmentSelector.CENTER -> (pathLength - textLength) / 2;
            case AlignmentSelector.RIGHT -> pathLength - textLength;
            default -> throw new IllegalStateException("Unexpected value: " + mlpAlignment);
        };

        // handle text overflow for right or center alignment
        if (initialOffset < 0 && (mlpAlignment == AlignmentSelector.RIGHT || mlpAlignment == AlignmentSelector.CENTER)) {
            // calculate how many glyphs we need to skip
            double accumulatedWidth = 0;
            double overflow = -initialOffset;
            if (mlpAlignment == AlignmentSelector.CENTER) {
                overflow = overflow / 2;
            }
            while (startGlyphIndex < numGlyphs && accumulatedWidth < overflow) {
                accumulatedWidth += glyphVector.getGlyphMetrics(startGlyphIndex).getAdvance() * Math.abs(scaleX);
                if (startGlyphIndex < numGlyphs - 1) {
                    accumulatedWidth += tracking;
                }
                startGlyphIndex++;
            }
            initialOffset = 0;
        }

        Path2D result = new Path2D.Float();
        PathIterator it = new FlatteningPathIterator(path.getPathIterator(null), 1);
        double[] points = new double[6];

        // the coordinates of the starting point of a path segment
        double moveX = 0, moveY = 0;

        // the coordinates of the last processed point
        double lastX = 0, lastY = 0;

        // the coordinates of the current point
        double thisX, thisY;

        int type;
        double currentDist = 0;
        double thresholdDist = initialOffset;

        int glyphIndex = startGlyphIndex;
        AffineTransform at = new AffineTransform();

        while (glyphIndex < numGlyphs && !it.isDone()) {
            type = it.currentSegment(points);
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    moveX = lastX = points[0];
                    moveY = lastY = points[1];
                    result.moveTo(moveX, moveY);
                    break;

                case PathIterator.SEG_CLOSE:
                    points[0] = moveX;
                    points[1] = moveY;
                    // fall through

                case PathIterator.SEG_LINETO:
                    thisX = points[0];
                    thisY = points[1];
                    double dx = thisX - lastX;
                    double dy = thisY - lastY;
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    currentDist += distance;

                    if (currentDist >= thresholdDist) {
                        double angle = Math.atan2(dy, dx);
                        while (glyphIndex < numGlyphs && currentDist >= thresholdDist) {
                            Shape glyph = glyphVector.getGlyphOutline(glyphIndex);
                            Point2D origGlyphPos = glyphVector.getGlyphPosition(glyphIndex);
                            double ratio = (thresholdDist - (currentDist - distance)) / distance;
                            double x = lastX + ratio * dx;
                            double y = lastY + ratio * dy;

                            at.setToTranslation(x, y);
                            at.rotate(angle + rotation);
                            if (scaleX != 1.0 || scaleY != 1.0) {
                                at.scale(scaleX, scaleY);
                            }
                            if (shearX != 0 || shearY != 0) {
                                at.shear(-shearX, -shearY);
                            }
                            at.translate(-origGlyphPos.getX(), -origGlyphPos.getY());
                            result.append(at.createTransformedShape(glyph), false);

                            // calculate next threshold
                            double advance = glyphVector.getGlyphMetrics(glyphIndex).getAdvance() * Math.abs(scaleX);
                            if (glyphIndex < numGlyphs - 1) {
                                advance += tracking;
                            }
                            thresholdDist += advance;
                            glyphIndex++;
                        }
                    }
                    lastX = thisX;
                    lastY = thisY;
                    break;
            }
            it.next();
        }
        return result;
    }

    private double calcTracking() {
        Float trackingValue = (Float) font.getAttributes().get(TRACKING);
        return trackingValue == null
            ? 0.0
            : trackingValue * font.getSize() * Math.abs(scaleX);
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
        Shape shape = calcUntransformedTextShape(g2);

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
                                    double scaleX, double scaleY,
                                    double shearX, double shearY) {
        boolean change = this.relLineHeight != newRelLineHeight
            || this.scaleX != scaleX || this.scaleY != scaleY
            || this.shearX != shearX || this.shearY != shearY;
        if (change) {
            this.relLineHeight = newRelLineHeight;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.shearX = shearX;
            this.shearY = shearY;
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
        copy.scaleX = scaleX;
        copy.scaleY = scaleY;
        copy.shearX = shearX;
        copy.shearY = shearY;

        copy.transformedRect = transformedRect;
        copy.boundingBox = boundingBox;
        copy.baseShape = baseShape;
        copy.textShape = textShape;

        return copy;
    }

    // Return the text shape relative to (0, 0)
    private Shape calcUntransformedTextShape(Graphics2D g2) {
        FontMetrics metrics = g2.getFontMetrics(font);
        FontRenderContext frc = g2.getFontRenderContext();

        Map<TextAttribute, ?> attributes = font.getAttributes();
        boolean hasKerning = KERNING_ON.equals(attributes.get(KERNING));
        boolean hasLigatures = LIGATURES_ON.equals(attributes.get(LIGATURES));
        boolean hasStrikeThrough = STRIKETHROUGH_ON.equals(attributes.get(STRIKETHROUGH));
        boolean hasUnderline = UNDERLINE_ON.equals(attributes.get(UNDERLINE));

        if (textLines.length == 1) {
            return getLineShape(textLines[0], frc, metrics, hasKerning, hasLigatures, hasUnderline, hasStrikeThrough);
        }

        assert textLines.length > 1;
        Area fullShape = null;
        for (int i = 0; i < textLines.length; i++) {
            String line = textLines[i];
            Shape lineShape = getLineShape(line, frc, metrics, hasKerning, hasLigatures, hasUnderline, hasStrikeThrough);
            double tx = switch (mlpAlignment) {
                case AlignmentSelector.LEFT -> 0;
                case AlignmentSelector.CENTER -> origTextWidth / 2.0 - metrics.stringWidth(line) / 2.0;
                case AlignmentSelector.RIGHT -> origTextWidth - metrics.stringWidth(line);
                default -> throw new IllegalStateException("alignment: " + mlpAlignment);
            };
            if (i == 0) {
                if (tx != 0) {
                    lineShape = Shapes.translate(lineShape, tx, 0);
                }
                fullShape = new Area(lineShape);
            } else {
                lineShape = Shapes.translate(lineShape, tx, i * lineHeight);
                fullShape.add(new Area(lineShape));
            }
        }
        return fullShape;
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

    public void setMLPAlignment(int mlpAlignment) {
        boolean change = this.mlpAlignment != mlpAlignment;
        if (change) {
            this.mlpAlignment = mlpAlignment;
            clearCache();
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
            textLines = newText.split("\n");
            for (int i = 0; i < textLines.length; i++) {
                textLines[i] = textLines[i].trim();
            }
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

            if (effects != null && effects.hasEnabledEffects()) {
                effectsWidth = (int) Math.ceil(effects.calcMaxEffectThickness());
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

    public BufferedImage watermarkImage(BufferedImage src, Composition comp) {
        BufferedImage bumpImage = createBumpMapImage(
            src.getWidth(), src.getHeight(), comp);
        return ImageUtils.bumpMap(src, bumpImage, "Watermarking");
    }

    // the bump map image has white text on a black background
    private BufferedImage createBumpMapImage(int width, int height, Composition comp) {
        BufferedImage bumpImage = new BufferedImage(width, height, TYPE_INT_RGB);
        Graphics2D g = bumpImage.createGraphics();
        Colors.fillWith(BLACK, g, width, height);
        setColor(WHITE);
        paint(g, width, height, comp);
        g.dispose();

        return bumpImage;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key, this);

        node.addInt("translation x", translationX);
        node.addInt("translation y", translationY);
        node.addDouble("rotation", rotation);
        node.addDouble("scale x", scaleX);
        node.addDouble("scale y", scaleY);
        node.addDouble("shear x", shearX);
        node.addDouble("shear y", shearY);

        node.addNullableDebuggable("boundingBox", boundingBox, DebugNodes::createRectangleNode);
        node.addNullableDebuggable("transformedRect", transformedRect);
        node.addNullableProperty("transformedShape", textShape);

        return node;
    }
}

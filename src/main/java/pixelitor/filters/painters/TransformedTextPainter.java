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
import pixelitor.AppMode;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.colors.Colors;
import pixelitor.compactions.FlipDirection;
import pixelitor.compactions.QuadrantAngle;
import pixelitor.gui.utils.BoxAlignment;
import pixelitor.gui.utils.MlpAlignmentSelector;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Shapes;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.DebugNodes;
import pixelitor.utils.debug.Debuggable;

import java.awt.*;
import java.awt.font.*;
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

    // alignment for multiline text or text on a path
    private int mlpAlignment = MlpAlignmentSelector.LEFT;

    private Font font = null;
    private Color color;

    private String text = "";
    private String[] textLines;

    private int effectsPadding; // extra space around the text for effects

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
    private Shape textShape;
    private float lineHeight;
    private double relLineHeight;

    // max width of the text block before transformations
    private int origTextWidth;

    private SoftReference<BufferedImage> renderCache;

    private boolean invalidLayout = true;

    // debug settings
    private static final boolean DISABLE_CACHE = false;
    private static final boolean DEBUG_LAYOUT = false;

    public void paint(Graphics2D g, int width, int height, Composition comp) {
        if (text.isBlank()) {
            return;
        }

        // must be called before updateLayout, even if we paint on the cached image
        setHighQualityRendering(g);

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
            paintText(g, g.getTransform());
            restoreDefaultRendering(g);
            return;
        }

        Rectangle bounds = getBoundingBox();
        if (bounds.isEmpty()) {
            // a zero-width bounding box can happen for some fonts like "EmojiOne Color"
            return;
        }

        BufferedImage cachedImg = renderCache == null ? null : renderCache.get();
        if (cachedImg == null) {
            // create the cached image containing the rendered text and effects
            cachedImg = GraphicsUtilities.createCompatibleTranslucentImage(bounds.width, bounds.height);
            Graphics2D cacheG = cachedImg.createGraphics();

            setHighQualityRendering(cacheG);
            AffineTransform origTransform = cacheG.getTransform();
            cacheG.translate(-bounds.x, -bounds.y);
            paintText(cacheG, origTransform);
            cacheG.dispose();

            renderCache = new SoftReference<>(cachedImg);
        }

        restoreDefaultRendering(g);
        g.drawImage(cachedImg, bounds.x, bounds.y, null);
    }

    private static void setHighQualityRendering(Graphics2D g) {
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_GASP);
    }

    private static void restoreDefaultRendering(Graphics2D g) {
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
     * Returns the last painted bounding box for the rendered text.
     * Note that this is an approximation.
     */
    public Rectangle getBoundingBox() {
        return transformedRect != null ? transformedRect.getBoundingBox() : boundingBox;
    }

    /**
     * Returns last painted shape of the rendered text's bounding box.
     */
    public Shape getBoundingShape() {
        return transformedRect != null ? transformedRect.asShape() : boundingBox;
    }

    /**
     * Calculates the position and size of a content area within
     * a container based on the painter's alignment settings.
     */
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

    /**
     * Calculates the final bounding box for the text, accounting
     * for transformations, translation, and padding for effects.
     */
    private Rectangle calcBoundingBox(int textWidth, int textHeight, int width, int height) {
        if (hasNoTransform()) {
            Rectangle layout = calcAlignment(textWidth, textHeight, width, height);
            transformedRect = null;

            // support the Move tool
            layout.translate(translationX, translationY);

            // The effect padding is added only after considering
            // the alignment. This means that in non-centered positions
            // the effects don't fit into the canvas, but the text
            // itself isn't shifted as effects are added.
            if (effectsPadding != 0) {
                layout.grow(effectsPadding, effectsPadding);
            }

            return layout;
        }

        // first, calculate a transformed rectangle starting at 0, 0
        // (ignoring the effects) to get its bounds
        transformedRect = new TransformedRectangle(0, 0,
            textWidth, textHeight, rotation, scaleX, scaleY, shearX, shearY);
        Rectangle transformedBounds = transformedRect.getBoundingBox();

        // use the transformed bounds to calculate the correct layout
        Rectangle layout = calcAlignment(transformedBounds.width, transformedBounds.height, width, height);

        // support the Move tool
        layout.translate(translationX, translationY);

        if (effectsPadding == 0) {
            // also correct the transformed rectangle's position,
            // as it will be used for painting
            transformedRect.align(layout, transformedBounds);
            return layout;
        }

        // if we have both transformation and effects, we need to
        // recalculate the transformed rectangle to include
        // the padding for the effects
        transformedRect = new TransformedRectangle(
            -effectsPadding, -effectsPadding,
            textWidth + 2 * effectsPadding,
            textHeight + 2 * effectsPadding,
            rotation, scaleX, scaleY, shearX, shearY);
        transformedRect.align(layout, transformedBounds);

        layout.grow(effectsPadding, effectsPadding);

        return layout;
    }

    private boolean hasNoTransform() {
        return rotation == 0 && scaleX == 1.0 && scaleY == 1.0 && shearX == 0 && shearY == 0;
    }

    /**
     * Renders the text, with all transformations and effects, onto the given Graphics2D.
     */
    private void paintText(Graphics2D g, AffineTransform origTransform) {
        g.setColor(color);

        boolean hasEffects = effects != null && effects.hasEnabledEffects();

        if (isOnPath()) {
            g.fill(textShape);
            if (hasEffects) {
                effects.apply(g, textShape);
            }
            return;
        }

        transformGraphics(g);

        Shape baseShape = calcUntransformedTextShape(g);
        g.fill(baseShape);

        // paint the effects
        var tx = g.getTransform();
        g.setTransform(origTransform);

        if (hasEffects) {
            // Transform the local shape into the final screen/cache coordinates.
            textShape = tx.createTransformedShape(baseShape);
            // Apply effects to the final, transformed shape.
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
        paintText(g2, g2.getTransform());
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
        FontRenderContext frc = g.getFontRenderContext();

        // the line height is needed for both height calculation and painting
        double fontLineHeight = metrics.getHeight();
        lineHeight = (float) (fontLineHeight * relLineHeight);

        // calculate the visual width of the entire text block
        Map<TextAttribute, ?> attributes = font.getAttributes();
        boolean hasKerning = KERNING_ON.equals(attributes.get(KERNING));
        boolean hasLigatures = LIGATURES_ON.equals(attributes.get(LIGATURES));
        double maxVisualWidth = 0;
        for (String line : textLines) {
            Shape lineShape = getLineShape(line, frc, metrics, hasKerning, hasLigatures, false, false);
            maxVisualWidth = Math.max(maxVisualWidth, lineShape.getBounds2D().getWidth());
        }
        origTextWidth = (int) Math.ceil(maxVisualWidth);

        // calculate the final text block dimensions using font metrics
        int textWidth = origTextWidth;
        int textHeight;
        if (textLines.length == 0) {
            textHeight = 0;
        } else {
            // height is from the top of the first line's ascent to the bottom of the last line's descent
            float ascent = metrics.getAscent();
            float descent = metrics.getDescent();
            textHeight = (int) Math.ceil(ascent + ((textLines.length - 1) * lineHeight) + descent);
        }

        // calculate the final bounding box, which includes padding for effects
        boundingBox = calcBoundingBox(textWidth, textHeight, width, height);
        invalidLayout = false;
    }

    private void renderOnPath(Path2D path, Graphics2D g2) {
        g2.setFont(font);
        FontRenderContext frc = g2.getFontRenderContext();
        GlyphVector glyphVector = font.createGlyphVector(frc, text);

        textShape = distributeGlyphsAlongPath(glyphVector, path);

        Rectangle textShapeBounds = textShape.getBounds();
        textShapeBounds.grow(effectsPadding, effectsPadding);
        boundingBox = textShapeBounds;

        // text on path handles its own transformations: make sure
        // that a leftover transformed rectangle is not interfering
        // when calculating the rectangle covered by the cached image
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

    // arranges the individual glyphs of a text along the given path
    private Path2D distributeGlyphsAlongPath(GlyphVector glyphVector, Path2D path) {
        double tracking = calcTracking();

        double initialOffset = calcPathStartOffset(glyphVector, path, tracking);
        int numGlyphs = glyphVector.getNumGlyphs();

        // handle text overflow for right or center alignment
        int startGlyphIndex = 0;
        if (initialOffset < 0) {
            // calculate how many glyphs we need to skip so the visible
            // part of the text starts at the beginning of the path
            startGlyphIndex = findStartGlyphForOverflow(
                glyphVector, -initialOffset, numGlyphs, tracking);
            initialOffset = 0;
        }

        Path2D result = new Path2D.Double();
        PathIterator pathIterator = new FlatteningPathIterator(path.getPathIterator(null), 1);
        double[] points = new double[6];

        // coordinates of the last processed point
        double lastX = 0, lastY = 0;

        double currentPathDist = 0;
        double segmentDist = 0;
        double dx = 0, dy = 0;

        // the distance along the path where the left edge of the current glyph should be
        double nextGlyphLeftEdgeDist = initialOffset;

        // prime the iterator to get the first segment
        if (!pathIterator.isDone()) {
            pathIterator.currentSegment(points);
            lastX = points[0];
            lastY = points[1];
            pathIterator.next();
        }

        // iterate through each glyph and find its place on the path.
        for (int glyphIndex = startGlyphIndex; glyphIndex < numGlyphs; glyphIndex++) {
            GlyphMetrics metrics = glyphVector.getGlyphMetrics(glyphIndex);
            double advance = metrics.getAdvance() * Math.abs(scaleX);
            double halfAdvance = advance / 2.0;

            // calculate the target distance for the glyph's center
            double centerTargetDist = nextGlyphLeftEdgeDist + halfAdvance;

            // advance along the path until we find the segment containing the center point
            while (!pathIterator.isDone() && currentPathDist + segmentDist < centerTargetDist) {
                currentPathDist += segmentDist;

                int type = pathIterator.currentSegment(points);
                if (type == PathIterator.SEG_LINETO) {
                    dx = points[0] - lastX;
                    dy = points[1] - lastY;
                    segmentDist = Math.sqrt(dx * dx + dy * dy);
                    lastX = points[0];
                    lastY = points[1];
                } else if (type == PathIterator.SEG_MOVETO) {
                    // handle disjoint paths: reset distance calculation for the new sub-path
                    dx = 0;
                    dy = 0;
                    segmentDist = 0;
                    lastX = points[0];
                    lastY = points[1];
                }
                pathIterator.next();
            }

            // if we ran out of path, stop placing glyphs
            if (centerTargetDist > currentPathDist + segmentDist) {
                break;
            }

            // calculate the anchor point and angle on the current segment
            double distIntoSegment = centerTargetDist - currentPathDist;
            double ratio = (segmentDist == 0) ? 0 : distIntoSegment / segmentDist;

            double x = lastX - dx + (ratio * dx); // anchor point X
            double y = lastY - dy + (ratio * dy); // anchor point Y
            double tangentAngle = Math.atan2(dy, dx);

            // build the transformation for the glyph
            AffineTransform glyphTX = new AffineTransform();
            glyphTX.setToTranslation(x, y);
            glyphTX.rotate(tangentAngle + rotation);
            if (scaleX != 1.0 || scaleY != 1.0) {
                glyphTX.scale(scaleX, scaleY);
            }
            if (shearX != 0 || shearY != 0) {
                glyphTX.shear(-shearX, -shearY);
            }

            // translate the glyph locally so its baseline-center is at the origin
            Point2D origGlyphPos = glyphVector.getGlyphPosition(glyphIndex);
            double unscaledHalfAdvance = metrics.getAdvance() / 2.0;
            glyphTX.translate(-(origGlyphPos.getX() + unscaledHalfAdvance), -origGlyphPos.getY());

            Shape glyph = glyphVector.getGlyphOutline(glyphIndex);
            result.append(glyphTX.createTransformedShape(glyph), false);

            // update the position for the next glyph's left edge
            nextGlyphLeftEdgeDist += advance;
            if (glyphIndex < numGlyphs - 1) {
                nextGlyphLeftEdgeDist += tracking;
            }
        }
        return result;
    }

    // calculates extra spacing between glyphs based on font attributes and scaling
    private double calcTracking() {
        Float trackingValue = (Float) font.getAttributes().get(TRACKING);
        return trackingValue == null
            ? 0.0
            : trackingValue * font.getSize() * Math.abs(scaleX);
    }

    /**
     * Calculates the initial distance along the path where the
     * text should begin, based on the current alignment setting.
     */
    private double calcPathStartOffset(GlyphVector glyphVector, Path2D path, double tracking) {
        if (mlpAlignment == MlpAlignmentSelector.LEFT) {
            return 0;
        }

        double pathLength = Shapes.calcPathLength(path);
        double textLength = calcTextLength(glyphVector, tracking);

        return switch (mlpAlignment) {
            case MlpAlignmentSelector.CENTER -> (pathLength - textLength) / 2;
            case MlpAlignmentSelector.RIGHT -> pathLength - textLength;
            default -> throw new IllegalStateException("Unexpected value: " + mlpAlignment);
        };
    }

    /**
     * Determines the index of the first glyph to render when the text
     * overflows the path, effectively clipping the beginning of the text.
     */
    private int findStartGlyphForOverflow(GlyphVector glyphVector, double overflow, int numGlyphs, double tracking) {
        int startGlyphIndex = 0;
        double accumulatedWidth = 0;

        // for centered text, the overflow is on both sides,
        // so we only need to account for half of it
        if (mlpAlignment == MlpAlignmentSelector.CENTER) {
            overflow /= 2;
        }

        while (startGlyphIndex < numGlyphs && accumulatedWidth < overflow) {
            accumulatedWidth += glyphVector.getGlyphMetrics(startGlyphIndex).getAdvance() * Math.abs(scaleX);
            if (startGlyphIndex < numGlyphs - 1) {
                accumulatedWidth += tracking;
            }
            startGlyphIndex++;
        }
        return startGlyphIndex;
    }

    public Shape getTextShape(Composition comp) {
        // This image is created just to get a Graphics2D somehow...
        BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = tmp.createGraphics();

        if (invalidLayout) {
            // normally we call this only for text layers that are already laid out
            assert AppMode.isUnitTesting();

            updateLayout(comp.getCanvasWidth(), comp.getCanvasHeight(), g2, comp);
        }

        if (isOnPath()) {
            // for text on a path, updateLayout has already computed the final shape
            return textShape;
        }

        var imgOrigTransform = g2.getTransform();

        transformGraphics(g2);
        var at = g2.getTransform();
        g2.setTransform(imgOrigTransform); // provideShape must be called with untransformed Graphics
        Shape shape = calcUntransformedTextShape(g2);

        g2.dispose();
        tmp.flush();

        return at.createTransformedShape(shape);
    }

    public void setTranslation(int newTranslationX, int newTranslationY) {
        this.translationX = newTranslationX;
        this.translationY = newTranslationY;
        invalidLayout = true;
    }

    public void setRotation(double newRotation) {
        if (this.rotation != newRotation) {
            this.rotation = newRotation;
            clearCache();
            invalidLayout = true;
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
        }
    }

    public void flip(FlipDirection direction, Canvas canvas) {
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
        boolean hasUnderline = UNDERLINE_ON.equals(attributes.get(UNDERLINE));
        boolean hasStrikeThrough = STRIKETHROUGH_ON.equals(attributes.get(STRIKETHROUGH));

        Path2D fullShape = new Path2D.Float();

        float currentY = effectsPadding + metrics.getAscent();

        for (String line : textLines) {
            Shape lineShape = getLineShape(line, frc, metrics, hasKerning, hasLigatures, hasUnderline, hasStrikeThrough);
            Rectangle2D lineBounds = lineShape.getBounds2D();

            float drawX = switch (mlpAlignment) {
                case MlpAlignmentSelector.LEFT -> effectsPadding;
                case MlpAlignmentSelector.CENTER ->
                    effectsPadding + (origTextWidth - (float) lineBounds.getWidth()) / 2.0f;
                case MlpAlignmentSelector.RIGHT -> effectsPadding + origTextWidth - (float) lineBounds.getWidth();
                default -> throw new IllegalStateException("alignment: " + mlpAlignment);
            };

            AffineTransform lineTransform = AffineTransform.getTranslateInstance(
                drawX - lineBounds.getX(),
                currentY
            );

            Shape positionedLineShape = lineTransform.createTransformedShape(lineShape);
            fullShape.append(positionedLineShape.getPathIterator(null), false);

            currentY += lineHeight;
        }
        return fullShape;
    }

    /**
     * Creates the combined shape for a single line of text, including
     * its glyphs and any active decorations like underlines or strikethroughs.
     */
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

        // The y-parameter to getOutline is the baseline. By setting it to 0,
        // we make the shape's coordinate system's origin align with the baseline.
        Shape glyphsOutline = glyphs.getOutline(0.0f, 0.0f);

        if (!hasUnderline && !hasStrikeThrough) {
            // simple case: the glyphs contain all of the shape
            return glyphsOutline;
        }

        // uses Area instead of Path2D.append to ensure that
        // self-intersecting paths don't create unfilled holes
        // and effects are painted as if the underline/strikethrough
        // was part of the font
        Area combinedOutline = new Area(glyphsOutline);

        LineMetrics lineMetrics = font.getLineMetrics(line, frc);
        int stringWidth = metrics.stringWidth(line);

        if (hasUnderline) {
            combinedOutline.add(
                createUnderlineShape(lineMetrics, stringWidth));
        }

        if (hasStrikeThrough) {
            combinedOutline.add(
                createStrikethroughShape(lineMetrics, stringWidth));
        }

        return combinedOutline;
    }

    /**
     * Generates the rectangular shape for an underline.
     */
    private static Area createUnderlineShape(LineMetrics lineMetrics, int stringWidth) {
        float underlineOffset = lineMetrics.getUnderlineOffset();
        float underlineThickness = lineMetrics.getUnderlineThickness();
        Shape underLineShape = new Rectangle2D.Float(
            0.0f,
            underlineOffset - underlineThickness / 2.0f,
            stringWidth,
            underlineThickness);
        return new Area(underLineShape);
    }

    /**
     * Generates the rectangular shape for a strikethrough.
     */
    private static Area createStrikethroughShape(LineMetrics lineMetrics, int stringWidth) {
        float strikethroughOffset = lineMetrics.getStrikethroughOffset();
        float strikethroughThickness = lineMetrics.getStrikethroughThickness();
        Shape strikethroughShape = new Rectangle2D.Float(
            0.0f,
            strikethroughOffset - strikethroughThickness / 2.0f,
            stringWidth,
            strikethroughThickness);
        return new Area(strikethroughShape);
    }

    public void setBoxAlignment(BoxAlignment newAlignment) {
        setBoxAlignment(newAlignment.getHorizontal(), newAlignment.getVertical());
    }

    public void setBoxAlignment(HorizontalAlignment newHorAlignment, VerticalAlignment newVerAlignment) {
        boolean change = this.horizontalAlignment != newHorAlignment
            || this.verticalAlignment != newVerAlignment;
        if (change) {
            this.horizontalAlignment = newHorAlignment;
            this.verticalAlignment = newVerAlignment;
            clearCache();
            invalidLayout = true;
        }
    }

    public void setMLPAlignment(int mlpAlignment) {
        boolean change = this.mlpAlignment != mlpAlignment;
        if (change) {
            this.mlpAlignment = mlpAlignment;
            clearCache();
            invalidLayout = true;
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
            clearCache();
            invalidLayout = true;
        }
    }

    public void setFont(Font newFont) {
        boolean change = !newFont.equals(this.font);
        this.font = newFont;
        if (change) {
            clearCache();
            invalidLayout = true;
        }
    }

    public void setEffects(AreaEffects newEffects) {
        boolean change = newEffects != effects;
        if (change) {
            this.effects = newEffects;
            clearCache();
            invalidLayout = true;

            if (effects != null && effects.hasEnabledEffects()) {
                effectsPadding = (int) effects.calcMaxEffectPadding();
            } else {
                effectsPadding = 0;
            }
        }
    }

    public boolean isOnPath() {
        return horizontalAlignment == null || verticalAlignment == null;
    }

    public void pathChanged() {
        assert isOnPath();
        clearCache();
        invalidLayout = true;
    }

    public BufferedImage createWatermark(BufferedImage src, Composition comp) {
        BufferedImage bumpImage = createBumpMap(
            src.getWidth(), src.getHeight(), comp);
        return ImageUtils.bumpMap(src, bumpImage, "Watermarking");
    }

    // the bump map image has white text on a black background
    private BufferedImage createBumpMap(int width, int height, Composition comp) {
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

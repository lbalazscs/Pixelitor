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

package pixelitor.filters.painters;

import org.jdesktop.swingx.painter.TextPainter;
import org.jdesktop.swingx.util.GraphicsUtilities;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.colors.Colors;
import pixelitor.filters.painters.TextOnPathLayouter.OnPathKey;
import pixelitor.filters.painters.TextOnPathLayouter.OnPathLayout;
import pixelitor.gui.utils.BoxAlignment;
import pixelitor.gui.utils.MlpAlignmentSelector;
import pixelitor.tools.pen.Path;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.PathSampler;
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
import static java.awt.RenderingHints.*;
import static java.awt.font.TextAttribute.*;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

/**
 * A class similar to {@link TextPainter}, but it can have an extra
 * translation (so that text layers can be moved with the move tool).
 * It also supports the rotation, scaling and shearing of the text,
 * and text-on-a-path rendering.
 *
 * <p>The layout of normal (not on a path) text is calculated in two
 * stages, so that cheap changes don't redo expensive work:</p>
 * <ol>
 *   <li><b>Measure</b> (expensive: glyph layout, kerning, ligatures,
 *   underline areas). Produces a {@link TextMeasurement}: the text
 *   block size and the untransformed text shape. It depends only on
 *   the text, the font, the multi-line alignment, the relative line
 *   height and the effects padding, and it is dropped (set to null)
 *   by exactly those setters.</li>
 *   <li><b>Placement</b> (cheap). Uses the cached measurement to
 *   calculate the bounding box, the transformed rectangle, the render
 *   bounds and the placed shape (the measured shape mapped to canvas
 *   coordinates by the placement transform). It depends additionally
 *   on the translation, the box alignment, the rotation/scale/shear
 *   and the canvas size.</li>
 * </ol>
 * <p>Everything that needs the text geometry ({@link #paintText},
 * {@link #getTextShape}, {@link #renderArea}) reuses the placed shape
 * instead of recalculating it. Text on a path also ends up as a shape in
 * canvas coordinates, so both kinds of text are painted the same way:
 * the fill and the effects use the same shape.</p>
 *
 * <p>Text on a path is laid out differently, because the path is not
 * owned by this painter, and it can be edited or moved without
 * notifying it. The validity of the layout is therefore checked
 * on every query, by comparing the flattened geometry of the path (and
 * the text parameters) with the ones the glyphs were distributed along
 * (see {@link OnPathLayout}). The expensive distribution of the glyphs
 * ({@link TextOnPathLayouter}, a pure calculation) is repeated only if
 * something has really changed, and if the path was only moved, then the
 * existing glyph shapes are reused. This class only validates and caches.</p>
 *
 * <p>Neither kind of layout uses the {@link Graphics2D} given to
 * {@link #paint}: it is used only for drawing, and it is never modified.</p>
 */
public class TransformedTextPainter implements Debuggable {
    /**
     * The result of the measure stage.
     *
     * @param width  the width of the widest line, before transformations
     * @param height the height of the text block, before transformations
     * @param shape  the outlines of all lines (including underline and
     *               strikethrough), relative to the top-left corner of
     *               the text block that is grown by the effects padding.
     *               It is never modified after creation.
     */
    private record TextMeasurement(int width, int height, Shape shape) {
    }

    /**
     * Moving a path by the Move tool recalculates its anchor points
     * from the original ones, so a move that is not an exact translation
     * is a real edit. This is just a tolerance for the floating-point
     * noise (in pixels).
     */
    private static final double SAME_PATH_TOLERANCE = 1e-6;

    private BoxAlignment boxAlignment = BoxAlignment.CENTER_CENTER;

    // alignment for multiline text or text on a path
    private int mlpAlignment = MlpAlignmentSelector.LEFT;

    private Font font = null;
    private Color color;

    private String text = "";
    private String[] textLines = new String[0];

    private int effectsPadding; // extra space around the text for effects

    private AreaEffects effects;

    private int translationX = 0;
    private int translationY = 0;
    private double rotation = 0;
    private double scaleX = 1.0;
    private double scaleY = 1.0;
    private double shearX;
    private double shearY;
    private double relLineHeight = 1.0;

    // Stage 1 (measure): null means invalid. It is not used for text on a path.
    private TextMeasurement measurement;

    // Stage 2 (placement)
    // Text on a path is intentionally never marked as valid, see needsLayout()
    private boolean invalidPlacement = true;
    // the canvas size for which the current placement was calculated
    private int layoutWidth = -1;
    private int layoutHeight = -1;
    private TransformedRectangle transformedRect;
    private Rectangle boundingBox;
    private Rectangle renderBounds; // the actual bounds for the cache image

    // The measured text shape mapped to canvas coordinates (normal text only).
    // It is created once per placement, shared by the bounds calculation,
    // painting and getTextShape(), and never modified. Null until the
    // placement is calculated, and again after it is invalidated.
    private Shape placedShape;

    // Text on a path (not used for normal text):
    // the glyphs, distributed along the path as it was when they were
    // created. Null means invalid, or that there is no path.
    private OnPathLayout onPathLayout;
    // how far the current path is from the one used by onPathLayout
    private double onPathDx;
    private double onPathDy;
    // The final shape of the text on the current path, which is
    // the glyph shape moved by the offset above. It is created lazily.
    private Shape textShape;

    private SoftReference<BufferedImage> renderCache;

    /**
     * Paints the text. The given Graphics2D is used only for drawing,
     * and its state (rendering hints, color, font, transform) is the
     * same after the call as it was before.
     */
    public void paint(Graphics2D g, int width, int height, Composition comp) {
        if (!prepareForPainting(width, height, comp)) {
            return; // blank text
        }

        // not getBoundingBox(), because the layout is already up to date
        Rectangle bounds = currentBounds();
        if (bounds.isEmpty()) {
            // a zero-width bounding box can happen for some fonts like "EmojiOne Color"
            return;
        }

        BufferedImage cachedImg = renderCache == null ? null : renderCache.get();
        if (cachedImg == null) {
            cachedImg = createCachedImage(bounds);
            renderCache = new SoftReference<>(cachedImg);
        }

        g.drawImage(cachedImg, bounds.x, bounds.y, null);
    }

    /**
     * Paints the text directly without using the render cache.
     * The given Graphics2D is used only for drawing, and its state
     * (rendering hints, color, font, transform) is the same after
     * the call as it was before.
     */
    public void paintUncached(Graphics2D g, int width, int height, Composition comp) {
        if (!prepareForPainting(width, height, comp)) {
            return; // blank text
        }

        if (currentBounds().isEmpty()) {
            return;
        }

        paintDirectly(g, color);
    }

    /**
     * Brings the layout up to date for painting on a canvas of the given
     * size. Returns false if there is no text to paint. Shared by
     * {@link #paint} and the watermark bump map, so both see the same layout.
     */
    private boolean prepareForPainting(int width, int height, Composition comp) {
        setCanvasSize(width, height);

        if (text.isBlank()) {
            applyBlankLayout();
            return false;
        }

        // The layout deliberately doesn't get the Graphics2D: normal text
        // is measured with a private one, and text on a path uses
        // TextOnPathLayouter.LAYOUT_FRC.
        // This way the geometry is the same for all callers.
        if (needsLayout()) {
            updateLayout(width, height, comp);
        }
        return true;
    }

    /**
     * Paints on the given Graphics2D without the cache, using the given
     * color instead of the painter's own (which is never modified).
     * It works on a copy, so the caller's graphics isn't affected.
     */
    private void paintDirectly(Graphics2D g, Color paintColor) {
        Graphics2D copy = (Graphics2D) g.create();
        try {
            setHighQualityRendering(copy);
            paintText(copy, paintColor);
        } finally {
            copy.dispose();
        }
    }

    /**
     * Creates the cached image containing the rendered text and effects.
     */
    private BufferedImage createCachedImage(Rectangle bounds) {
        BufferedImage img = GraphicsUtilities.createCompatibleTranslucentImage(
            bounds.width, bounds.height);
        return renderToImage(img, bounds, color);
    }

    private BufferedImage renderToImage(BufferedImage img, Rectangle area, Color paintColor) {
        Graphics2D g = img.createGraphics();
        try {
            setHighQualityRendering(g);
            g.translate(-area.x, -area.y);
            paintText(g, paintColor);
        } finally {
            g.dispose();
        }
        return img;
    }

    private static void setHighQualityRendering(Graphics2D g) {
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_GASP);
    }

    private void clearCache() {
        BufferedImage cache = renderCache == null ? null : renderCache.get();
        if (cache != null) {
            cache.flush();
        }
        renderCache = null;
    }

    /**
     * The text geometry (size or shape) changes:
     * both stages have to be recalculated.
     */
    private void invalidateMeasurement() {
        measurement = null;
        invalidatePlacement();
    }

    /**
     * Only the position or the orientation of the text changes:
     * the measured shape stays valid, but the placed one doesn't.
     */
    private void invalidatePlacement() {
        invalidPlacement = true;
        placedShape = null;
    }

    /**
     * Checks if the canvas size used for placement has changed. The placement
     * depends on it (via the box alignment), but the measurement doesn't.
     */
    private void setCanvasSize(int width, int height) {
        if (width != layoutWidth || height != layoutHeight) {
            invalidatePlacement();
        }
    }

    private boolean needsLayout() {
        // Text on a path is deliberately validated every time: the path
        // can be edited or moved (by the Move tool) without notifying
        // this painter. The validation is cheap, because the glyphs are
        // distributed again only if the text or the shape of the path
        // really changed, see updateOnPathLayout().
        return isOnPath() || measurement == null || invalidPlacement;
    }

    private void applyBlankLayout() {
        if (!invalidPlacement) {
            return;
        }
        boundingBox = new Rectangle();
        renderBounds = new Rectangle();
        transformedRect = null;
        invalidPlacement = false;
    }

    private void ensureLayout(Composition comp) {
        if (comp != null) {
            setCanvasSize(comp.getCanvasWidth(), comp.getCanvasHeight());
        }

        if (text.isBlank()) {
            applyBlankLayout();
            return;
        }

        if (!needsLayout()) {
            return;
        }

        if (comp == null) {
            return;
        }

        int w = comp.getCanvasWidth();
        int h = comp.getCanvasHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        updateLayout(w, h, comp);
    }

    /**
     * Returns the bounding box for the rendered text in the given composition.
     * Note that this is an approximation.
     */
    public Rectangle getBoundingBox(Composition comp) {
        ensureLayout(comp);
        return new Rectangle(currentBounds());
    }

    /**
     * Returns the bounds of the current layout, without validating it.
     */
    private Rectangle currentBounds() {
        if (renderBounds != null) {
            return renderBounds;
        }
        if (transformedRect != null) {
            return transformedRect.getBoundingBox();
        }
        return boundingBox != null ? boundingBox : new Rectangle();
    }

    /**
     * Returns the shape of the rendered text's bounding box in the given composition.
     */
    public Shape getBoundingShape(Composition comp) {
        ensureLayout(comp);
        if (transformedRect != null) {
            return transformedRect.asShape();
        }
        return boundingBox != null ? boundingBox : new Rectangle();
    }

    /**
     * Calculates the position and size of a content area within
     * a container based on the painter's alignment settings.
     */
    private Rectangle calcAlignment(int contentWidth, int contentHeight,
                                    int width, int height) {
        assert !boxAlignment.isPath();

        Rectangle rect = new Rectangle();

        rect.x = switch (boxAlignment.getHorizontal()) {
            case LEFT -> 0;
            case CENTER -> (width - contentWidth) / 2;
            case RIGHT -> width - contentWidth;
        };
        rect.y = switch (boxAlignment.getVertical()) {
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
     * Renders the text, with all effects, onto the given Graphics2D.
     * Both kinds of text are painted the same way: the fill and the
     * effects use the same canvas-space shape under the same transform
     * of the graphics (which is just a translation into the image
     * being rendered). This method assumes that the layout is up to
     * date, and it never measures the text.
     */
    private void paintText(Graphics2D g, Color paintColor) {
        if (text.isBlank()) {
            return;
        }

        Shape shape = getCanvasShape();
        if (shape == null) {
            return; // there is no layout (yet)
        }

        g.setColor(paintColor);
        g.fill(shape);

        // effects that should appear behind the text, such as
        // a drop shadow, are already clipped to the text shape
        if (effects != null && effects.hasEnabledEffects()) {
            effects.apply(g, shape);
        }
    }

    /**
     * Renders a possibly off-canvas image, reusing the already
     * measured text shape (measuring the text again with a different
     * Graphics2D can cause rounding errors in the ORA export).
     */
    public BufferedImage renderArea(Rectangle area, Composition comp) {
        // does nothing if the layout is already up to date
        ensureLayout(comp);

        BufferedImage img = new BufferedImage(
            Math.max(1, area.width), Math.max(1, area.height), BufferedImage.TYPE_INT_ARGB);
        return renderToImage(img, area, color);
    }

    private void updateLayout(int width, int height, Composition comp) {
        if (isOnPath()) {
            updateOnPathLayout(comp);
            // intentionally doesn't validate the placement, see needsLayout()
            return;
        }

        if (measurement == null) {
            measurement = measure();
        }
        place(measurement, width, height);
    }

    /**
     * Stage 2: calculates everything that depends on where the
     * (already measured) text is, without touching the glyphs.
     */
    private void place(TextMeasurement m, int width, int height) {
        // calculate the final bounding box, which includes padding for effects
        boundingBox = calcBoundingBox(m.width(), m.height(), width, height);

        // The only place where the measured shape is mapped to the canvas.
        // The result is kept for painting and getTextShape().
        Shape shape = createPlacementTransform().createTransformedShape(m.shape());

        // calculate actual render bounds to prevent clipping
        Rectangle actualShapeBounds = shape.getBounds();
        if (effectsPadding > 0) {
            actualShapeBounds.grow(effectsPadding, effectsPadding);
        }
        // save to renderBounds instead of mutating boundingBox
        renderBounds = boundingBox.union(actualShapeBounds);

        placedShape = shape;
        layoutWidth = width;
        layoutHeight = height;
        invalidPlacement = false;
    }

    /**
     * Creates the transform that maps the untransformed text shape to
     * the canvas. This method assumes that the bounding box and the
     * transformed rectangle are already calculated.
     */
    private AffineTransform createPlacementTransform() {
        AffineTransform tx = new AffineTransform();
        if (hasNoTransform()) {
            assert transformedRect == null;
            tx.translate(boundingBox.x, boundingBox.y);
        } else {
            assert transformedRect != null;
            tx.translate(transformedRect.getTopLeftX(), transformedRect.getTopLeftY());
            if (rotation != 0) {
                tx.rotate(rotation);
            }
            if (scaleX != 1.0 || scaleY != 1.0) {
                tx.scale(scaleX, scaleY);
            }
            if (shearX != 0 || shearY != 0) {
                tx.shear(-shearX, -shearY);
            }
        }
        return tx;
    }

    /**
     * Stage 1: measures the text and builds the untransformed text shape.
     */
    private TextMeasurement measure() {
        // A private Graphics2D makes the result independent of the one
        // that triggered the measurement (its transform and rendering
        // hints), so that all users of the shape see the same geometry.
        BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = tmp.createGraphics();
        try {
            setHighQualityRendering(g);
            return measure(g.getFontMetrics(font), g.getFontRenderContext());
        } finally {
            g.dispose();
            tmp.flush();
        }
    }

    private TextMeasurement measure(FontMetrics metrics, FontRenderContext frc) {
        // these flags are the same for all lines
        Map<TextAttribute, ?> attributes = font.getAttributes();
        boolean hasKerning = KERNING_ON.equals(attributes.get(KERNING));
        boolean hasLigatures = LIGATURES_ON.equals(attributes.get(LIGATURES));
        boolean hasUnderline = UNDERLINE_ON.equals(attributes.get(UNDERLINE));
        boolean hasStrikeThrough = STRIKETHROUGH_ON.equals(attributes.get(STRIKETHROUGH));
        boolean hasDecoration = hasUnderline || hasStrikeThrough;

        // the line height is needed for both height calculation and painting
        double fontLineHeight = metrics.getHeight();
        float lineHeight = (float) (fontLineHeight * relLineHeight);

        // first pass: create the shape of each line exactly once
        // and find the visual width of the entire text block
        int numLines = textLines.length;
        Shape[] lineShapes = new Shape[numLines];
        Rectangle2D[] bareBounds = new Rectangle2D[numLines];
        double maxVisualWidth = 0;
        for (int i = 0; i < numLines; i++) {
            String line = textLines[i];
            Shape bareShape = createGlyphOutline(line, frc, hasKerning, hasLigatures);
            // anchor layout calculations against the bare shape to prevent bounding jitters
            bareBounds[i] = bareShape.getBounds2D();
            lineShapes[i] = hasDecoration
                ? addDecorations(bareShape, line, frc, metrics, hasUnderline, hasStrikeThrough)
                : bareShape;
            maxVisualWidth = Math.max(maxVisualWidth, bareBounds[i].getWidth());
        }
        int textWidth = (int) Math.ceil(maxVisualWidth);

        // calculate the final text block dimensions using font metrics:
        // height is from the top of the first line's ascent to the bottom of the last line's descent
        assert numLines > 0;
        float ascent = metrics.getAscent();
        float descent = metrics.getDescent();
        int textHeight = (int) Math.ceil(ascent + ((numLines - 1) * lineHeight) + descent);

        // second pass: position the lines, now that the width is known
        Path2D fullShape = new Path2D.Float();
        float currentY = effectsPadding + ascent;
        for (int i = 0; i < numLines; i++) {
            Rectangle2D bareLineBounds = bareBounds[i];

            float drawX = switch (mlpAlignment) {
                case MlpAlignmentSelector.LEFT -> effectsPadding;
                case MlpAlignmentSelector.CENTER ->
                    effectsPadding + (textWidth - (float) bareLineBounds.getWidth()) / 2.0f;
                case MlpAlignmentSelector.RIGHT -> effectsPadding + textWidth - (float) bareLineBounds.getWidth();
                default -> throw new IllegalStateException("alignment: " + mlpAlignment);
            };

            AffineTransform lineTransform = AffineTransform.getTranslateInstance(
                drawX - bareLineBounds.getX(), currentY);

            fullShape.append(lineShapes[i].getPathIterator(lineTransform), false);

            currentY += lineHeight;
        }

        return new TextMeasurement(textWidth, textHeight, fullShape);
    }

    /**
     * Validates the layout of the text on the active path of the given
     * composition, and updates the bounds. The glyphs are distributed
     * along the path again only if the text parameters or the shape of
     * the path have changed. If the path was only moved, the existing
     * glyphs are reused (and only their bounds have to be moved).
     */
    private void updateOnPathLayout(Composition comp) {
        if (comp == null) { // the text filter, not a text layer
            comp = Views.getActiveComp();
        }
        Path path = comp == null ? null : comp.getActivePath();
        if (path == null) {
            // The painter is still in path mode, but there is nothing
            // to follow (for example, the path was just deleted, and
            // the painter wasn't notified yet): behave as if there was no text.
            applyEmptyOnPathLayout();
            return;
        }

        // the only place where the path is flattened for this layout
        PathSampler pathSampler = new PathSampler(path.toImageSpaceShape());
        OnPathKey key = new OnPathKey(text, font, mlpAlignment,
            rotation, scaleX, scaleY, shearX, shearY);

        OnPathLayout layout = onPathLayout;
        Point2D offset = null;
        if (layout != null && layout.key().equals(key)) {
            // The offset is always measured from the path of the layout, not
            // from the previous query, so the rounding errors can't add up.
            offset = pathSampler.offsetFrom(layout.path(), SAME_PATH_TOLERANCE);
        }

        double dx = 0;
        double dy = 0;
        if (offset != null) {
            dx = offset.getX();
            dy = offset.getY();
        } else {
            layout = TextOnPathLayouter.layout(key, pathSampler);
            // the cached image shows the glyphs of the old layout.
            // (This is needed when the path was edited without notification.)
            clearCache();
        }
        placeOnPath(layout, dx, dy);
    }

    /**
     * Makes the given layout, moved by the given offset, the current one,
     * and recalculates the bounds (which depend on the effects padding too).
     */
    private void placeOnPath(OnPathLayout layout, double dx, double dy) {
        if (layout != onPathLayout || dx != onPathDx || dy != onPathDy) {
            onPathLayout = layout;
            onPathDx = dx;
            onPathDy = dy;
            textShape = null; // will be created when needed
        }

        Rectangle2D glyphBounds = layout.glyphBounds();
        Rectangle bounds;
        if (glyphBounds.isEmpty()) {
            // no glyph fits on the path: there is nothing to cache or draw
            bounds = new Rectangle();
        } else {
            bounds = new Rectangle2D.Double(
                glyphBounds.getX() + dx, glyphBounds.getY() + dy,
                glyphBounds.getWidth(), glyphBounds.getHeight()).getBounds();
            bounds.grow(effectsPadding, effectsPadding);
        }
        boundingBox = bounds;
        renderBounds = bounds; // keep them synced on a path

        // text on path handles its own transformations: make sure
        // that a leftover transformed rectangle is not interfering
        // when calculating the rectangle covered by the cached image
        transformedRect = null;
    }

    /**
     * The state for text that has no path to follow.
     */
    private void applyEmptyOnPathLayout() {
        onPathLayout = null;
        textShape = null;
        boundingBox = new Rectangle();
        renderBounds = new Rectangle();
        transformedRect = null;
        clearCache();
    }

    /**
     * Returns the shape of the text on the current path, or null if there
     * is no layout. The returned shape must not be modified.
     */
    private Shape getOnPathShape() {
        OnPathLayout layout = onPathLayout;
        if (layout == null) {
            return null;
        }
        if (textShape == null) {
            textShape = (onPathDx == 0 && onPathDy == 0)
                ? layout.glyphs()
                : AffineTransform.getTranslateInstance(onPathDx, onPathDy)
                .createTransformedShape(layout.glyphs());
        }
        return textShape;
    }

    /**
     * Returns the shape of the text in canvas coordinates (the placed
     * shape for normal text, the glyphs on the current path for text on
     * a path), or null if there is no layout. The returned shape is shared
     * and must not be modified.
     */
    private Shape getCanvasShape() {
        return isOnPath() ? getOnPathShape() : placedShape;
    }

    public Shape getTextShape(Composition comp) {
        if (text.isBlank()) {
            return new Path2D.Double();
        }

        ensureLayout(comp);

        Shape shape = getCanvasShape();
        if (shape == null) {
            return new Path2D.Double(); // the layout couldn't be calculated
        }

        // return a copy, so the cached shape can't be modified by the caller
        return new Path2D.Double(shape);
    }

    public void setTranslation(int newTranslationX, int newTranslationY) {
        if (this.translationX != newTranslationX || this.translationY != newTranslationY) {
            this.translationX = newTranslationX;
            this.translationY = newTranslationY;
            // clearCache() is unnecessary here, and the text doesn't
            // have to be measured again, only moved
            invalidatePlacement();
        }
    }

    public void setRotation(double newRotation) {
        if (this.rotation != newRotation) {
            this.rotation = newRotation;
            clearCache();
            invalidatePlacement();
        }
    }

    public void setAdvancedSettings(double newRelLineHeight,
                                    double scaleX, double scaleY,
                                    double shearX, double shearY) {
        boolean lineHeightChange = this.relLineHeight != newRelLineHeight;
        boolean transformChange = this.scaleX != scaleX || this.scaleY != scaleY
            || this.shearX != shearX || this.shearY != shearY;
        if (lineHeightChange || transformChange) {
            this.relLineHeight = newRelLineHeight;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.shearX = shearX;
            this.shearY = shearY;
            clearCache();
            if (lineHeightChange) {
                // the line height changes the text shape
                invalidateMeasurement();
            } else {
                // scaling and shearing happen only in the placement
                invalidatePlacement();
            }
        }
    }

    /**
     * Creates the outlines of the glyphs of a single line of text.
     * The origin of the returned shape is on the baseline.
     */
    private Shape createGlyphOutline(String line, FontRenderContext frc,
                                     boolean hasKerning, boolean hasLigatures) {
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
        return glyphs.getOutline(0.0f, 0.0f);
    }

    /**
     * Combines the glyph outlines of a line with its underline and/or strikethrough.
     */
    private Shape addDecorations(Shape glyphsOutline, String line,
                                 FontRenderContext frc, FontMetrics metrics,
                                 boolean hasUnderline, boolean hasStrikeThrough) {
        // uses Area instead of Path2D.append to ensure that
        // self-intersecting paths don't create unfilled holes
        // and effects are painted as if the underline/strikethrough
        // was part of the font
        Area combinedOutline = new Area(glyphsOutline);

        LineMetrics lineMetrics = font.getLineMetrics(line, frc);
        int stringWidth = metrics.stringWidth(line);

        if (hasUnderline) {
            combinedOutline.add(decorationLine(
                lineMetrics.getUnderlineOffset(), lineMetrics.getUnderlineThickness(), stringWidth));
        }

        if (hasStrikeThrough) {
            combinedOutline.add(decorationLine(
                lineMetrics.getStrikethroughOffset(), lineMetrics.getStrikethroughThickness(), stringWidth));
        }

        return combinedOutline;
    }

    /**
     * Generates a rectangular area for a text decoration line (underline or strikethrough).
     */
    private static Area decorationLine(float offset, float thickness, int width) {
        return new Area(new Rectangle2D.Float(
            0.0f,
            offset - thickness / 2.0f,
            width,
            thickness));
    }

    public void setBoxAlignment(BoxAlignment newAlignment) {
        Objects.requireNonNull(newAlignment);
        boolean change = this.boxAlignment != newAlignment;
        if (change) {
            this.boxAlignment = newAlignment;
            clearCache();
            invalidatePlacement();
        }
    }

    public void setMLPAlignment(int mlpAlignment) {
        boolean change = this.mlpAlignment != mlpAlignment;
        if (change) {
            this.mlpAlignment = mlpAlignment;
            clearCache();
            invalidateMeasurement();
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
            text = (newText == null) ? "" : newText;
            textLines = text.split("\n");
            clearCache();
            invalidateMeasurement();
        }
    }

    public void setFont(Font newFont) {
        boolean change = !newFont.equals(this.font);
        this.font = newFont;
        if (change) {
            clearCache();
            invalidateMeasurement();
        }
    }

    public void setEffects(AreaEffects newEffects) {
        boolean change = newEffects != effects;
        if (change) {
            this.effects = newEffects;
            clearCache();

            int newPadding;
            if (effects != null && effects.hasEnabledEffects()) {
                newPadding = (int) effects.calcMaxEffectPadding();
            } else {
                newPadding = 0;
            }

            // The effects are painted on top of the measured shape, so the
            // layout is affected only if the padding around the text changes.
            if (newPadding != effectsPadding) {
                effectsPadding = newPadding;
                invalidateMeasurement();
            }
        }
    }

    public boolean isOnPath() {
        return boxAlignment.isPath();
    }

    /**
     * Must be called when the path of the text was edited. The path is
     * also validated automatically (see {@link #needsLayout()}), so this is
     * just an explicit invalidation: it forces the glyphs to be
     * distributed again, even if the new path looks like the old one.
     */
    public void pathChanged() {
        assert isOnPath();
        clearCache();
        onPathLayout = null;
        textShape = null;
        invalidatePlacement();
    }

    /**
     * Embosses the text into the given image. Doesn't change the painter's
     * color or its render cache.
     */
    public BufferedImage createWatermark(BufferedImage src, Composition comp) {
        BufferedImage bumpImage = createBumpMap(src.getWidth(), src.getHeight(), comp);
        return ImageUtils.bumpMap(src, bumpImage, "Watermarking");
    }

    // the bump map image has white text on a black background
    private BufferedImage createBumpMap(int width, int height, Composition comp) {
        BufferedImage bumpImage = new BufferedImage(width, height, TYPE_INT_RGB);
        Graphics2D g = bumpImage.createGraphics();
        try {
            Colors.fillWith(BLACK, g, width, height);

            // The text is drawn with an explicit color, bypassing the render cache,
            // so the painter's color and cached image stay those of normal rendering.
            // The empty-bounds check mirrors the early exit in paint().
            if (prepareForPainting(width, height, comp) && !currentBounds().isEmpty()) {
                paintDirectly(g, WHITE);
            }
        } finally {
            g.dispose();
        }
        return bumpImage;
    }

    public boolean checkInvariants(int expectedTx, int expectedTy) {
        if (translationX != expectedTx || translationY != expectedTy) {
            throw new AssertionError(String.format(
                "translationX = %d, expectedTx = %d, translationY = %d, expectedTy = %d",
                translationX, expectedTx, translationY, expectedTy));
        }
        return true;
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
        node.addNullableDebuggable("renderBounds", renderBounds, DebugNodes::createRectangleNode);
        node.addNullableDebuggable("transformedRect", transformedRect);
        node.addPresence("measuredShape", measurement == null ? null : measurement.shape());
        node.addPresence("placedShape", placedShape);
        node.addPresence("glyphs (on path)", onPathLayout == null ? null : onPathLayout.glyphs());
        node.addDouble("on path dx", onPathDx);
        node.addDouble("on path dy", onPathDy);

        return node;
    }
}

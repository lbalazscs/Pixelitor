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

import pixelitor.gui.utils.MlpAlignmentSelector;
import pixelitor.utils.PathSampler;

import java.awt.Font;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

import static java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_GASP;
import static java.awt.font.TextAttribute.TRACKING;

/**
 * Distributes the glyphs of a text along a path.
 *
 * <p>This is a pure calculation: the result depends only on the
 * {@link OnPathKey} and on the geometry of the {@link PathSampler}.
 * There is no painter state, no {@code Graphics2D} and no
 * {@code Composition} involved, so it can be tested in isolation.
 * {@link TransformedTextPainter} owns the validation and the caching
 * of the results.</p>
 */
final class TextOnPathLayouter {
    /**
     * Everything (except the path itself) that the distribution of the
     * glyphs along a path depends on. The effects padding and the line
     * height are deliberately missing: they don't change the glyphs.
     *
     * @param text         the text, which is laid out as a single line
     * @param font         the font, including its tracking attribute
     * @param mlpAlignment one of the constants of {@link MlpAlignmentSelector}
     * @param rotation     the extra rotation of every glyph, in radians
     * @param scaleX       the horizontal scale of the glyphs, which also
     *                     scales the advances and the tracking
     * @param scaleY       the vertical scale of the glyphs
     * @param shearX       the horizontal shear of the glyphs
     * @param shearY       the vertical shear of the glyphs
     */
    record OnPathKey(String text, Font font, int mlpAlignment,
                     double rotation, double scaleX, double scaleY,
                     double shearX, double shearY) {
        OnPathKey {
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(font, "font");
        }
    }

    /**
     * The result of distributing the glyphs of the text along a path,
     * together with the inputs that it was calculated from.
     *
     * @param key         the text parameters
     * @param path        the flattened path that the glyphs were distributed
     *                    along, which also identifies its geometry
     * @param glyphs      the outlines of the placed glyphs. It is never
     *                    modified after creation.
     * @param glyphBounds the exact bounds of the glyphs
     */
    record OnPathLayout(OnPathKey key, PathSampler path,
                        Shape glyphs, Rectangle2D glyphBounds) {
    }

    /**
     * The layout of text on a path uses this instead of the
     * {@link FontRenderContext} of whatever {@code Graphics2D} is at
     * hand, so that the result doesn't depend on the transform or the
     * hints of the caller. It has the same properties as the one that
     * the measure stage of {@link TransformedTextPainter} gets from its
     * private graphics (no transform, the same text hints), so on a path
     * the glyphs have the same advances as in normal text.
     */
    static final FontRenderContext LAYOUT_FRC = new FontRenderContext(
        null, VALUE_TEXT_ANTIALIAS_GASP, VALUE_FRACTIONALMETRICS_ON);

    private TextOnPathLayouter() {
        // only static methods
    }

    /**
     * Distributes the glyphs of the text of the given key along the
     * given path. The result remembers both of its inputs.
     */
    static OnPathLayout layout(OnPathKey key, PathSampler path) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(path, "path");

        GlyphVector glyphVector = key.font().createGlyphVector(LAYOUT_FRC, key.text());
        Path2D glyphs = distributeGlyphsAlongPath(key, glyphVector, path);
        return new OnPathLayout(key, path, glyphs, glyphs.getBounds2D());
    }

    /**
     * Arranges the individual glyphs of the text along the given path.
     *
     * <p>The start offset can be negative if the text is longer than the
     * path and is centered or right-aligned. The text is not shifted in
     * this case: every glyph keeps its true distance along the path, and
     * the glyphs whose center lies before the start of the path are
     * skipped. This way centered text stays centered and right-aligned
     * text ends exactly at the end of the path. The glyphs whose center
     * lies beyond the end of the path are not drawn either.</p>
     */
    private static Path2D distributeGlyphsAlongPath(OnPathKey key,
                                                    GlyphVector glyphVector,
                                                    PathSampler pathSampler) {
        int numGlyphs = glyphVector.getNumGlyphs();
        double scaleX = key.scaleX();
        double scaleY = key.scaleY();
        double shearX = key.shearX();
        double shearY = key.shearY();
        double absScaleX = Math.abs(scaleX);

        // The unscaled advance of each glyph, calculated only once:
        // both the alignment and the placement of the glyphs need it.
        double[] advances = new double[numGlyphs];
        for (int i = 0; i < numGlyphs; i++) {
            advances[i] = glyphVector.getGlyphMetrics(i).getAdvance();
        }

        double tracking = calcTracking(key.font(), absScaleX);

        // can be negative if the text is longer than the path
        double initialOffset = calcPathStartOffset(key.mlpAlignment(),
            advances, absScaleX, tracking, pathSampler.length());

        Path2D result = new Path2D.Double();

        // the distance along the path where the left edge of the current glyph should be
        double nextGlyphLeftEdgeDist = initialOffset;

        // iterate through each glyph and find its place on the path.
        for (int glyphIndex = 0; glyphIndex < numGlyphs; glyphIndex++) {
            double baseAdvance = advances[glyphIndex];
            double advance = baseAdvance * absScaleX;
            double halfAdvance = advance / 2.0;

            // calculate the target distance for the glyph's center
            double centerTargetDist = nextGlyphLeftEdgeDist + halfAdvance;

            // update the position for the next glyph's left edge.
            // This must happen before skipping, because the skipped
            // glyphs also take up space along the path.
            nextGlyphLeftEdgeDist += advance;
            if (glyphIndex < numGlyphs - 1) {
                nextGlyphLeftEdgeDist += tracking;
            }

            // Calculate the anchor point and angle. There is no sample
            // if the center of the glyph is before the start of the path
            // or beyond its end: such glyphs are not drawn.
            PathSampler.Sample sample = pathSampler.pointAndAngleAt(centerTargetDist);
            if (sample == null) {
                continue;
            }

            // build the transformation for the glyph
            AffineTransform glyphTransform = AffineTransform.getTranslateInstance(sample.x(), sample.y());
            glyphTransform.rotate(sample.angle() + key.rotation());
            if (scaleX != 1.0 || scaleY != 1.0) {
                glyphTransform.scale(scaleX, scaleY);
            }
            if (shearX != 0 || shearY != 0) {
                glyphTransform.shear(-shearX, -shearY);
            }

            // translate the glyph locally so its baseline-center is at the origin
            Point2D origGlyphPos = glyphVector.getGlyphPosition(glyphIndex);
            double baseHalfAdvance = baseAdvance / 2.0;
            glyphTransform.translate(-(origGlyphPos.getX() + baseHalfAdvance), -origGlyphPos.getY());

            Shape glyph = glyphVector.getGlyphOutline(glyphIndex);
            result.append(glyphTransform.createTransformedShape(glyph), false);
        }
        return result;
    }

    /**
     * Calculates the distance along the path where the left edge of the
     * text should be, based on the alignment setting.
     *
     * <p>The result is negative if the text is longer than the path and
     * is centered or right-aligned. Its absolute value is then the length
     * of the text that sticks out before the start of the path. For centered
     * text this is already the overflow on one side (half of the total
     * overflow), so it must not be halved again.</p>
     */
    private static double calcPathStartOffset(int mlpAlignment, double[] advances,
                                              double absScaleX, double tracking,
                                              double pathLength) {
        if (mlpAlignment == MlpAlignmentSelector.LEFT) {
            return 0;
        }

        double textLength = calcTextLength(advances, absScaleX, tracking);

        return switch (mlpAlignment) {
            case MlpAlignmentSelector.CENTER -> (pathLength - textLength) / 2;
            case MlpAlignmentSelector.RIGHT -> pathLength - textLength;
            default -> throw new IllegalStateException("Unexpected value: " + mlpAlignment);
        };
    }

    /**
     * The length of the text along the path: the scaled advances of all
     * glyphs, with the tracking between them (but not after the last one).
     */
    private static double calcTextLength(double[] advances, double absScaleX, double tracking) {
        double totalTextLength = 0;
        int numGlyphs = advances.length;

        for (int i = 0; i < numGlyphs; i++) {
            totalTextLength += advances[i] * absScaleX;
            if (i < numGlyphs - 1) {
                totalTextLength += tracking;
            }
        }
        return totalTextLength;
    }

    // calculates extra spacing between glyphs based on font attributes and scaling
    private static double calcTracking(Font font, double absScaleX) {
        // a Number (and not a Float) so that a font created
        // from an attribute map with a Double doesn't fail
        if (font.getAttributes().get(TRACKING) instanceof Number trackingValue) {
            // the float arithmetic is deliberate: it keeps the old results
            return trackingValue.floatValue() * font.getSize() * absScaleX;
        }
        return 0.0;
    }
}

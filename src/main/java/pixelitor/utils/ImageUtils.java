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

package pixelitor.utils;

import com.jhlabs.composite.OverlayComposite;
import com.jhlabs.image.AbstractBufferedImageOp;
import com.jhlabs.image.EmbossFilter;
import com.jhlabs.image.ImageMath;
import com.twelvemonkeys.image.ImageUtil;
import org.jdesktop.swingx.graphics.BlendComposite;
import org.jdesktop.swingx.painter.CheckerboardPainter;
import pixelitor.Canvas;
import pixelitor.filters.Invert;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.selection.Selection;
import pixelitor.tools.Tools;
import pixelitor.utils.debug.Debug;
import pixelitor.utils.test.Assertions;

import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.util.List;

import static java.awt.AlphaComposite.SRC_OVER;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.image.BufferedImage.*;
import static java.awt.image.DataBuffer.TYPE_INT;
import static java.lang.String.format;

/**
 * Static image-related utility methods.
 */
public class ImageUtils {
    private static final double DEG_315_IN_RADIANS = Math.PI / 4;
    private static final Color CHECKERBOARD_GRAY = new Color(200, 200, 200);

    private static final boolean X_RENDER_WORKAROUND = Toolkit.getDefaultToolkit().getClass().getName()
        .equals("sun.awt.X11.XToolkit");
    private static final double MAX_SAFE_SCALED_SIZE = 16_384;

    private static final GraphicsConfiguration graphicsConfig = GraphicsEnvironment
        .getLocalGraphicsEnvironment()
        .getDefaultScreenDevice()
        .getDefaultConfiguration();
    private static final ColorModel defaultColorModel = graphicsConfig.getColorModel();

    private ImageUtils() {
    }

    public static CheckerboardPainter createCheckerboardPainter() {
        // return a different instance for each painting
        // scenario, because the painters use cached images
        return new CheckerboardPainter(CHECKERBOARD_GRAY, WHITE);
    }

    public static BufferedImage toSysCompatibleImage(BufferedImage input) {
        assert input != null;

        if (input.getColorModel().equals(defaultColorModel)) {
            // RGB images have the right direct color model, but we need transparency
            if (input.getType() != TYPE_INT_RGB) {
                return input;
            }
        }

        BufferedImage output = graphicsConfig.createCompatibleImage(
            input.getWidth(), input.getHeight(), TRANSLUCENT);
        Graphics2D g = output.createGraphics();
        g.drawImage(input, 0, 0, null);
        g.dispose();

        return output;
    }

    public static BufferedImage createSysCompatibleImage(Dimension dim) {
        return createSysCompatibleImage(dim.width, dim.height);
    }

    public static BufferedImage createSysCompatibleImage(Rectangle rect) {
        return createSysCompatibleImage(rect.width, rect.height);
    }

    public static BufferedImage createSysCompatibleImage(Canvas canvas) {
        return createSysCompatibleImage(canvas.getWidth(), canvas.getHeight());
    }

    public static BufferedImage createSysCompatibleImage(int width, int height) {
        assert width > 0 && height > 0;

        return graphicsConfig.createCompatibleImage(width, height, TRANSLUCENT);
    }

    public static VolatileImage createSysCompatibleVolatileImage(Canvas canvas) {
        return createSysCompatibleVolatileImage(canvas.getWidth(), canvas.getHeight());
    }

    public static VolatileImage createSysCompatibleVolatileImage(int width, int height) {
        assert width > 0 && height > 0;

        return graphicsConfig.createCompatibleVolatileImage(width, height, TRANSLUCENT);
    }

    /**
     * Creates a new {@link BufferedImage} with the same
     * {@link ColorModel} as the given source image.
     */
    public static BufferedImage createImageWithSameCM(BufferedImage src) {
        ColorModel cm = src.getColorModel();
        return new BufferedImage(cm, cm.createCompatibleWritableRaster(
            src.getWidth(), src.getHeight()),
            cm.isAlphaPremultiplied(), null);
    }

    /**
     * Creates a new {@link BufferedImage} with the same
     * {@link ColorModel} as the given source image and
     * with the given width and height.
     */
    public static BufferedImage createImageWithSameCM(BufferedImage src,
                                                      int width, int height) {
        ColorModel cm = src.getColorModel();
        return new BufferedImage(cm,
            cm.createCompatibleWritableRaster(width, height),
            cm.isAlphaPremultiplied(), null);
    }

    public static boolean hasPackedIntArray(BufferedImage image) {
        assert image != null;

        int type = image.getType();
        if (type != TYPE_INT_ARGB_PRE && type != TYPE_INT_ARGB && type != TYPE_INT_RGB) {
            return false;
        }

        // For sub-images the underlying DataBufferInt is the entire parent
        // image's buffer with non-zero offsets and scanline strides.
        // Iterating over the raw array would process incorrect pixels.
        // TODO FilterHistoryTest.filterHistory fails if this is not commented out
//        if (isSubImage(image)) {
//            return false;
//        }

        return (long) image.getRaster().getDataBuffer().getSize() == (long) image.getWidth() * image.getHeight();
    }

    /**
     * Returns the pixel array behind the given BufferedImage.
     * If the array data is modified, the image itself is modified.
     */
    public static int[] getPixels(BufferedImage src) {
        assert src != null;

        int[] pixels;
        if (hasPackedIntArray(src)) {
            assert src.getRaster().getTransferType() == TYPE_INT;
            assert src.getRaster().getNumDataElements() == 1;

            DataBufferInt srcDataBuffer = (DataBufferInt) src.getRaster().getDataBuffer();
            pixels = srcDataBuffer.getData();
        } else {
            // if the image's pixels are not stored in an int array,
            // a correct int array could still be retrieved with
            // src.getRGB(0, 0, width, height, null, 0, width);
            // but modifying that array wouldn't have any effect on the image
            throw new UnsupportedOperationException("type is " + Debug.bufferedImageTypeToString(src.getType()));
        }

        return pixels;
    }

    public static byte[] getGrayPixels(BufferedImage img) {
        assert isGrayscale(img);

        WritableRaster raster = img.getRaster();
        DataBufferByte db = (DataBufferByte) raster.getDataBuffer();

        return db.getData();
    }

    public static boolean isGrayscale(BufferedImage img) {
        return img.getType() == TYPE_BYTE_GRAY;
    }

    public static BufferedImage convertToARGB_PRE(BufferedImage src, boolean flushSrc) {
        assert src != null;

        BufferedImage dest = copyTo(TYPE_INT_ARGB_PRE, src);

        if (flushSrc) {
            src.flush();
        }

        return dest;
    }

    public static BufferedImage convertToARGB(BufferedImage src, boolean flushSrc) {
        assert src != null;

        BufferedImage dest = copyTo(TYPE_INT_ARGB, src);

        if (flushSrc) {
            src.flush();
        }

        return dest;
    }

    public static BufferedImage copyTo(int newType, BufferedImage src) {
        BufferedImage dest = new BufferedImage(src.getWidth(), src.getHeight(), newType);
        Graphics2D g = dest.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return dest;
    }

    public static BufferedImage convertToInterleavedRGB(BufferedImage src) {
        return convertToInterleaved(src, false);
    }

    public static BufferedImage convertToInterleavedRGBA(BufferedImage src) {
        return convertToInterleaved(src, true);
    }

    private static BufferedImage convertToInterleaved(BufferedImage src, boolean addAlpha) {
        int numChannels = addAlpha ? 4 : 3;
        WritableRaster wr = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
            src.getWidth(), src.getHeight(), numChannels, null);
        ColorSpace sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        ComponentColorModel ccm = new ComponentColorModel(sRGB, addAlpha,
            false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

        BufferedImage dest = new BufferedImage(ccm, wr, false, null);
        Graphics2D g = dest.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();

        return dest;
    }

    public static BufferedImage convertToRGB(BufferedImage src) {
        return convertToRGB(src, false);
    }

    public static BufferedImage convertToRGB(BufferedImage src, boolean flushSrc) {
        assert src != null;

        BufferedImage dest = copyTo(TYPE_INT_RGB, src);

        if (flushSrc) {
            src.flush();
        }

        return dest;
    }

    public static BufferedImage convertToIndexed(BufferedImage src) {
        return convertToIndexed(src, false);
    }

    public static BufferedImage convertToIndexed(BufferedImage src, boolean flushSrc) {
        assert src != null;

        if (src.getColorModel() instanceof IndexColorModel) {
            return src;
        }

        // is this still necessary?
        if (src.isAlphaPremultiplied()) {
            // otherwise transparent parts will be black when
            // this is drawn on the transparent image
            src = convertToARGB(src, flushSrc);
            flushSrc = true;
        }

        BufferedImage dest = ImageUtil.createIndexed(src, 256,
            BLACK,
            ImageUtil.COLOR_SELECTION_QUALITY + ImageUtil.TRANSPARENCY_BITMASK);

        if (flushSrc) {
            src.flush();
        }

        return dest;
    }

    // There are two cases when this method can't be used to
    // copy an image: (1) for images with an IndexColorModel
    // this returns an image with a shared raster (JDK bug?)
    // (2) for an image created with BufferedImage.getSubimage
    // it throws an exception if the raster doesn't start at (0, 0).
    public static BufferedImage copyImage(BufferedImage src) {
        assert src != null;

        WritableRaster raster = src.copyData(null);
        return new BufferedImage(src.getColorModel(), raster, src.isAlphaPremultiplied(), null);
    }

    // whether the given image was created via BufferedImage.getSubimage()
    public static boolean isSubImage(BufferedImage img) {
        WritableRaster raster = img.getRaster();
        return raster.getParent() != null
            || raster.getSampleModelTranslateX() != 0
            || raster.getSampleModelTranslateY() != 0;
    }

    // Can copy an image that was created by BufferedImage.getSubimage
    public static BufferedImage copySubImage(BufferedImage src) {
        return copyTo(src.getType(), src);
    }

    /**
     * Unlike BufferedImage.getSubimage, this method creates a copy of the data.
     */
    public static BufferedImage copySubImage(BufferedImage src, Rectangle bounds) {
        assert src != null;
        assert bounds != null;

        Rectangle intersection = SwingUtilities.computeIntersection(
            0, 0, src.getWidth(), src.getHeight(), // image bounds
            bounds
        );

        if (intersection.width <= 0 || intersection.height <= 0) {
            throw new IllegalStateException("empty intersection: bounds = " + bounds
                + ", src width = " + src.getWidth()
                + ", src height = " + src.getHeight()
                + ", intersection = " + intersection);
        }

        Raster copyRaster = src.getData(intersection);  // a copy
        Raster startingFrom00 = copyRaster.createChild(
            intersection.x, intersection.y,
            intersection.width, intersection.height,
            0, 0, null);

        return new BufferedImage(src.getColorModel(),
            (WritableRaster) startingFrom00,
            src.isAlphaPremultiplied(), null);
    }

    /**
     * Copies the source image into the destination buffer.
     */
    public static BufferedImage copy(BufferedImage src, BufferedImage dest) {
        Graphics2D g = dest.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return dest;
    }

    public static BufferedImage crop(BufferedImage input, Rectangle bounds) {
        return crop(input, bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public static BufferedImage crop(BufferedImage input, int x, int y, int width, int height) {
        assert input != null;

        if (width <= 0) {
            throw new IllegalArgumentException("width = " + width);
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height = " + height);
        }

        BufferedImage output = createImageWithSameCM(input, width, height);
        Graphics2D g = output.createGraphics();
        g.transform(AffineTransform.getTranslateInstance(-x, -y));
        g.drawImage(input, null, 0, 0);
        g.dispose();

        return output;
    }

    public static int lerpAndPremultiply(float t, int[] color1, int[] color2) {
        int alpha = color1[0] + (int) (t * (color2[0] - color1[0]));
        int red;
        int green;
        int blue;
        if (alpha == 0) {
            red = 0;
            green = 0;
            blue = 0;
        } else {
            red = color1[1] + (int) (t * (color2[1] - color1[1]));
            green = color1[2] + (int) (t * (color2[2] - color1[2]));
            blue = color1[3] + (int) (t * (color2[3] - color1[3]));

            if (alpha != 255) {  // premultiply
                float f = alpha / 255.0f;
                red = (int) (red * f);
                green = (int) (green * f);
                blue = (int) (blue * f);
            }
        }

        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    // See https://graphicdesign.stackexchange.com/questions/89969/what-does-photoshops-high-pass-filter-actually-do-under-the-hood
    public static BufferedImage toHighPassFilteredImage(BufferedImage original, BufferedImage blurred) {
        assert original != null;
        assert blurred != null;

        // the blurred image is the low-pass filtered version of the image,
        // so we subtract it from the original by inverting it...
        BufferedImage retVal = Invert.invertImage(blurred);
        // ... and blending it at 50% with the original
        Graphics2D g = retVal.createGraphics();
        g.setComposite(AlphaComposite.getInstance(SRC_OVER, 0.5f));
        g.drawImage(original, 0, 0, null);
        g.dispose();

        return retVal;
    }

    public static BufferedImage toHighPassSharpenedImage(BufferedImage original, BufferedImage blurred) {
        BufferedImage highPass = toHighPassFilteredImage(original, blurred);

        // blend it with overlay to get a sharpening effect
        Graphics2D g = highPass.createGraphics();
        g.setComposite(new OverlayComposite(1.0f));
        g.drawImage(original, 0, 0, null);
        g.dispose();

        return highPass;
    }

    public static BufferedImage bumpMap(BufferedImage src,
                                        BufferedImage bumpImage,
                                        String filterName) {
        return bumpMap(src, bumpImage,
            (float) DEG_315_IN_RADIANS, 2.0f, filterName, false);
    }

    public static BufferedImage bumpMap(BufferedImage src,
                                        BufferedImage bumpImage,
                                        float azimuth, float bumpHeight,
                                        String filterName, boolean tile) {
        var emboss = new EmbossFilter(filterName, azimuth,
            (float) (Math.PI / 6.0), bumpHeight, false, EmbossFilter.LightingMode.MATTE);

        BufferedImage bumpMap = emboss.filter(bumpImage, null);

        BufferedImage dest = copyImage(src);

        Graphics2D g = dest.createGraphics();
        g.setComposite(BlendComposite.HardLight);
        if (tile) {
            // TODO if 3 is not subtracted here, then for some reason
            //   there are 3-pixel-wide gaps between the tiles
            int bumpMapWidth = Math.max(bumpMap.getWidth() - 3, 1);
            int bumpMapHeight = Math.max(bumpMap.getHeight() - 3, 1);

            int numHorTiles = dest.getWidth() / bumpMapWidth + 1;
            int numVerTiles = dest.getHeight() / bumpMapHeight + 1;
            for (int i = 0; i < numHorTiles; i++) {
                for (int j = 0; j < numVerTiles; j++) {
                    int x = i * bumpMapWidth;
                    int y = j * bumpMapHeight;
                    g.drawImage(bumpMap, x, y, null);
                }
            }
        } else {
            g.drawImage(bumpMap, 0, 0, null);
        }
        g.dispose();

        return dest;
    }

    public static BufferedImage convertToGrayscaleImage(BufferedImage src) {
        return copyTo(TYPE_BYTE_GRAY, src);
    }

    public static BufferedImage extractSelectedRegion(BufferedImage src,
                                                      Selection selection,
                                                      int tx, int ty) {
        assert selection != null;

        Rectangle bounds = selection.getShapeBounds(); // relative to the canvas

        bounds.translate(-tx, -ty); // now relative to the image

        // the intersection of the selection with the image
        bounds = SwingUtilities.computeIntersection(
            0, 0, src.getWidth(), src.getHeight(), // image bounds
            bounds);

        if (bounds.isEmpty()) { // the selection is outside the image
            // this should not happen, because the selection should be
            // always within the canvas
            throw new IllegalStateException(format("tx = %d, ty = %d, bounds = %s",
                tx, ty, selection.getShapeBounds()));
        }

        return copySubImage(src, bounds);
    }

    /**
     * Replaces the selected region of the source image with a new image.
     * Returns the modified image with the selected region replaced.
     */
    public static BufferedImage replaceSelectedRegion(BufferedImage src,
                                                      BufferedImage replacement,
                                                      boolean isUndoRedo,
                                                      ImageLayer layer) {
        assert src != null;
        assert replacement != null;
        assert Assertions.rasterStartsAtOrigin(replacement);

        Selection selection = layer.getComp().getSelection();
        if (selection == null) {
            return replacement;
        }

        int tx = layer.getTx();
        int ty = layer.getTy();
        Rectangle selBounds = selection.getShapeBounds();

        if (isUndoRedo) {
            // ignore the precise selection shape to avoid AA complications
            Graphics2D g = src.createGraphics();
            g.drawImage(replacement, selBounds.x - tx, selBounds.y - ty, null);
            g.dispose();
            return src;
        } else if (selection.isRectangular()) {
            // rectangular selection, simple selection shape clipping
            // is enough, because there are no aliasing problems
            Graphics2D g = src.createGraphics();

            // it's important to translate the whole graphics, and not
            // just the draw start, because the clip must also be translated
            g.translate(-tx, -ty);

            // transparency comes from the new image
            g.setComposite(AlphaComposite.Src);
            Shape shape = selection.getShape();
            g.setClip(shape);
            g.drawImage(replacement, selBounds.x, selBounds.y, null);
            g.dispose();
            return src;
        } else {  // non-rectangular selection: do soft clipping
            // 1. create the mask for the selection
            BufferedImage maskImg = createSysCompatibleImage(selBounds);
            Graphics2D maskG = createSoftSelectionMask(maskImg, selection.getShape(), selBounds.x, selBounds.y);
            maskG.dispose();

            // 2. prepare a temporary buffer the exact size of the selection to avoid Java2D coordinate bugs
            BufferedImage blended = createSysCompatibleImage(selBounds);
            Graphics2D blendG = blended.createGraphics();

            // 3. extract the background from the source image by drawing it with a negative offset
            blendG.drawImage(src, -(selBounds.x - tx), -(selBounds.y - ty), null);

            // 4. blend the replacement using the custom composite cleanly at 0,0
            blendG.setComposite(new MaskedReplaceComposite(maskImg, 0, 0));
            blendG.drawImage(replacement, 0, 0, null);
            blendG.dispose();

            // 5. overwrite the original image with the fully composited tile
            Graphics2D srcG = src.createGraphics();
            srcG.setComposite(AlphaComposite.Src); // force replace mode
            srcG.drawImage(blended, selBounds.x - tx, selBounds.y - ty, null);
            srcG.dispose();

            return src;
        }
    }

    /**
     * Prepares a temporary Graphics2D for soft (anti-aliased) selection
     * clipping. It follows ideas from
     * http://web.archive.org/web/20120603053853/http://weblogs.java.net/blog/campbell/archive/2006/07/java_2d_tricker.html
     */
    public static Graphics2D createSoftSelectionMask(Image image, Shape selShape,
                                                     int selStartX, int selStartY) {
        Graphics2D maskG = (Graphics2D) image.getGraphics();

        // fill the entire image with transparent pixels
        maskG.setComposite(AlphaComposite.Clear);
        maskG.fillRect(0, 0, image.getWidth(null), image.getHeight(null));

        // fill the transparent image with anti-aliased
        // selection-shaped white mask
        maskG.setComposite(AlphaComposite.Src);
        maskG.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        maskG.setColor(WHITE);
        maskG.translate(-selStartX, -selStartY); // because the selection shape is relative to the canvas
        maskG.fill(selShape);

        // Prepare the Graphics2D for subsequent rendering.
        // It is important to use SrcIn, and not SrcAtop like in the
        // blog mentioned above, because the new content might also
        // contain transparent pixels, and we don't want to lose that information.
        maskG.setComposite(AlphaComposite.SrcIn);
        maskG.translate(selStartX, selStartY); // undo the previous translation

        return maskG;
    }

    public static BufferedImage copyToBufferedImage(Image src) {
        BufferedImage copy;
        if (src instanceof BufferedImage bufferedImage) {
            if (bufferedImage.getColorModel() instanceof IndexColorModel) {
                copy = convertToARGB(bufferedImage, false);
            } else {
                copy = copyImage(bufferedImage);
            }
        } else if (src instanceof VolatileImage volatileImage) {
            copy = volatileImage.getSnapshot();
        } else {
            throw new UnsupportedOperationException("src class is " + src.getClass().getName());
        }
        return copy;
    }

    public static void unpremultiply(BufferedImage dest) {
        int[] pixels = getPixels(dest);
        ImageMath.unpremultiply(pixels);
    }

    public static void premultiply(BufferedImage src) {
        int[] pixels = getPixels(src);
        ImageMath.premultiply(pixels);
    }

    public static BufferedImage filterPremultiplied(BufferedImage src,
                                                    BufferedImage dest,
                                                    AbstractBufferedImageOp filter) {
        BufferedImage filterSrc = src;

        boolean premultiply = !src.isAlphaPremultiplied() && hasPackedIntArray(src);
        if (premultiply) {
            filterSrc = copyImage(src);
            premultiply(filterSrc);
        }

        dest = filter.filter(filterSrc, dest);

        if (premultiply) {
            unpremultiply(dest);
        }
        return dest;
    }

    /**
     * Calculates the composite image from the given layers.
     */
    public static BufferedImage calcComposite(List<Layer> layers, Canvas canvas) {
        if (layers.size() == 1) { // optimization for single-layer compositions
            Layer layer = layers.getFirst();
            if (Tools.activeTool.isDirectDrawing() && layer.isVisible()) {
                BufferedImage layerImg = layer.toImage(true, true);

                // it can be null if there's a single adjustment layer
                if (layerImg != null) {
                    return layerImg;
                }
            }
        }

        var compositeImg = new BufferedImage(
            canvas.getWidth(), canvas.getHeight(), TYPE_INT_ARGB_PRE);
        Graphics2D g = compositeImg.createGraphics();

        // the first visible layer is always applied with normal blending mode
        boolean firstVisibleLayer = true;
        for (Layer layer : layers) {
            if (!layer.isVisible()) {
                continue;
            }
            BufferedImage result = layer.render(g, compositeImg, firstVisibleLayer);
            if (result != null) { // adjustment layer or watermarking text layer
                compositeImg = result;
                g.dispose();
                g = compositeImg.createGraphics();
            }
            firstVisibleLayer = false;
        }

        g.dispose();
        return compositeImg;
    }

    /**
     * Returns the minimum enclosing rectangle around the non-transparent region in the given image.
     * Returns null if the image is entirely transparent.
     */
    public static Rectangle calcOpaqueBounds(BufferedImage image) {
        WritableRaster alphaRaster = image.getAlphaRaster();
        if (alphaRaster == null) {
            // no alpha channel implies the whole image is opaque
            return new Rectangle(0, 0, image.getWidth(), image.getHeight());
        }

        int width = alphaRaster.getWidth();
        int height = alphaRaster.getHeight();

        // initial bounds
        int left = 0;
        int top = 0;
        int right = width - 1;
        int bottom = height - 1;

        // optimization helper variables
        int minRight = width - 1;
        int minBottom = height - 1;

        // iterates through the rows from the top and stops
        // when it finds the first non-transparent pixel
        topLabel:
        for (; top <= bottom; top++) { // use <= so we don't miss the final row
            for (int x = 0; x < width; x++) {
                if (alphaRaster.getSample(x, top, 0) != 0) {
                    minRight = x;
                    minBottom = top;
                    break topLabel;
                }
            }
        }

        if (top > bottom) { // we found no opaque pixels
            return null;
        }

        // iterates through the columns from the left
        leftLabel:
        for (; left < minRight; left++) {
            for (int y = height - 1; y > top; y--) {
                if (alphaRaster.getSample(left, y, 0) != 0) {
                    minBottom = y;
                    break leftLabel;
                }
            }
        }

        // iterates through the rows from the bottom
        bottomLabel:
        for (; bottom > minBottom; bottom--) {
            for (int x = width - 1; x >= left; x--) {
                if (alphaRaster.getSample(x, bottom, 0) != 0) {
                    minRight = x;
                    break bottomLabel;
                }
            }
        }

        // iterates through the columns from the right
        rightLabel:
        for (; right > minRight; right--) {
            for (int y = bottom; y >= top; y--) {
                if (alphaRaster.getSample(right, y, 0) != 0) {
                    break rightLabel;
                }
            }
        }

        return new Rectangle(left, top, right - left + 1, bottom - top + 1);
    }

    /**
     * Returns true if the coordinates (x, y) are within the image.
     */
    public static boolean isWithinBounds(int x, int y, BufferedImage img) {
        return x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight();
    }

    // maximizes the contrast in the given image
    public static void normalizeImage(BufferedImage img) {
        // stretching the color channels without unpremultiplying them first
        // would break the premultiplication invariant (color channels must be <= alpha)
        assert !img.isAlphaPremultiplied();

        int[] pixels = getPixels(img);
        int max = 0;

        boolean hasAlpha = img.getTransparency() != Transparency.OPAQUE;

        // find the maximum channel value
        for (int rgb : pixels) {
            // ignore "hidden" colors in fully transparent pixels
            if (hasAlpha && (rgb >>> 24) == 0) {
                continue;
            }

            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;

            int localMax = Math.max(r, Math.max(g, b));
            if (localMax > max) {
                max = localMax;
            }
        }

        // normalize if the image isn't already stretched and isn't purely black
        if (max > 0 && max < 255) {
            // precompute a scaling look-up table
            int[] lut = new int[256];
            double sf = 255.0 / max;
            for (int i = 0; i < 256; i++) {
                lut[i] = Math.min(255, (int) Math.round(i * sf));
            }

            // apply the LUT to all pixels
            for (int i = 0; i < pixels.length; i++) {
                int rgb = pixels[i];

                // preserve the original alpha channel bits
                int a = rgb & 0xFF_00_00_00;

                int r = lut[(rgb >> 16) & 0xFF];
                int g = lut[(rgb >> 8) & 0xFF];
                int b = lut[rgb & 0xFF];

                pixels[i] = a | (r << 16) | (g << 8) | b;
            }
        }
    }

    /**
     * Like g.drawImage(img, x, y, null), but if the image would be scaled to a
     * huge size on the device, only the part inside the clip is drawn.
     */
    public static void drawImageVisiblePart(Graphics2D g, BufferedImage img, int x, int y) {
        if (!X_RENDER_WORKAROUND) {
            g.drawImage(img, x, y, null);
            return;
        }

        AffineTransform at = g.getTransform();
        double devW = img.getWidth() * Math.abs(at.getScaleX());
        double devH = img.getHeight() * Math.abs(at.getScaleY());
        Rectangle clip = g.getClipBounds();

        if (clip == null || (devW <= MAX_SAFE_SCALED_SIZE && devH <= MAX_SAFE_SCALED_SIZE)) {
            g.drawImage(img, x, y, null);
            return;
        }

        int dx1 = Math.max(x, clip.x);
        int dy1 = Math.max(y, clip.y);
        int dx2 = Math.min(x + img.getWidth(), clip.x + clip.width);
        int dy2 = Math.min(y + img.getHeight(), clip.y + clip.height);
        if (dx1 >= dx2 || dy1 >= dy2) {
            return; // nothing visible
        }

        // the sub-image shares the raster (no pixel copy) but is a different
        // BufferedImage, so XRender sees source coordinates starting at 0
        BufferedImage visible = img.getSubimage(dx1 - x, dy1 - y, dx2 - dx1, dy2 - dy1);
        g.drawImage(visible, dx1, dy1, null);
        visible.flush(); // drop any cached pixmap for this throwaway image
    }
}

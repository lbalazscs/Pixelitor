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

package pixelitor.utils;

import com.jhlabs.composite.OverlayComposite;
import com.jhlabs.composite.ScreenComposite;
import com.jhlabs.image.AbstractBufferedImageOp;
import com.jhlabs.image.BoxBlurFilter;
import com.jhlabs.image.EmbossFilter;
import com.jhlabs.image.ImageMath;
import com.twelvemonkeys.image.ImageUtil;
import org.jdesktop.swingx.graphics.BlendComposite;
import org.jdesktop.swingx.painter.CheckerboardPainter;
import pixelitor.Canvas;
import pixelitor.colors.Colors;
import pixelitor.filters.Invert;
import pixelitor.gui.utils.Dialogs;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.selection.Selection;
import pixelitor.tools.Tools;
import pixelitor.utils.debug.Debug;
import pixelitor.utils.test.Assertions;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.*;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.jhlabs.image.ImageMath.HALF_SQRT_3;
import static com.jhlabs.image.ImageMath.SQRT_3;
import static java.awt.AlphaComposite.SRC_OVER;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
import static java.awt.image.BufferedImage.TRANSLUCENT;
import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.awt.image.DataBuffer.TYPE_INT;
import static java.lang.String.format;
import static pixelitor.Views.thumbSize;
import static pixelitor.utils.Threads.onPool;

/**
 * Static image-related utility methods
 */
public class ImageUtils {
    private static final double DEG_315_IN_RADIANS = Math.PI / 4;
    private static final Color CHECKERBOARD_GRAY = new Color(200, 200, 200);

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

    public static CompletableFuture<BufferedImage> resizeAsync(BufferedImage img,
                                                               int targetWidth,
                                                               int targetHeight) {
        return CompletableFuture.supplyAsync(() ->
            resize(img, targetWidth, targetHeight), onPool);
    }

    public static BufferedImage resize(BufferedImage img, int targetWidth, int targetHeight) {
        boolean progressiveBilinear = targetWidth < img.getWidth() / 2
            || targetHeight < img.getHeight() / 2;
        return scaleImage(img, targetWidth, targetHeight, VALUE_INTERPOLATION_BICUBIC, progressiveBilinear);
    }

    // From the Filthy Rich Clients book

    /**
     * Convenience method that returns a scaled instance of the
     * provided BufferedImage.
     *
     * @param img                 the original image to be scaled
     * @param targetWidth         the desired width of the scaled instance,
     *                            in pixels
     * @param targetHeight        the desired height of the scaled instance,
     *                            in pixels
     * @param hint                one of the rendering hints that corresponds to
     *                            RenderingHints.KEY_INTERPOLATION (e.g.
     *                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR,
     *                            RenderingHints.VALUE_INTERPOLATION_BILINEAR,
     *                            RenderingHints.VALUE_INTERPOLATION_BICUBIC)
     * @param progressiveBilinear if true, this method will use a multi-step
     *                            scaling technique that provides higher quality than the usual
     *                            one-step technique (only useful in down-scaling cases, where
     *                            targetWidth or targetHeight is
     *                            smaller than the original dimensions)
     * @return a scaled version of the original BufferedImage
     */
    private static BufferedImage scaleImage(BufferedImage img,
                                            int targetWidth, int targetHeight,
                                            Object hint,
                                            boolean progressiveBilinear) {
        assert img != null;

        int prevW = img.getWidth();
        int prevH = img.getHeight();

        if (targetWidth >= prevW || targetHeight >= prevH) {
            progressiveBilinear = false;
        }

        int type = img.getType();

        BufferedImage ret = img;
        int w, h;
        boolean isTranslucent = img.getTransparency() != Transparency.OPAQUE;

        if (progressiveBilinear) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        BufferedImage scratchImage = null;
        Graphics2D g2 = null;
        do {
            if (progressiveBilinear && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (progressiveBilinear && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            if (scratchImage == null || isTranslucent) {
                // Use a single scratch buffer for all iterations
                // and then copy to the final, correctly-sized image
                // before returning
                scratchImage = new BufferedImage(w, h, type);
                g2 = scratchImage.createGraphics();
            }
            g2.setRenderingHint(KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, 0, 0, prevW, prevH, null);
            prevW = w;
            prevH = h;

            ret = scratchImage;
        } while (w != targetWidth || h != targetHeight);

        if (g2 != null) {
            g2.dispose();
        }

        // If we used a scratch buffer that is larger than our target size,
        // create an image of the right size and copy the results into it
        if (targetWidth != ret.getWidth() || targetHeight != ret.getHeight()) {
            scratchImage = new BufferedImage(targetWidth, targetHeight, type);
            g2 = scratchImage.createGraphics();
            g2.drawImage(ret, 0, 0, null);
            g2.dispose();
            ret = scratchImage;
        }

        return ret;
    }

    /**
     * Also an iterative approach, but using even smaller steps
     */
    public static BufferedImage enlargeSmoothly(BufferedImage src,
                                                int targetWidth, int targetHeight,
                                                Object hint, double step, ProgressTracker pt) {
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        double factorX = targetWidth / (double) srcWidth;
        double factorY = targetHeight / (double) srcHeight;

        // they should be the same, but rounding errors can cause small problems
        assert Math.abs(factorX - factorY) < 0.05;

        double factor = (factorX + factorY) / 2.0;
        assert factor > 1.0; // this only makes sense for enlarging
        double progress = 1.0;
        double lastStep = factor / step;
        BufferedImage last = src;
        AffineTransform stepScale = AffineTransform.getScaleInstance(step, step);
        while (progress < lastStep) {
            progress = progress * step;
            int newSrcWidth = (int) (srcWidth * progress);
            int newSrcHeight = (int) (srcHeight * progress);
            BufferedImage tmp = new BufferedImage(newSrcWidth, newSrcHeight, src.getType());
            Graphics2D g = tmp.createGraphics();
            if (hint != null) {
                g.setRenderingHint(KEY_INTERPOLATION, hint);
            }

            g.drawImage(last, stepScale, null);
            g.dispose();

            BufferedImage willBeForgotten = last;
            last = tmp;
            willBeForgotten.flush();
            pt.unitDone();
        }

        // do the last step: resize exactly to the target values
        BufferedImage retVal = new BufferedImage(targetWidth, targetHeight, src.getType());
        Graphics2D g = retVal.createGraphics();
        if (hint != null) {
            g.setRenderingHint(KEY_INTERPOLATION, hint);
        }

        g.drawImage(last, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        pt.unitDone();

        return retVal;
    }

    /**
     * Returns the number of steps (progress tracking work units)
     * required for smooth enlargement.
     */
    public static int calcEnlargeSmoothlySteps(double resizeFactor, double step) {
        double progress = 1.0;
        double lastStep = resizeFactor / step;
        int retVal = 1; // for the final step
        while (progress < lastStep) {
            progress = progress * step;
            retVal++;
        }
        return retVal;
    }

    public static boolean hasPackedIntArray(BufferedImage image) {
        assert image != null;

        int type = image.getType();
        return (type == TYPE_INT_ARGB_PRE || type == TYPE_INT_ARGB || type == TYPE_INT_RGB);
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
            // If the image's pixels are not stored in an int array,
            // a correct int array could still be retrieved with
            // src.getRGB(0, 0, width, height, null, 0, width);
            // but modifying that array wouldn't have any effect on the image.
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

    public static BufferedImage createGrayscaleImage(byte[] pixels, int width, int height) {
        assert pixels.length == width * height;

        DataBuffer data = new DataBufferByte(pixels, 1);
        WritableRaster raster = Raster.createInterleavedRaster(data,
            width, height, width, 1, new int[]{0}, new Point(0, 0));

        ColorSpace cs = new ICC_ColorSpace(ICC_Profile.getInstance(ColorSpace.CS_GRAY));
        ColorModel cm = new ComponentColorModel(cs,
            false, false,
            Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

        return new BufferedImage(cm, raster, false, null);
    }

    /**
     * Converts an image filename to a resource URL within the images directory.
     */
    public static URL findImageURL(String fileName) {
        assert fileName != null;

        String path = "/images/" + fileName;
        URL imgURL = ImageUtils.class.getResource(path);
        if (imgURL == null) {
            Messages.showError("Error", path + " not found");
        }

        return imgURL;
    }

    public static BufferedImage loadResourceImage(String fileName) {
        assert fileName != null;

        URL imgURL = findImageURL(fileName);
        BufferedImage image = null;
        try {
            image = ImageIO.read(imgURL);
        } catch (IOException e) {
            Messages.showException(e);
        }
        return image;
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

    private static BufferedImage makeIndexedTransparent(BufferedImage image, int x, int y) {
        ColorModel cm = image.getColorModel();
        assert cm instanceof IndexColorModel;

        IndexColorModel icm = (IndexColorModel) cm;
        WritableRaster raster = image.getRaster();

        int size = icm.getMapSize();
        byte[] reds = new byte[size];
        byte[] greens = new byte[size];
        byte[] blues = new byte[size];
        icm.getReds(reds);
        icm.getGreens(greens);
        icm.getBlues(blues);

        int pixel = raster.getSample(x, y, 0);
        var icm2 = new IndexColorModel(8, size, reds, greens, blues, pixel);

        return new BufferedImage(icm2, raster, image.isAlphaPremultiplied(), null);
    }

    /**
     * Shrinks the "src" image to match its width to "size" and returns the new image.
     */
    public static BufferedImage createThumbnail(BufferedImage src, int size, CheckerboardPainter painter) {
        assert src != null;

        Dimension thumbDim = calcThumbDimensions(src.getWidth(), src.getHeight(), size, true);

        return downSizeFast(src, thumbDim.width, thumbDim.height, painter);
    }

    /**
     * Calculates the target dimensions if an image needs to be resized
     * to fit into a box of a given size without distorting the aspect ratio.
     */
    public static Dimension calcThumbDimensions(int srcWidth, int srcHeight, int boxSize, boolean upscale) {
        int thumbWidth;
        int thumbHeight;
        if (srcWidth > srcHeight) { // landscape
            if (upscale || srcWidth > boxSize) {
                thumbWidth = boxSize;
                double ratio = (double) srcWidth / srcHeight;
                thumbHeight = (int) (boxSize / ratio);
            } else {
                // the image already fits in the box and no up-scaling is needed
                thumbWidth = srcWidth;
                thumbHeight = srcHeight;
            }
        } else { // portrait
            if (upscale || srcHeight > boxSize) {
                thumbHeight = boxSize;
                double ratio = (double) srcHeight / srcWidth;
                thumbWidth = (int) (boxSize / ratio);
            } else {
                // the image already fits in the box and no up-scaling is needed
                thumbWidth = srcWidth;
                thumbHeight = srcHeight;
            }
        }

        if (thumbWidth == 0) {
            thumbWidth = 1;
        }
        if (thumbHeight == 0) {
            thumbHeight = 1;
        }

        return new Dimension(thumbWidth, thumbHeight);
    }

    public static BufferedImage createThumbnail(BufferedImage src,
                                                int maxWidth, int maxHeight,
                                                CheckerboardPainter painter) {
        assert src != null;

        int imgWidth = src.getWidth();
        int imgHeight = src.getHeight();

        double xScaling = maxWidth / (double) imgWidth;
        double yScaling = maxHeight / (double) imgHeight;
        double scaling = Math.min(xScaling, yScaling);
        int thumbWidth = (int) (imgWidth * scaling);
        int thumbHeight = (int) (imgHeight * scaling);

        return downSizeFast(src, thumbWidth, thumbHeight, painter);
    }

    private static BufferedImage downSizeFast(BufferedImage src,
                                              int thumbWidth, int thumbHeight,
                                              CheckerboardPainter painter) {
        BufferedImage thumb = createSysCompatibleImage(thumbWidth, thumbHeight);
        Graphics2D g = thumb.createGraphics();

        if (painter != null) {
            painter.paint(g, null, thumbWidth, thumbHeight);
        }

        g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(src, 0, 0, thumbWidth, thumbHeight, null);
        g.dispose();

        return thumb;
    }

    public static void paintRedXOn(BufferedImage thumb) {
        int thumbWidth = thumb.getWidth();
        int thumbHeight = thumb.getHeight();

        Graphics2D g = thumb.createGraphics();

        g.setColor(new Color(200, 0, 0));
        g.setStroke(new BasicStroke(2.5f));
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.drawLine(0, 0, thumbWidth, thumbHeight);
        g.drawLine(thumbWidth - 1, 0, 0, thumbHeight - 1);
        g.dispose();
    }

    public static void main(String[] args) {
        BufferedImage img = createSysCompatibleImage(100, 100);
        copyImage(img);
        BufferedImage sub = img.getSubimage(20, 20, 50, 50);
        copyImage(sub); // throws exception
    }

    // There are two cases when this method can't be used to
    // copy an image: (1) for images with an IndexColorModel
    // this returns an image with a shared raster (jdk bug?)
    // (2) for an image created with BufferedImage.getSubimage
    // it throws an exception if the raster doesn't start at (0, 0).
    public static BufferedImage copyImage(BufferedImage src) {
        assert src != null;

        BufferedImage copy = null;
        try {
            WritableRaster raster = src.copyData(null);
            copy = new BufferedImage(src.getColorModel(), raster, src.isAlphaPremultiplied(), null);
        } catch (OutOfMemoryError e) {
            Dialogs.showOutOfMemoryDialog(e);
        }
        return copy;
    }

    public static Boolean isSubImage(BufferedImage src) {
        WritableRaster raster = src.getRaster();
        return raster.getSampleModelTranslateX() != 0
            || raster.getSampleModelTranslateY() != 0;
    }

    // Can copy an image that was created by BufferedImage.getSubimage
    public static BufferedImage copySubImage(BufferedImage src) {
        return copyTo(src.getType(), src);
    }

    /**
     * Unlike BufferedImage.getSubimage, this method creates a copy of the data
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
     * A hack so that Fade can work with PartialImageEdit rasters.
     * It would be better if Fade could work with rasters directly.
     */
    public static BufferedImage rasterToImage(Raster raster) {
        assert raster != null;

        int minX = raster.getMinX();
        int minY = raster.getMinY();
        int width = raster.getWidth();
        int height = raster.getHeight();
        Raster startingFrom00 = raster.createChild(minX, minY, width, height, 0, 0, null);
        BufferedImage image = new BufferedImage(width, height, TYPE_INT_ARGB_PRE);
        image.setData(startingFrom00);

        return image;
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

    public static void screenWithItself(BufferedImage src, float opacity) {
        assert src != null;

        Graphics2D g = src.createGraphics();
        g.setComposite(new ScreenComposite(opacity));
        g.drawImage(src, 0, 0, null);
        g.dispose();
    }

    // See https://graphicdesign.stackexchange.com/questions/89969/what-does-photoshops-high-pass-filter-actually-do-under-the-hood
    public static BufferedImage toHighPassFilteredImage(BufferedImage original, BufferedImage blurred) {
        assert original != null;
        assert blurred != null;

        // The blurred image is the low-pass filtered version of the image,
        // so we subtract it from the original by inverting it...
        blurred = Invert.invertImage(blurred);
        // ... and blending it at 50% with the original
        Graphics2D g = blurred.createGraphics();
        g.setComposite(AlphaComposite.getInstance(SRC_OVER, 0.5f));
        g.drawImage(original, 0, 0, null);
        g.dispose();

        return blurred;
    }

    public static BufferedImage toHighPassSharpenedImage(BufferedImage original, BufferedImage blurred) {
        BufferedImage highPass = toHighPassFilteredImage(original, blurred);

        // blend it with overlay to get a sharpening effect
        Graphics2D g2 = highPass.createGraphics();
        g2.setComposite(new OverlayComposite(1.0f));
        g2.drawImage(original, 0, 0, null);
        g2.dispose();

        return highPass;
    }

    public static BufferedImage createRandomPointsTemplateBrush(int diameter, float density) {
        if (density < 0.0 && density > 1.0) {
            throw new IllegalArgumentException("density is " + density);
        }

        BufferedImage brushImage = new BufferedImage(diameter, diameter, TYPE_INT_ARGB);

        int radius = diameter / 2;
        int radius2 = radius * radius;
        Random random = new Random();

        int[] pixels = getPixels(brushImage);
        for (int x = 0; x < diameter; x++) {
            for (int y = 0; y < diameter; y++) {
                int dx = x - radius;
                int dy = y - radius;
                int centerDistance2 = dx * dx + dy * dy;
                if (centerDistance2 < radius2) {
                    float rn = random.nextFloat();
                    if (density > rn) {
                        pixels[x + y * diameter] = random.nextInt();
                    } else {
                        pixels[x + y * diameter] = 0xFF_FF_FF_FF;  // white
                    }
                } else {
                    pixels[x + y * diameter] = 0xFF_FF_FF_FF; // white
                }
            }
        }

        return brushImage;
    }

    public static BufferedImage createSoftBWBrush(int size) {
        BufferedImage brushImage = new BufferedImage(size, size, TYPE_INT_ARGB);
        Graphics2D g = brushImage.createGraphics();

        // fill a black circle over the white background
        Colors.fillWith(WHITE, g, size, size);
        g.setColor(BLACK);
        int softness = size / 4;
        g.fillOval(softness, softness, size - 2 * softness, size - 2 * softness);
        g.dispose();

        // blur it
        var blur = new BoxBlurFilter(softness, softness, 1, "Blur");
        blur.setProgressTracker(ProgressTracker.NULL_TRACKER);
        brushImage = blur.filter(brushImage, brushImage);

        return brushImage;
    }

    public static BufferedImage createSoftTransparencyImage(int size) {
        BufferedImage image = createSoftBWBrush(size);
        int[] pixels = getPixels(image);
        for (int i = 0, pixelsLength = pixels.length; i < pixelsLength; i++) {
            int pixelValue = pixels[i] & 0xFF; // take the blue channel: they are all the same
            int alpha = 255 - pixelValue;
            pixels[i] = alpha << 24;
        }
        return image;
    }

    public static void renderGrid(Graphics2D g,
                                  int lineWidth, int spacing,
                                  int width, int height) {
        assert lineWidth > 0;
        assert spacing > 0;

        // horizontal lines
        int halfLineThickness = lineWidth / 2;
        for (int y = 0; y < height; y += spacing) {
            int startY = y - halfLineThickness;
            //noinspection SuspiciousNameCombination
            g.fillRect(0, startY, width, lineWidth);
        }

        // vertical lines
        for (int x = 0; x < width; x += spacing) {
            g.fillRect(x - halfLineThickness, 0, lineWidth, height);
        }
    }

    public static void renderBrickGrid(Graphics2D g, int brickHeight,
                                       int width, int height) {
        if (brickHeight < 1) {
            throw new IllegalArgumentException("brickHeight = " + brickHeight);
        }

        int brickWidth = brickHeight * 2;
        int currentY = 0;
        int rowCount = 0;

        while (currentY <= height) {
            // horizontal lines
            g.drawLine(0, currentY, width, currentY);

            // only draw vertical lines if we're not at the last line
            if (currentY < height) {
                // vertical lines
                int horOffset = ((rowCount % 2) == 1) ? brickHeight : 0;
                for (int x = horOffset; x < width; x += brickWidth) {
                    g.drawLine(x, currentY, x, Math.min(currentY + brickHeight, height));
                }
            }

            currentY += brickHeight;
            rowCount++;
        }
    }

    public static void renderTriangleGrid(Graphics2D g, int size,
                                          int width, int height) {
        double halfSize = size / 2.0;
        double triangleHeight = size * HALF_SQRT_3;
        double tan30 = halfSize / triangleHeight;
        double cotan30 = triangleHeight / halfSize;

        // horizontal lines
        double currentY = triangleHeight;
        while (currentY < height) {
            Line2D line = new Line2D.Double(0, currentY, width, currentY);
            g.draw(line);
            currentY += triangleHeight;
        }

        // slanted lines downwards to the right
        // starting from the top edge (startX = 0)
        for (double startY = 0; startY <= height; startY += 2 * triangleHeight) {
            double startX = 0;

            double endX = width;
            double endY = startY + width * cotan30;

            // ensure the end point stays within bounds
            if (endY > height) {
                endX = (height - startY) * tan30;
                endY = height;
            }

            g.draw(new Line2D.Double(startX, startY, endX, endY));
        }

        // slanted lines downwards to the right
        // starting from the left edge (startY = 0)
        for (double x = 0; x <= width; x += size) {
            double startX = x;
            double startY = 0;

            double endX = x + height * tan30;
            double endY = height;

            // ensure the end point stays within bounds
            if (endX > width) {
                endY = height - (endX - width) * cotan30;
                endX = width;
            }

            g.draw(new Line2D.Double(startX, startY, endX, endY));
        }

        // slanted lines upwards to the right
        // starting from the top edge (startY = 0)
        for (double x = size; x <= width; x += size) {
            double startX = x;
            double startY = 0;

            double endX = x - height * tan30;
            double endY = height;

            // ensure the end point stays within bounds
            if (endX < 0) {
                endY = height + endX * cotan30;
                endX = 0;
            }

            g.draw(new Line2D.Double(startX, startY, endX, endY));
        }

        // slanted lines upwards to the right
        // starting from the right edge (startX = width)
        int numHorTriangles = width / size;
        int xOffset = (numHorTriangles + 1) * size - width;
        double yOffset = xOffset * cotan30;
        for (double startY = yOffset; startY <= height; startY += 2 * triangleHeight) {
            double startX = width;

            double endX = width - (height - startY) * tan30;
            double endY = height;

            // ensure the end point stays within bounds
            if (endX < 0) {
                endY = startY + width * cotan30;
                endX = 0;
            }

            g.draw(new Line2D.Double(startX, startY, endX, endY));
        }
    }

    // Converts pixel (x, y) to axial (q, r) assuming a flat top hexagon
    private static Point2D.Double pixelToAxial(double x, double y, double s) {
        double q = (2.0 / 3.0 * x) / s;
        double r = (-1.0 / 3.0 * x + SQRT_3 / 3.0 * y) / s;
        return new Point2D.Double(q, r);
    }

    /**
     * Renders a grid of hexagons matching the HexagonBlockFilter structure.
     */
    public static void renderHexagonGrid(Graphics2D g, int size,
                                         int width, int height) {
        // Axial coordinates that uniquely identify a hexagon.
        record AxialCoord(int q, int r) implements Comparable<AxialCoord> {
            @Override
            public int compareTo(AxialCoord other) {
                if (this.q != other.q) {
                    return Integer.compare(this.q, other.q);
                }
                return Integer.compare(this.r, other.r);
            }
        }

        // An edge is uniquely defined by the two hexagons it separates.
        record EdgeKey(AxialCoord c1, AxialCoord c2) {
            EdgeKey(AxialCoord c1, AxialCoord c2) {
                // ensure that the edge key is independent of the order of the hexagons
                if (c1.compareTo(c2) < 0) {
                    this.c1 = c1;
                    this.c2 = c2;
                } else {
                    this.c1 = c2;
                    this.c2 = c1;
                }
            }
        }

        double s = size;
        if (s <= 0) {
            return;
        }

        // vertical distance from center to horizontal sides
        double hexHeight = s * HALF_SQRT_3;

        // horizontal distance from center to vertical sides
        double hexWidth = s * 1.5;

        // determine iteration range based on filter's coordinate system
        double W = width;
        double H = height;
        // a buffer to catch hexagons overlapping the edges
        double buffer = s * 1.1;

        // check axial coordinates for the buffered bounding box corners
        Point2D.Double tl = pixelToAxial(-buffer, -buffer, s);
        Point2D.Double tr = pixelToAxial(W + buffer, -buffer, s);
        Point2D.Double bl = pixelToAxial(-buffer, H + buffer, s);
        Point2D.Double br = pixelToAxial(W + buffer, H + buffer, s);

        // the integer iteration range based on the min/max axial coordinates
        int qMin = (int) Math.floor(Math.min(tl.x, bl.x));
        int qMax = (int) Math.ceil(Math.max(tr.x, br.x));
        int rMin = (int) Math.floor(Math.min(tl.y, tr.y));
        int rMax = (int) Math.ceil(Math.max(bl.y, br.y));

        // the set of edges to avoid drawing the same edge twice
        Set<EdgeKey> drawnEdges = new HashSet<>();

        for (int q = qMin; q <= qMax; q++) {
            for (int r = rMin; r <= rMax; r++) {
                // center (cx, cy) for axial coordinates (q, r) in a flat top hexagon
                double cx = hexWidth * q;
                double cy = hexHeight * q + 2.0 * hexHeight * r;

                // calculate the bounding box of this hexagon
                double hexMinX = cx - s;
                double hexMaxX = cx + s;
                double hexMinY = cy - hexHeight;
                double hexMaxY = cy + hexHeight;

                // skip hexagon if its bounding box is entirely outside the canvas
                if (hexMaxX <= 0 || hexMinX >= W || hexMaxY <= 0 || hexMinY >= H) {
                    continue;
                }

                // current hexagon's axial coordinate
                AxialCoord currentCoord = new AxialCoord(q, r);

                // the vertices for the flat-top hexagon centered at (cx, cy)
                Point2D.Double[] vertices = {
                    new Point2D.Double(cx + s, cy),                   // right
                    new Point2D.Double(cx + s / 2.0, cy + hexHeight), // top-right
                    new Point2D.Double(cx - s / 2.0, cy + hexHeight), // top-left
                    new Point2D.Double(cx - s, cy),                   // left
                    new Point2D.Double(cx - s / 2.0, cy - hexHeight), // bottom-left
                    new Point2D.Double(cx + s / 2.0, cy - hexHeight), // bottom-right
                };

                // Define neighbors based on edge index for flat-top grid
                // Matches edge indices to neighbor axial coordinate offsets
                AxialCoord[] neighbors = {
                    new AxialCoord(q + 1, r),     // neighbor for edge 0 (vertices 0-1)
                    new AxialCoord(q, r + 1),     // neighbor for edge 1 (vertices 1-2)
                    new AxialCoord(q - 1, r + 1), // neighbor for edge 2 (vertices 2-3)
                    new AxialCoord(q - 1, r),     // neighbor for edge 3 (vertices 3-4)
                    new AxialCoord(q, r - 1),     // neighbor for edge 4 (vertices 4-5)
                    new AxialCoord(q + 1, r - 1)  // neighbor for edge 5 (vertices 5-0)
                };

                // draw the edges of the hexagon if they haven't been drawn
                for (int i = 0; i < 6; i++) {
                    Point2D p1 = vertices[i];
                    Point2D p2 = vertices[(i + 1) % 6];

                    // create the edge key using the current and neighbor axial coordinates
                    EdgeKey edgeKey = new EdgeKey(currentCoord, neighbors[i]);

                    // draw it if the key was not already present
                    if (drawnEdges.add(edgeKey)) {
                        g.draw(new Line2D.Double(p1, p2));
                    }
                }
            }
        }
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
        var emboss = new EmbossFilter(filterName);
        emboss.setAzimuth(azimuth);
        emboss.setElevation((float) (Math.PI / 6.0));
        emboss.setBumpHeight(bumpHeight);

        BufferedImage bumpMap = emboss.filter(bumpImage, null);

        BufferedImage dest = copyImage(src);

        Graphics2D g = dest.createGraphics();
        g.setComposite(BlendComposite.HardLight);
        if (tile) {
            // If 3 is not subtracted here, then for some reason
            // there are 3 pixel wide gaps between the tiles.
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

    public static int premultiply(int rgb) {
        int a = (rgb >>> 24) & 0xFF;
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8) & 0xFF;
        int b = rgb & 0xFF;

        float f = a * (1.0f / 255.0f);
        r = (int) (r * f);
        g = (int) (g * f);
        b = (int) (b * f);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int unPremultiply(int rgb) {
        int a = (rgb >>> 24) & 0xFF;
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8) & 0xFF;
        int b = rgb & 0xFF;

        if (a == 0 || a == 255) {
            return rgb;
        }

        float f = 255.0f / a;
        r = (int) (r * f);
        g = (int) (g * f);
        b = (int) (b * f);
        if (r > 255) {
            r = 255;
        }
        if (g > 255) {
            g = 255;
        }
        if (b > 255) {
            b = 255;
        }

        return (a << 24) | (r << 16) | (g << 8) | b;
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
            BufferedImage tmpImg = createSysCompatibleImage(selBounds.width, selBounds.height);
            Graphics2D tmpG = createSoftSelectionMask(tmpImg, selection.getShape(), selBounds.x, selBounds.y);

            tmpG.drawImage(replacement, 0, 0, null);
            tmpG.dispose();

            Graphics2D srcG = src.createGraphics();
            srcG.drawImage(tmpImg, selBounds.x - tx, selBounds.y - ty, null);
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

    public static BufferedImage applyTransform(BufferedImage src, AffineTransform at, int targetWidth, int targetHeight) {
        assert targetWidth > 0 && targetHeight > 0 : "target = " + targetWidth + "x" + targetHeight;
        BufferedImage newImage = new BufferedImage(targetWidth, targetHeight, TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();
        g.setTransform(at);
        if (targetWidth > src.getWidth() || targetHeight > src.getHeight()) {
            g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
        } else {
            g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
        }
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return newImage;
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

    public static BufferedImage createCircleThumb(Color color) {
        BufferedImage img = createSysCompatibleImage(thumbSize, thumbSize);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.fillOval(0, 0, thumbSize, thumbSize);
        g2.dispose();
        return img;
    }

    /**
     * Returns the minimum enclosing rectangle around the non-transparent region in the given image.
     */
    public static Rectangle calcOpaqueBounds(BufferedImage image) {
        WritableRaster alphaRaster = image.getAlphaRaster();
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
        for (; top < bottom; top++) {
            for (int x = 0; x < width; x++) {
                if (alphaRaster.getSample(x, top, 0) != 0) {
                    minRight = x;
                    minBottom = top;
                    break topLabel;
                }
            }
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
     * Blends two source images based on a mask image. The blend ratio for
     * each pixel is determined by the corresponding pixel in the mask image.
     */
    public static BufferedImage blendWithMask(BufferedImage srcA, BufferedImage srcB, BufferedImage mask) {
        BufferedImage dest = createImageWithSameCM(srcA);
        int[] srcAPixels = getPixels(srcA);
        int[] srcBPixels = getPixels(srcB);
        int[] maskPixels = getPixels(mask);
        int[] destPixels = getPixels(dest);

        for (int i = 0, numPixels = destPixels.length; i < numPixels; i++) {
            // take the blue channel, assuming that all channels are the same
            float transparency = (maskPixels[i] & 0xFF) / 255.0f;
            destPixels[i] = ImageMath.mixColors(transparency, srcAPixels[i], srcBPixels[i]);
        }

        return dest;
    }

    /**
     * Returns true if the coordinates (x, y) are within the image.
     */
    public static boolean isWithinBounds(int x, int y, BufferedImage img) {
        return x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight();
    }
}
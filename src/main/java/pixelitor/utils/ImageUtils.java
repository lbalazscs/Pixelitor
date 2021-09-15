/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import com.jhlabs.image.BoxBlurFilter;
import com.jhlabs.image.EmbossFilter;
import com.twelvemonkeys.image.ImageUtil;
import org.jdesktop.swingx.graphics.BlendComposite;
import org.jdesktop.swingx.painter.CheckerboardPainter;
import pixelitor.Canvas;
import pixelitor.colors.Colors;
import pixelitor.filters.Invert;
import pixelitor.gui.utils.Dialogs;
import pixelitor.selection.Selection;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.IOException;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static java.awt.AlphaComposite.SRC_OVER;
import static java.awt.BasicStroke.CAP_ROUND;
import static java.awt.BasicStroke.JOIN_ROUND;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.*;
import static java.awt.Transparency.TRANSLUCENT;
import static java.awt.image.BufferedImage.*;
import static java.awt.image.DataBuffer.TYPE_INT;
import static java.lang.String.format;
import static pixelitor.colors.Colors.packedIntToString;
import static pixelitor.colors.Colors.toPackedARGB;
import static pixelitor.utils.Threads.onPool;

/**
 * Static image-related utility methods
 */
public class ImageUtils {
    public static final double DEG_315_IN_RADIANS = Math.PI / 4;
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

    public static BufferedImage createImageWithSameCM(BufferedImage src) {
        ColorModel cm = src.getColorModel();
        return new BufferedImage(cm, cm.createCompatibleWritableRaster(
            src.getWidth(), src.getHeight()),
            cm.isAlphaPremultiplied(), null);
    }

    // like the above but instead of src width and height, it uses the arguments
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
        boolean progressiveBilinear = targetWidth < img.getWidth() / 2
                                      || targetHeight < img.getHeight() / 2;

        return CompletableFuture.supplyAsync(() ->
            getFasterScaledInstance(img, targetWidth, targetHeight,
                VALUE_INTERPOLATION_BICUBIC, progressiveBilinear), onPool);
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
    public static BufferedImage getFasterScaledInstance(BufferedImage img,
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
    public static BufferedImage enlargeSmooth(BufferedImage src,
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
     * Returns the number of steps necessary for progress tracking
     */
    public static int calcNumStepsForEnlargeSmooth(double resizeFactor, double step) {
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
        return (type == TYPE_INT_ARGB_PRE || type == TYPE_INT_RGB || type == TYPE_INT_ARGB);
    }

    /**
     * This methods returns the pixel array behind the given BufferedImage
     * If the array data is modified, the image itself is modified
     */
    public static int[] getPixelsAsArray(BufferedImage src) {
        assert src != null;

        int[] pixels;
        if (hasPackedIntArray(src)) {
            assert src.getRaster().getTransferType() == TYPE_INT;
            assert src.getRaster().getNumDataElements() == 1;

            DataBufferInt srcDataBuffer = (DataBufferInt) src.getRaster().getDataBuffer();
            pixels = srcDataBuffer.getData();
        } else {
            // if the image's pixels are not stored in an int array,
            // a correct int array can still be retrieved with
            // src.getRGB(0, 0, width, height, null, 0, width);
            // but modifying that array won't have any effect on the image.
            throw new UnsupportedOperationException();
        }

        return pixels;
    }

    public static byte[] getGrayPixelsAsByteArray(BufferedImage img) {
        assert img.getType() == TYPE_BYTE_GRAY;

        WritableRaster raster = img.getRaster();
        DataBufferByte db = (DataBufferByte) raster.getDataBuffer();

        return db.getData();
    }

    public static BufferedImage getGrayImageFromByteArray(byte[] pixels, int width, int height) {
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

    public static URL imagePathToURL(String fileName) {
        assert fileName != null;

        String iconPath = "/images/" + fileName;
        URL imgURL = ImageUtils.class.getResource(iconPath);
        if (imgURL == null) {
            Messages.showError("Error", iconPath + " not found");
        }

        return imgURL;
    }

    public static BufferedImage loadJarImageFromImagesFolder(String fileName) {
        // consider caching
        // for image brushes this is not necessary because
        // the template brush always has the max size

        assert fileName != null;

        URL imgURL = imagePathToURL(fileName);
        BufferedImage image = null;
        try {
            image = ImageIO.read(imgURL);
        } catch (IOException e) {
            Messages.showException(e);
        }
        return image;
    }

    public static BufferedImage convertToARGB_PRE(BufferedImage src, boolean flushOld) {
        assert src != null;

        BufferedImage dest = drawOn(TYPE_INT_ARGB_PRE, src);

        if (flushOld) {
            src.flush();
        }

        return dest;
    }

    public static BufferedImage convertToARGB(BufferedImage src, boolean flushOld) {
        assert src != null;

        BufferedImage dest = drawOn(TYPE_INT_ARGB, src);

        if (flushOld) {
            src.flush();
        }

        return dest;
    }

    private static BufferedImage drawOn(int newType, BufferedImage src) {
        var dest = new BufferedImage(src.getWidth(), src.getHeight(), newType);
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

    public static BufferedImage convertToRGB(BufferedImage src, boolean flushOld) {
        assert src != null;

        BufferedImage dest = drawOn(TYPE_INT_RGB, src);

        if (flushOld) {
            src.flush();
        }

        return dest;
    }

    public static BufferedImage convertToIndexed(BufferedImage src) {
        return convertToIndexed(src, false);
    }

    public static BufferedImage convertToIndexed(BufferedImage src, boolean flushOld) {
        assert src != null;

        if (src.getColorModel() instanceof IndexColorModel) {
            return src;
        }

        // is this still necessary?
        if (src.isAlphaPremultiplied()) {
            // otherwise transparent parts will be black when
            // this is drawn on the transparent image
            src = convertToARGB(src, flushOld);
            flushOld = true;
        }

        BufferedImage dest = ImageUtil.createIndexed(src, 256,
            BLACK,
            ImageUtil.COLOR_SELECTION_QUALITY + ImageUtil.TRANSPARENCY_BITMASK);

// the old solution was based on http://gman.eichberger.de/2007/07/transparent-gifs-in-java.html
//        var dest = new BufferedImage(
//            src.getWidth(), src.getHeight(), TYPE_BYTE_INDEXED);
//
//        Graphics2D g = dest.createGraphics();
//        // this hideous color will be transparent
//        Colors.fillWith(new Color(231, 20, 189), g, src.getWidth(), src.getHeight());
//        g.dispose();
//
//        dest = makeIndexedTransparent(dest, 0, 0);
//
//        g = dest.createGraphics();
//        g.drawImage(src, 0, 0, null);
//        g.dispose();

        if (flushOld) {
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
     * Shrinks the "src" image to match it's width to "size" and returns the new image.
     */
    public static BufferedImage createThumbnail(BufferedImage src, int size, CheckerboardPainter painter) {
        assert src != null;

        Dimension thumbDim = calcThumbDimensions(src.getWidth(), src.getHeight(), size);

        return downSizeFast(src, thumbDim.width, thumbDim.height, painter);
    }

    /**
     * Returns a Dimension object representing the given srcWidth
     * and srcHeight scaled down to match srcWidth to size.
     */
    public static Dimension calcThumbDimensions(int srcWidth, int srcHeight, int size) {
        int thumbWidth;
        int thumbHeight;
        if (srcWidth > srcHeight) {
            thumbWidth = size;
            float ratio = (float) srcWidth / srcHeight;
            thumbHeight = (int) (size / ratio);
        } else {
            thumbHeight = size;
            float ratio = (float) srcHeight / srcWidth;
            thumbWidth = (int) (size / ratio);
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

    // Can copy an image which was created by BufferedImage.getSubimage
    public static BufferedImage copySubImage(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        Graphics2D g2 = copy.createGraphics();
        g2.drawImage(src, 0, 0, null);
        g2.dispose();
        return copy;
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
    public static BufferedImage getHighPassFilteredImage(BufferedImage original, BufferedImage blurred) {
        assert original != null;
        assert blurred != null;

        // The blurred image is the low-pass filtered version of the image
        // so we subtract it form the original by inverting it...
        blurred = Invert.invertImage(blurred);
        // ... and blending it at 50% with the original
        Graphics2D g = blurred.createGraphics();
        g.setComposite(AlphaComposite.getInstance(SRC_OVER, 0.5f));
        g.drawImage(original, 0, 0, null);
        g.dispose();

        return blurred;
    }

    public static BufferedImage getHighPassSharpenedImage(BufferedImage original, BufferedImage blurred) {
        BufferedImage highPass = getHighPassFilteredImage(original, blurred);

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

        int[] pixels = getPixelsAsArray(brushImage);
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
                        pixels[x + y * diameter] = 0xFFFFFFFF;  // white
                    }
                } else {
                    pixels[x + y * diameter] = 0xFFFFFFFF; // white
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
        int[] pixels = getPixelsAsArray(image);
        for (int i = 0, pixelsLength = pixels.length; i < pixelsLength; i++) {
            int pixelValue = pixels[i] & 0xFF; // take the blue channel: they are all the same
            int alpha = 255 - pixelValue;
            pixels[i] = alpha << 24;
        }
        return image;
    }

    public static void drawGrid(Color color, Graphics2D g,
                                int maxX, int maxY,
                                int hWidth, int hSpacing,
                                int vWidth, int vSpacing,
                                boolean emptyIntersections) {
        if (hWidth < 0) {
            throw new IllegalArgumentException("hWidth = " + hWidth);
        }
        if (vWidth < 0) {
            throw new IllegalArgumentException("vWidth = " + vWidth);
        }
        if (hSpacing <= 0) {
            throw new IllegalArgumentException("hSpacing = " + hSpacing);
        }
        if (vSpacing <= 0) {
            throw new IllegalArgumentException("vSpacing = " + vSpacing);
        }

        g.setColor(color);

        Composite savedComposite = g.getComposite();
        if (emptyIntersections) {
            g.setComposite(AlphaComposite.Xor);
        }

        int halfHWidth = hWidth / 2;
        int halfVWidth = vWidth / 2;

        // horizontal lines
        if (hWidth > 0) {
            for (int y = 0; y < maxY; y += vSpacing) {
                int startY = y - halfVWidth;
                //noinspection SuspiciousNameCombination
                g.fillRect(0, startY, maxX, vWidth);
            }
        }

        // vertical lines
        if (vWidth > 0) {
            for (int x = 0; x < maxX; x += hSpacing) {
                g.fillRect(x - halfHWidth, 0, hWidth, maxY);
            }
        }

        if (emptyIntersections) {
            g.setComposite(savedComposite);
        }
    }

    public static void drawBrickGrid(Color color, Graphics2D g, int size,
                                     int maxX, int maxY) {
        if (size < 1) {
            throw new IllegalArgumentException("size = " + size);
        }

        g.setColor(color);

        int doubleSize = size * 2;
        int y = size;
        int verticalCount = 0;
        while (y < maxY) {
            // vertical lines
            int hShift = 0;
            if ((verticalCount % 2) == 1) {
                hShift = size;
            }
            for (int x = hShift; x < maxX; x += doubleSize) {
                g.drawLine(x, y, x, y - size);
            }

            // horizontal lines
            g.drawLine(0, y, maxX, y);
            y += size;
            verticalCount++;
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
        var embossFilter = new EmbossFilter(filterName);
        embossFilter.setAzimuth(azimuth);
        embossFilter.setElevation((float) (Math.PI / 6.0));
        embossFilter.setBumpHeight(bumpHeight);

        BufferedImage bumpMap = embossFilter.filter(bumpImage, null);

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

    public static BufferedImage convertToGrayScaleImage(BufferedImage src) {
        BufferedImage dest = new BufferedImage(
            src.getWidth(), src.getHeight(), TYPE_BYTE_GRAY);
        Graphics2D g2 = dest.createGraphics();
        g2.drawImage(src, 0, 0, null);
        g2.dispose();

        return dest;
    }

    public static void paintAffectedAreaShapes(BufferedImage image, Shape[] shapes) {
        Graphics2D g = image.createGraphics();
        g.setColor(BLACK);

//        ZoomLevel zoomLevel = OpenComps.getActiveView().getZoomLevel();

//        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

//        g.setStroke(zoomLevel.getOuterStroke());

        for (Shape shape : shapes) {
            g.draw(shape);
        }
        g.setColor(WHITE);
//        g.setStroke(zoomLevel.getInnerStroke());

        for (Shape shape : shapes) {
            g.draw(shape);
        }

        g.dispose();
    }

    public static void fillWithTransparentRectangle(Graphics2D g, int size) {
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, size, size);
        g.setComposite(AlphaComposite.SrcOver);
    }

    public static boolean compareSmallImages(BufferedImage img1, BufferedImage img2) {
        assert img1.getWidth() == img2.getWidth();
        assert img1.getHeight() == img2.getHeight();

        int width = img1.getWidth();
        int height = img1.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb1 = img1.getRGB(x, y);
                int rgb2 = img2.getRGB(x, y);
                if (rgb1 != rgb2) {
                    String msg = format("at (%d, %d) rgb1 is %s and rgb2 is %s",
                        x, y, packedIntToString(rgb1), packedIntToString(rgb2));
                    System.out.println("ImageUtils::compareSmallImages: " + msg);
                    return false;
                }
            }
        }

        return true;
    }

    public static String debugSmallImage(BufferedImage im) {
        int width = im.getWidth();
        int height = im.getHeight();
        StringBuilder s = new StringBuilder(100);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = im.getRGB(x, y);
                String asString = packedIntToString(rgb);
                s.append(asString);
                if (x == width - 1) {
                    s.append("\n");
                } else {
                    s.append(" ");
                }
            }
        }
        return s.toString();
    }

    public static BufferedImage create1x1Image(Color c) {
        return create1x1Image(c.getAlpha(), c.getRed(), c.getGreen(), c.getBlue());
    }

    public static BufferedImage create1x1Image(int a, int r, int g, int b) {
        BufferedImage img = createSysCompatibleImage(1, 1);
        img.setRGB(0, 0, toPackedARGB(a, r, g, b));
        return img;
    }

    public static void paintBlurredGlow(Shape shape, Graphics2D g, int numSteps, float effectWidth) {
        float brushAlpha = 1.0f / numSteps;
        g.setComposite(AlphaComposite.getInstance(SRC_OVER, brushAlpha));
//        g.setComposite(new AddComposite(brushAlpha));
        for (float i = 0; i < numSteps; i = i + 1.0f) {
            float brushWidth = i * effectWidth / numSteps;
            g.setStroke(new BasicStroke(brushWidth, CAP_ROUND, JOIN_ROUND));
            g.draw(shape);
        }
    }

    public static BufferedImage getSelectionSizedPartFrom(BufferedImage src,
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
     * Sets up the given image to be temporary image needed for soft
     * (anti-aliased) selection clipping, following ideas from
     * https://community.oracle.com/blogs/campbell/2006/07/19/java-2d-trickery-soft-clipping
     */
    public static Graphics2D setupForSoftSelection(Image image, Shape selShape,
                                                   int selStartX, int selStartY) {
        Graphics2D tmpG = (Graphics2D) image.getGraphics();

        // fill with transparent pixels
        tmpG.setComposite(AlphaComposite.Clear);
        tmpG.fillRect(0, 0, image.getWidth(null), image.getHeight(null));

        // fill the transparent image with anti-aliased
        // selection-shaped white
        tmpG.setComposite(AlphaComposite.Src);
        tmpG.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        tmpG.setColor(WHITE);
        tmpG.translate(-selStartX, -selStartY); // because the selection shape is relative to the canvas
        tmpG.fill(selShape);

        // further drawing operations should not
        // change the transparency of the image.
        // It is important to use SrcIn, and not SrcAtop like in the
        // blog mentioned above, because the new content might also
        // contain transparent pixels and we don't want to lose that information.
        tmpG.setComposite(AlphaComposite.SrcIn);
        tmpG.translate(selStartX, selStartY);

        return tmpG;
    }

    public static BufferedImage copyToBufferedImage(Image img) {
        BufferedImage copy;
        if (img instanceof BufferedImage bufferedImage) {
            if (bufferedImage.getColorModel() instanceof IndexColorModel) {
                copy = convertToARGB(bufferedImage, false);
            } else {
                copy = copyImage(bufferedImage);
            }
        } else if (img instanceof VolatileImage volatileImage) {
            copy = volatileImage.getSnapshot();
        } else {
            throw new UnsupportedOperationException("img class is " + img.getClass().getName());
        }
        return copy;
    }
}
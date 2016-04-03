/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import org.jdesktop.swingx.graphics.BlendComposite;
import org.jdesktop.swingx.painter.CheckerboardPainter;
import pixelitor.colors.ColorUtils;
import pixelitor.filters.Invert;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.utils.Dialogs;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.utils.debug.BufferedImageNode;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.PixelGrabber;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.Random;

import static java.awt.AlphaComposite.SRC_OVER;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

/**
 * Image utility methods
 */
public class ImageUtils {
    public static final double DEG_315_IN_RADIANS = 0.7853981634;
    public static final float[] FRACTIONS_2_COLOR_UNIFORM = {0.0f, 1.0f};
    private static final Color CHECKERBOARD_GRAY = new Color(200, 200, 200);

    /**
     * Utility class with static methods
     */
    private ImageUtils() {
    }

    public static CheckerboardPainter createCheckerboardPainter() {
          return new CheckerboardPainter(CHECKERBOARD_GRAY, Color.WHITE);
    }

    public static BufferedImage toSysCompatibleImage(BufferedImage input) {
        assert input != null;

        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();

        if (input.getColorModel().equals(gc.getColorModel())) {
            // already compatible
            return input;
        }

        int transparency = Transparency.TRANSLUCENT;
        BufferedImage output = gc.createCompatibleImage(input.getWidth(), input.getHeight(), transparency);
        Graphics2D g = output.createGraphics();
        g.drawImage(input, 0, 0, null);
        g.dispose();

        return output;
    }

    public static BufferedImage createSysCompatibleImage(int width, int height) {
        assert (width > 0) && (height > 0);

        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        BufferedImage output = gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
        return output;
    }

    public static BufferedImage createImageWithSameColorModel(BufferedImage src) {
        ColorModel dstCM = src.getColorModel();
        return new BufferedImage(dstCM, dstCM.createCompatibleWritableRaster(src.getWidth(), src.getHeight()), dstCM.isAlphaPremultiplied(), null);
    }

    // like the above but instead of src width and height, it uses the arguments
    public static BufferedImage createImageWithSameColorModel(BufferedImage src, int width, int height) {
        ColorModel dstCM = src.getColorModel();
        return new BufferedImage(dstCM, dstCM.createCompatibleWritableRaster(width, height), dstCM.isAlphaPremultiplied(), null);
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
                                                        int targetWidth, int targetHeight, Object hint,
                                                        boolean progressiveBilinear) {
        assert img != null;

        int prevW = img.getWidth();
        int prevH = img.getHeight();

        if (targetWidth >= prevW || targetHeight >= prevH) {
            progressiveBilinear = false;
        }

//        // TODO in these two cases the original method from the Filthy Rich Clients book goes into infinite loop!
//        if ((prevH <= targetHeight) && (prevW >= targetWidth)) {
//            return simpleResize(img, targetWidth, targetHeight, hint);
//        }
//        if ((prevH > targetHeight) && (prevW < targetWidth)) {
//            return simpleResize(img, targetWidth, targetHeight, hint);
//        }


//        int type = (img.getTransparency() == Transparency.OPAQUE) ?
//                BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        int type = img.getType();

        BufferedImage ret = img;
        BufferedImage scratchImage = null;
        Graphics2D g2 = null;
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

        do {
            if (progressiveBilinear && (w > targetWidth)) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (progressiveBilinear && (h > targetHeight)) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            if ((scratchImage == null) || isTranslucent) {
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
        } while ((w != targetWidth) || (h != targetHeight));

        if (g2 != null) {
            g2.dispose();
        }

        // If we used a scratch buffer that is larger than our target size,
        // create an image of the right size and copy the results into it
        if ((targetWidth != ret.getWidth()) || (targetHeight != ret.getHeight())) {
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
    public static BufferedImage enlargeSmooth(BufferedImage src, int targetWidth, int targetHeight, Object hint, double step, ProgressTracker pt) {
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
     * Returns the number of steps necessary for
     * For progress tracking
     */
    public static int getNumStepsForEnlargeSmooth(double resizeFactor, double step) {
        double progress = 1.0;
        double lastStep = resizeFactor / step;
        int retVal = 1; // for the final step
        while (progress < lastStep) {
            progress = progress * step;
            retVal++;
        }
        return retVal;
    }

    private static BufferedImage simpleResize(BufferedImage img, int targetWidth, int targetHeight, Object hint) {
        assert img != null;

        BufferedImage ret = new BufferedImage(targetWidth, targetHeight, img.getType());
        Graphics2D g2 = ret.createGraphics();
        g2.setRenderingHint(KEY_INTERPOLATION, hint);
        g2.drawImage(img, 0, 0, targetWidth, targetHeight, null);
        g2.dispose();
        return ret;
    }

    // TODO duplicate functionality
    public static BufferedImage resizeImage(double newSize, BufferedImage original) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        int maxOriginalSize = Math.max(originalWidth, originalHeight);
        double ratio = ((double) maxOriginalSize) / newSize;
        int imageWidth = (int) (originalWidth / ratio);
        int imageHeight = (int) (originalHeight / ratio);
        BufferedImage resizedImage = new BufferedImage(imageWidth, imageHeight, TYPE_INT_ARGB_PRE);
        Graphics2D g2 = resizedImage.createGraphics();
        g2.drawImage(original, 0, 0, imageWidth, imageHeight, null);
        g2.dispose();
        return resizedImage;
    }

    /**
     * Samples 9 pixels at and around the given pixel coordinates
     *
     * @param src
     * @param x
     * @param y
     * @return the average color
     */
    public static Color sample9Points(BufferedImage src, int x, int y) {
        int averageRed = 0;
        int averageGreen = 0;
        int averageBlue = 0;
        int width = src.getWidth();
        int height = src.getHeight();

        for (int i = x - 1; i < x + 2; i++) {
            for (int j = y - 1; j < y + 2; j++) {
                int limitedX = limitSamplingIndex(i, width - 1);
                int limitedY = limitSamplingIndex(j, height - 1);

                int rgb = src.getRGB(limitedX, limitedY);
//                int a = (rgb >>> 24) & 0xFF;
                int r = (rgb >>> 16) & 0xFF;
                int g = (rgb >>> 8) & 0xFF;
                int b = (rgb) & 0xFF;
                averageRed += r;
                averageGreen += g;
                averageBlue += b;
            }
        }

        return new Color(averageRed / 9, averageGreen / 9, averageBlue / 9);
    }

    private static int limitSamplingIndex(int x, int max) {
        int r = x;
        if (r < 0) {
            r = 0;
        }
        if (r > max) {
            r = max;
        }
        return r;
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

        boolean fastWay = hasPackedIntArray(src);
        if (fastWay) {
            DataBufferInt srcDataBuffer = (DataBufferInt) src.getRaster().getDataBuffer();
            pixels = srcDataBuffer.getData();
        } else if (src.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            // TODO this does not seem to work - why?
            int width = src.getWidth();
            int height = src.getHeight();
            pixels = src.getRGB(0, 0, width, height, null, 0, width);
        } else {
            int width = src.getWidth();
            int height = src.getHeight();
            pixels = new int[width * height];
            PixelGrabber pg = new PixelGrabber(src, 0, 0, width, height, pixels, 0, width);

            try {
                pg.grabPixels();
            } catch (InterruptedException e) {
                Messages.showException(e);
            }
        }
        return pixels;
    }

    public static byte[] getPixelsAsByteArray(BufferedImage src) {
        assert src.getType() == BufferedImage.TYPE_BYTE_GRAY;

        WritableRaster raster = src.getRaster();
        DataBufferByte db = (DataBufferByte) raster.getDataBuffer();
        return db.getData();
    }

    public static URL resourcePathToURL(String fileName) {
        assert fileName != null;

        String iconPath = "/images/" + fileName;
        URL imgURL = ImageUtils.class.getResource(iconPath);
        if (imgURL == null) {
            String message = iconPath + " not found";
            Messages.showError("Error", message);
        }
        return imgURL;
    }

    /**
     * Loads an image from the images folder
     */
    public static BufferedImage loadBufferedImage(String fileName) {
        // consider caching
        // for image brushes this is not necessary because the template brush always has the max size

        assert fileName != null;

        URL imgURL = resourcePathToURL(fileName);
        BufferedImage image = null;
        try {
            image = ImageIO.read(imgURL);
        } catch (IOException e) {
            Messages.showException(e);
        }
        return image;
    }

    public static BufferedImage convertToARGB_PRE(BufferedImage src, boolean oldCanBeFlushed) {
        assert src != null;

        BufferedImage dest = new BufferedImage(src.getWidth(), src.getHeight(), TYPE_INT_ARGB_PRE);
        Graphics2D g = dest.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();

        if (oldCanBeFlushed) {
            src.flush();
        }

        return dest;
    }

    public static BufferedImage convertToARGB(BufferedImage src, boolean oldCanBeFlushed) {
        assert src != null;

        BufferedImage dest = new BufferedImage(src.getWidth(), src.getHeight(), TYPE_INT_ARGB);
        Graphics2D g = dest.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();

        if (oldCanBeFlushed) {
            src.flush();
        }

        return dest;
    }

    public static BufferedImage convertToRGB(BufferedImage src, boolean oldCanBeFlushed) {
        assert src != null;

        BufferedImage dest = new BufferedImage(src.getWidth(), src.getHeight(), TYPE_INT_RGB);
        Graphics2D g = dest.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();

        if (oldCanBeFlushed) {
            src.flush();
        }

        return dest;
    }

    // without this the drawing on large images would be very slow
    // TODO is this faster? the simple g.drawImage also respects the clipping

    public static void drawImageWithClipping(Graphics g, BufferedImage img) {
        assert img != null;

        Rectangle clipBounds = g.getClipBounds();
        int clipX = (int) clipBounds.getX();
        int clipY = (int) clipBounds.getY();
        int clipWidth = (int) clipBounds.getWidth();
        int clipHeight = (int) clipBounds.getHeight();
        int clipX2 = clipX + clipWidth;
        int clipY2 = clipY + clipHeight;
        g.drawImage(img, clipX, clipY, clipX2, clipY2, clipX, clipY, clipX2, clipY2, null);
    }

    public static void serializeImage(ObjectOutputStream out, BufferedImage img) throws IOException {
        assert img != null;
        int imgType = img.getType();
        int imgWidth = img.getWidth();
        int imgHeight = img.getHeight();

        out.writeInt(imgWidth);
        out.writeInt(imgHeight);
        out.writeInt(imgType);

        if (imgType == BufferedImage.TYPE_BYTE_GRAY) {
            ImageIO.write(img, "PNG", out);
        } else {
            int[] pixelsAsArray = getPixelsAsArray(img);
            for (int pixel : pixelsAsArray) {
                out.writeInt(pixel);
            }
        }
    }

    public static BufferedImage deserializeImage(ObjectInputStream in) throws IOException {
        int width = in.readInt();
        int height = in.readInt();
        int type = in.readInt();
        if (type == BufferedImage.TYPE_BYTE_GRAY) {
            return ImageIO.read(in);
        } else {
            BufferedImage img = new BufferedImage(width, height, type);
            int[] pixelsAsArray = getPixelsAsArray(img);
            for (int i = 0; i < pixelsAsArray.length; i++) {
                pixelsAsArray[i] = in.readInt();
            }
            return img;
        }
    }

    public static BufferedImage createThumbnail(BufferedImage src, int size, CheckerboardPainter painter) {
        assert src != null;

        Dimension thumbDim = calcThumbDimensions(src, size);

        return downSizeFast(src, painter, thumbDim.width, thumbDim.height);
    }

    public static Dimension calcThumbDimensions(BufferedImage src, int size) {
        int width = src.getWidth();
        int height = src.getHeight();

        int thumbWidth;
        int thumbHeight;
        if (width > height) {
            thumbWidth = size;
            float ratio = (float) width / (float) height;
            thumbHeight = (int) (size / ratio);
        } else {
            thumbHeight = size;
            float ratio = (float) height / (float) width;
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

    public static BufferedImage createThumbnail(BufferedImage src, int maxWidth, int maxHeight, CheckerboardPainter painter) {
        assert src != null;

        int imgWidth = src.getWidth();
        int imgHeight = src.getHeight();

        double xScaling = maxWidth / (double) imgWidth;
        double yScaling = maxHeight / (double) imgHeight;
        double scaling = Math.min(xScaling, yScaling);
        int thumbWidth = (int) (imgWidth * scaling);
        int thumbHeight = (int) (imgHeight * scaling);

        return downSizeFast(src, painter, thumbWidth, thumbHeight);
    }

    private static BufferedImage downSizeFast(BufferedImage src, CheckerboardPainter painter, int thumbWidth, int thumbHeight) {
        BufferedImage thumb = createSysCompatibleImage(thumbWidth, thumbHeight);
        Graphics2D g = thumb.createGraphics();

        if(painter != null) {
            painter.paint(g, null, thumbWidth, thumbHeight);
        }

        g.setRenderingHint(KEY_INTERPOLATION,
                VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(src, 0, 0, thumbWidth, thumbHeight, null);
        g.dispose();
        return thumb;
    }

    public static void paintRedXOnThumb(BufferedImage thumb) {
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

    public static BufferedImage copyImage(BufferedImage src) {
        assert src != null;

        WritableRaster raster = null;
        try {
            raster = src.copyData(null);
        } catch (OutOfMemoryError e) {
            Dialogs.showOutOfMemoryDialog(e);
        }
        return new BufferedImage(src.getColorModel(), raster, src.isAlphaPremultiplied(), null);
    }

    /**
     * In contrast to BufferedImage.getSubimage, this method creates a copy of the data
     */
    public static BufferedImage getCopiedSubimage(BufferedImage src, Rectangle bounds) {
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
        Raster startingFrom00 = copyRaster.createChild(intersection.x, intersection.y, intersection.width, intersection.height, 0, 0, null);
        return new BufferedImage(src.getColorModel(), (WritableRaster) startingFrom00, src.isAlphaPremultiplied(), null);
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

    public static BufferedImage crop(BufferedImage input, int x, int y, int width, int height) {
        assert input != null;

        if (width <= 0) {
            throw new IllegalArgumentException("width = " + width);
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height = " + height);
        }
        BufferedImage output = new BufferedImage(width
                , height
                , input.getType());
        Graphics2D g = output.createGraphics();
        AffineTransform t = AffineTransform.getTranslateInstance(-x, -y);
        g.transform(t);
        g.drawImage(input, null, 0, 0);
        g.dispose();

        return output;
    }


    public static int lerpAndPremultiplyColorWithAlpha(float t, int[] color1, int[] color2) {
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
                red *= f;
                green *= f;
                blue *= f;
            }
        }

        return (alpha << 24 | red << 16 | green << 8 | blue);
    }

    public static void screenWithItself(BufferedImage src, float opacity) {
        assert src != null;

        Graphics2D g = src.createGraphics();
        g.setComposite(new ScreenComposite(opacity));
        g.drawImage(src, 0, 0, null);
        g.dispose();
    }

    public static BufferedImage getHighPassSharpenedImage(BufferedImage original, BufferedImage blurred) {
        assert original != null;
        assert blurred != null;

        // the blurred image is the low-pass filtered version of the image
        // so we subtract it form the original by inverting it...
        Invert.invertImage(blurred, blurred);
        // ... and blending it at 50% with the original
        Graphics2D g = blurred.createGraphics();
        g.setComposite(AlphaComposite.getInstance(SRC_OVER, 0.5f));
        g.drawImage(original, 0, 0, null);
        g.dispose();

        // blend it with overlay to get a sharpening effect
        Graphics2D g2 = blurred.createGraphics();
        g2.setComposite(new OverlayComposite(1.0f));
        g2.drawImage(original, 0, 0, null);
        g2.dispose();
        return blurred;
    }

    public static BufferedImage createRandomPointsTemplateBrush(int diameter, float density) {
        if (density < 0.0 && density > 1.0) {
            throw new IllegalArgumentException("density is " + density);
        }

        BufferedImage brushImage = new BufferedImage(diameter, diameter, TYPE_INT_ARGB);

        int radius = diameter / 2;
        int radius2 = radius * radius;
        Random random = new Random();

        int[] pixels = ImageUtils.getPixelsAsArray(brushImage);
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

//        Fill.fillImage(brushImage, Color.BLACK);

        return brushImage;
    }

    public static BufferedImage createSoftBWBrush(int size) {
        BufferedImage brushImage = new BufferedImage(size, size, TYPE_INT_ARGB);

        Graphics2D g = brushImage.createGraphics();

        g.setColor(WHITE);
        g.fillRect(0, 0, size, size);

        g.setColor(BLACK);

        int softness = size / 4;

        g.fillOval(softness, softness, size - 2 * softness, size - 2 * softness);
        g.dispose();

        BoxBlurFilter blur = new BoxBlurFilter(softness, softness, 1, null);
        brushImage = blur.filter(brushImage, brushImage);

        return brushImage;
    }

    public static BufferedImage createSoftTransparencyImage(int size) {
        BufferedImage image = createSoftBWBrush(size);
        int[] pixels = ImageUtils.getPixelsAsArray(image);
        for (int i = 0, pixelsLength = pixels.length; i < pixelsLength; i++) {
            int pixelValue = pixels[i] & 0xFF; // take the blue channel: they are all the same
            int alpha = 255 - pixelValue;
            pixels[i] = alpha << 24;
        }
        return image;
    }

    public static BufferedImage getGridImageOnTransparentBackground(Color color, int maxX, int maxY, int hWidth, int hSpacing, int vWidth, int vSpacing, boolean emptyIntersections) {
        // create transparent image
        BufferedImage img = new BufferedImage(maxX, maxY, TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        drawGrid(color, g, maxX, maxY, hWidth, hSpacing, vWidth, vSpacing, emptyIntersections);
        g.dispose();
        return img;
    }

    public static void drawGrid(Color color, Graphics2D g, int maxX, int maxY, int hWidth, int hSpacing, int vWidth, int vSpacing, boolean emptyIntersections) {
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

    public static void drawBrickGrid(Color color, Graphics2D g, int size, int maxX, int maxY) {
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

    public static BufferedImage bumpMap(BufferedImage src, BufferedImage bumpMapSource, String filterName) {
        return bumpMap(src, bumpMapSource, (float) ImageUtils.DEG_315_IN_RADIANS, 0.53f, 2.0f, filterName);
    }

    public static BufferedImage bumpMap(BufferedImage src, BufferedImage bumpMapSource, float azimuth, float elevation, float bumpHeight, String filterName) {
        return bumpMap(src, bumpMapSource, BlendComposite.HardLight, azimuth, elevation, bumpHeight, filterName);
    }

    public static BufferedImage bumpMap(BufferedImage src, BufferedImage bumpMapSource, Composite composite, float azimuth, float elevation, float bumpHeight, String filterName) {
        // TODO optimize it so that the bumpMapSource can be smaller, and an offset is given - useful for text effects
        // tiling could be also an option

        EmbossFilter embossFilter = new EmbossFilter(filterName);
        embossFilter.setAzimuth(azimuth);
        embossFilter.setElevation(elevation);
        embossFilter.setBumpHeight(bumpHeight);

        BufferedImage bumpMap = embossFilter.filter(bumpMapSource, null);

        BufferedImage dest = ImageUtils.copyImage(src);

        Graphics2D g = dest.createGraphics();
        g.setComposite(composite);
        g.drawImage(bumpMap, 0, 0, null);
        g.dispose();

        return dest;
    }

    /**
     * Fills the BufferedImage with the specified color
     */
    public static void fillImage(BufferedImage img, Color c) {
        int[] pixels = getPixelsAsArray(img);

        int fillColor = c.getRGB();

//        int red = c.getRed();
//        int green = c.getGreen();
//        int blue = c.getBlue();
//
//        int fillColor = (0xFF000000 | (red << 16) | (green << 8) | blue);

        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = fillColor;
        }
    }

    public static int premultiply(int rgb) {
        int a = (rgb >>> 24) & 0xFF;
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8) & 0xFF;
        int b = rgb & 0xFF;

        float f = a * (1.0f / 255.0f);
        r *= f;
        g *= f;
        b *= f;
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
        r *= f;
        g *= f;
        b *= f;
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

//    // TODO this method increases the contrast of the image - why?
//    public static BufferedImage convertToGrayScaleImage(BufferedImage src) {
//        BufferedImage dest = new BufferedImage(src.getWidth(),
//                src.getHeight(),
//                TYPE_BYTE_GRAY);
//        ColorConvertOp colorConvertOp = new ColorConvertOp(null);
//        dest = colorConvertOp.filter(src, dest);
//        return dest;
//    }

    public static BufferedImage convertToGrayScaleImage(BufferedImage src) {
        BufferedImage dest = new BufferedImage(src.getWidth(),
                src.getHeight(),
                TYPE_BYTE_GRAY);
        Graphics2D g2 = dest.createGraphics();
        g2.drawImage(src, 0, 0, null);
        g2.dispose();

        return dest;
    }

    public static void paintAffectedAreaShapes(BufferedImage image, Shape[] shapes) {
        Graphics2D g = image.createGraphics();
        g.setColor(BLACK);

        ZoomLevel zoomLevel = ImageComponents.getActiveIC().getZoomLevel();

//        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setStroke(zoomLevel.getOuterGeometryStroke());

        for (Shape shape : shapes) {
            g.draw(shape);
        }
        g.setColor(WHITE);
        g.setStroke(zoomLevel.getInnerGeometryStroke());

        for (Shape shape : shapes) {
            g.draw(shape);
        }

        g.dispose();
    }

    public static void debugImageToText(BufferedImage img) {
        BufferedImageNode imgNode = new BufferedImageNode("debug", img);
        String s = imgNode.toDetailedString();
        System.out.println(String.format("ImageUtils::debugImage: s = '%s'", s));
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
                    String msg = String.format("at (%d, %d) rgb1 is %s and rgb2 is %s",
                            x, y, ColorUtils.intColorToString(rgb1), ColorUtils.intColorToString(rgb2));
                    System.out.println(String.format("ImageUtils::compareSmallImages: %s", msg));
                    return false;
                }
            }
        }

        return true;
    }

    public static String debugSmallImage(BufferedImage im) {
        int width = im.getWidth();
        int height = im.getHeight();
        StringBuilder s = new StringBuilder();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = im.getRGB(x, y);
                String asString = ColorUtils.intColorToString(rgb);
                s.append(asString);
                if(x == width - 1) {
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
        img.setRGB(0, 0, ColorUtils.toPackedInt(a, r, g, b));
        return img;
    }
}
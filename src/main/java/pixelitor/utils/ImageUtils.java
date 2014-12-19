/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.utils;

import com.jhlabs.composite.OverlayComposite;
import com.jhlabs.composite.ScreenComposite;
import com.jhlabs.image.BoxBlurFilter;
import com.jhlabs.image.EmbossFilter;
import org.jdesktop.swingx.graphics.BlendComposite;
import pixelitor.ImageComponents;
import pixelitor.filters.Invert;
import pixelitor.menus.view.ZoomLevel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.DataBufferInt;
import java.awt.image.PixelGrabber;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.Random;

/**
 * Image utility methods
 */
public class ImageUtils {
    public static final double DEG_315_IN_RADIANS = 0.7853981634;
    public static final float[] FRACTIONS_2_COLOR_UNIFORM = {0.0f, 1.0f};

    /**
     * Utility class with static methods
     */
    private ImageUtils() {
    }

    public static BufferedImage transformToCompatibleImage(BufferedImage input) {
        assert input != null;

        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

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

    public static BufferedImage createCompatibleImage(int width, int height) {
        assert (width > 0) && (height > 0);

        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        BufferedImage output = gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
        return output;
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

        // in these two cases the original method from the Filthy Rich Clients book goes into infinite loop!
        if ((prevH < targetHeight) && (prevW > targetWidth)) {
            return simpleResize(img, targetWidth, targetHeight, hint);
        }
        if ((prevH > targetHeight) && (prevW < targetWidth)) {
            return simpleResize(img, targetWidth, targetHeight, hint);
        }


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
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
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

    private static BufferedImage simpleResize(BufferedImage img, int targetWidth, int targetHeight, Object hint) {
        assert img != null;

        BufferedImage ret = new BufferedImage(targetWidth, targetHeight, img.getType());
        Graphics2D g2 = ret.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
        g2.drawImage(img, 0, 0, targetWidth, targetHeight, null);
        g2.dispose();
        return ret;
    }

    // TODO possibly duplicate functionality
    public static BufferedImage resizeImage(double newSize, BufferedImage original) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        int maxOriginalSize = Math.max(originalWidth, originalHeight);
        double ratio = ((double) maxOriginalSize) / newSize;
        int imageWidth = (int) (originalWidth / ratio);
        int imageHeight = (int) (originalHeight / ratio);
        BufferedImage resizedImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g2 = resizedImage.createGraphics();
        g2.drawImage(original, 0, 0, imageWidth, imageHeight, null);
        g2.dispose();
        return resizedImage;
    }

    public static String intColorToString(int color) {
        Color c = new Color(color);
        return "[r=" + c.getRed() + ", g=" + c.getGreen() + ", b=" + c.getBlue() + ']';
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

        return (type == BufferedImage.TYPE_INT_ARGB_PRE || type == BufferedImage.TYPE_INT_RGB || type == BufferedImage.TYPE_INT_ARGB);
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
        } else {
            int width = src.getWidth();
            int height = src.getHeight();
            pixels = new int[width * height];
            PixelGrabber pg = new PixelGrabber(src, 0, 0, width, height, pixels, 0, width);

            try {
                pg.grabPixels();
            } catch (InterruptedException e) {
                Dialogs.showExceptionDialog(e);
            }
        }
        return pixels;
    }

    public static URL resourcePathToURL(String fileName) {
        assert fileName != null;

        String iconPath = "/images/" + fileName;
        URL imgURL = ImageUtils.class.getResource(iconPath);
        if (imgURL == null) {
            JOptionPane.showMessageDialog(null, iconPath + " not found", "Error", JOptionPane.ERROR_MESSAGE);
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
            Dialogs.showExceptionDialog(e);
        }
        return image;
    }

    public static int limitTo8Bits(int value) {
        if (value > 0xFF) {
            return 0xFF;
        }
        if (value < 0) {
            return 0;
        }
        return value;
    }

    public static short limitTo8Bits(short value) {
        if (value > 0xFF) {
            return 0xFF;
        }
        if (value < 0) {
            return 0;
        }
        return value;
    }

    public static BufferedImage convertToARGB_PRE(BufferedImage src, boolean oldCanBeFlushed) {
        assert src != null;

        BufferedImage dest = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
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

        BufferedImage dest = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
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

        BufferedImage dest = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dest.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();

        if (oldCanBeFlushed) {
            src.flush();
        }

        return dest;
    }

    public static Color getRandomColor(boolean randomAlpha) {
        Random rnd = new Random();
        int r = rnd.nextInt(256);
        int g = rnd.nextInt(256);
        int b = rnd.nextInt(256);

        if (randomAlpha) {
            int a = rnd.nextInt(256);
            return new Color(r, g, b, a);
        }

        return new Color(r, g, b);
    }

    /**
     * Calculates the average of two colors in the HSB space. Full opacity is assumed.
     */
    public static Color getHSBAverageColor(Color c1, Color c2) {
        assert c1 != null && c2 != null;

        int rgb1 = c1.getRGB();
        int rgb2 = c2.getRGB();

        int r1 = (rgb1 >>> 16) & 0xFF;
        int g1 = (rgb1 >>> 8) & 0xFF;
        int b1 = (rgb1) & 0xFF;

        int r2 = (rgb2 >>> 16) & 0xFF;
        int g2 = (rgb2 >>> 8) & 0xFF;
        int b2 = (rgb2) & 0xFF;

        float[] hsb1 = Color.RGBtoHSB(r1, g1, b1, null);
        float[] hsb2 = Color.RGBtoHSB(r2, g2, b2, null);

        float hue1 = hsb1[0];
        float hue2 = hsb2[0];
        float hue = calculateHueAverage(hue1, hue2);

        float sat = (hsb1[1] + hsb2[1]) / 2.0f;
        float bri = (hsb1[2] + hsb2[2]) / 2.0f;
        return Color.getHSBColor(hue, sat, bri);
    }

    private static float calculateHueAverage(float f1, float f2) {
        float delta = f1 - f2;
        if (delta < 0.5f && delta > -0.5f) {
            return (f1 + f2) / 2.0f;
        } else if (delta >= 0.5f) { // f1 is bigger
            float retVal = f1 + (1.0f - f1 + f2) / 2.0f;
            return retVal;
        } else if (delta <= 0.5f) { // f2 is bigger
            float retVal = f2 + (1.0f - f2 + f1) / 2.0f;
            return retVal;
        } else {
            throw new IllegalStateException("should not get here");
        }
    }

    /**
     * Calculates the average of two colors in the RGB space. Full opacity is assumed.
     */
    public static Color getRGBAverageColor(Color c1, Color c2) {
        assert c1 != null && c2 != null;

        int rgb1 = c1.getRGB();
        int rgb2 = c2.getRGB();

        int r1 = (rgb1 >>> 16) & 0xFF;
        int g1 = (rgb1 >>> 8) & 0xFF;
        int b1 = (rgb1) & 0xFF;

        int r2 = (rgb2 >>> 16) & 0xFF;
        int g2 = (rgb2 >>> 8) & 0xFF;
        int b2 = (rgb2) & 0xFF;

        int r = (r1 + r2) / 2;
        int g = (g1 + g2) / 2;
        int b = (b1 + b2) / 2;

        return new Color(r, g, b);
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

        out.writeInt(img.getWidth());
        out.writeInt(img.getHeight());
        out.writeInt(img.getType());
        int[] pixelsAsArray = getPixelsAsArray(img);
        for (int pixel : pixelsAsArray) {
            out.writeInt(pixel);
        }
    }

    public static BufferedImage deserializeImage(ObjectInputStream in) throws IOException {
        int width = in.readInt();
        int height = in.readInt();
        int type = in.readInt();
        BufferedImage img = new BufferedImage(width, height, type);
        int[] pixelsAsArray = getPixelsAsArray(img);
        for (int i = 0; i < pixelsAsArray.length; i++) {
            pixelsAsArray[i] = in.readInt();
        }
        return img;
    }

    public static BufferedImage createThumbnail(BufferedImage src, int size) {
        assert src != null;

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

        BufferedImage thumb = new BufferedImage(thumbWidth, thumbHeight, src.getType());
        Graphics2D g = thumb.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(src, 0, 0, thumbWidth, thumbHeight, null);
        } finally {
            g.dispose();
        }
        return thumb;
    }

    public static BufferedImage copyImage(BufferedImage src) {
        assert src != null;

        WritableRaster raster = null;
        try {
            raster = src.copyData(null);
        } catch (OutOfMemoryError e) {
            Dialogs.showOutOfMemoryDialog();
        }
        return new BufferedImage(src.getColorModel(), raster, src.isAlphaPremultiplied(), null);
    }

    /**
     * In contrast to BufferedImage.getSubimage, this method creates a copy of the data
     */
    public static BufferedImage copyAndTranslateSubimage(BufferedImage src, Rectangle bounds) {
        assert src != null;
        assert bounds != null;

        Rectangle imageBounds = new Rectangle(0, 0, src.getWidth(), src.getHeight());
        // TODO SwingUtilities.computeIntersection can do this without allocating a rectangle
        Rectangle intersection = bounds.intersection(imageBounds);

        if (intersection.width <= 0 || intersection.height <= 0) {
            throw new IllegalStateException("empty intersection: bounds = " + bounds + ", imageBounds = " + imageBounds + ", intersection = " + intersection);
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
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
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
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
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

        BufferedImage brushImage = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);

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

    public static BufferedImage createSoftTemplateBrush(int size) {
        BufferedImage brushImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = brushImage.createGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size, size);

        g.setColor(Color.BLACK);

        int softness = size / 4;

        g.fillOval(softness, softness, size - 2 * softness, size - 2 * softness);
        g.dispose();


        BoxBlurFilter blur = new BoxBlurFilter(softness, softness, 1);
        brushImage = blur.filter(brushImage, brushImage);

        return brushImage;
    }

    public static BufferedImage getGridImageOnTransparentBackground(Color color, int maxX, int maxY, int hWidth, int hSpacing, int vWidth, int vSpacing, boolean emptyIntersections) {
        // create transparent image
        BufferedImage img = new BufferedImage(maxX, maxY, BufferedImage.TYPE_INT_ARGB);
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

    public static BufferedImage bumpMap(BufferedImage src, BufferedImage bumpMapSource) {
        return bumpMap(src, bumpMapSource, (float) ImageUtils.DEG_315_IN_RADIANS, 0.53f, 2.0f);
    }

    public static BufferedImage bumpMap(BufferedImage src, BufferedImage bumpMapSource, float azimuth, float elevation, float bumpHeight) {
        return bumpMap(src, bumpMapSource, BlendComposite.HardLight, azimuth, elevation, bumpHeight);
    }

    public static BufferedImage bumpMap(BufferedImage src, BufferedImage bumpMapSource, Composite composite, float azimuth, float elevation, float bumpHeight) {
        // TODO optimize it so that the bumpMapSource can be smaller, and an offset is given - useful for text effects
        // tiling could be also an option

        EmbossFilter embossFilter = new EmbossFilter();
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

    public static BufferedImage convertToGrayScaleImage(BufferedImage src) {
        BufferedImage dest = new BufferedImage(src.getWidth(),
                src.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);
        ColorConvertOp colorConvertOp = new ColorConvertOp(null);
        dest = colorConvertOp.filter(src, dest);
        return dest;
    }

    public static void drawLightning(float startX, float startY, float endX, float endY) {
        float dx = endX - startX;
        float dy = endY - startY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        int numIntermediatePointIterations = 10;
        float maxDeviationDist = (float) (dist / 2);

        for (int i = 0; i < numIntermediatePointIterations; i++) {
        }
    }

    public static void paintAffectedAreaShapes(BufferedImage image, Shape[] shapes) {
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLACK);

        ZoomLevel zoomLevel = ImageComponents.getActiveImageComponent().getZoomLevel();

//        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setStroke(zoomLevel.getOuterGeometryStroke());

        for (Shape shape : shapes) {
            g.draw(shape);
        }
        g.setColor(Color.WHITE);
        g.setStroke(zoomLevel.getInnerGeometryStroke());

        for (Shape shape : shapes) {
            g.draw(shape);
        }

        g.dispose();
    }

    public static float calcSaturation(int r, int g, int b) {
        float sat;
        int cmax = (r > g) ? r : g;
        if (b > cmax) {
            cmax = b;
        }
        int cmin = (r < g) ? r : g;
        if (b < cmin) {
            cmin = b;
        }

        if (cmax != 0) {
            sat = ((float) (cmax - cmin)) / ((float) cmax);
        } else {
            sat = 0;
        }
        return sat;
    }
}
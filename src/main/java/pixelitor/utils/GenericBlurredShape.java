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

import com.jhlabs.image.BoxBlurFilter;
import pixelitor.colors.Colors;
import pixelitor.tools.shapes.ShapeType;
import pixelitor.tools.shapes.StarSettings;
import pixelitor.tools.util.Drag;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;

import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;

/**
 * A generic {@link BlurredShape} with a customizable boundary.
 */
public class GenericBlurredShape implements BlurredShape {
    private final double imgTx;
    private final double imgTy;
    private final int imgWidth;
    private final int imgHeight;
    private final byte[] pixels;

    private static final Object LOCK = new Object();

    // A helper class to hold the cache key and the heavy payload
    private static class CacheEntry {
        final int type;
        final double innerRadiusX, innerRadiusY, outerRadiusX, outerRadiusY;
        final WeakReference<byte[]> pixelsRef;

        CacheEntry(int type, double inX, double inY, double outX, double outY, byte[] pixels) {
            this.type = type;
            this.innerRadiusX = inX;
            this.innerRadiusY = inY;
            this.outerRadiusX = outX;
            this.outerRadiusY = outY;
            this.pixelsRef = new WeakReference<>(pixels);
        }

        boolean matches(int type, double inX, double inY, double outX, double outY) {
            return this.type == type
                && this.innerRadiusX == inX
                && this.innerRadiusY == inY
                && this.outerRadiusX == outX
                && this.outerRadiusY == outY;
        }
    }

    private static CacheEntry lastCacheEntry;

    public static GenericBlurredShape of(int type,
                                         Point2D center,
                                         double innerRadiusX, double innerRadiusY,
                                         double outerRadiusX, double outerRadiusY) {

        byte[] cachedPixels = null;

        synchronized (LOCK) {
            if (lastCacheEntry != null && lastCacheEntry.matches(type, innerRadiusX, innerRadiusY, outerRadiusX, outerRadiusY)) {
                cachedPixels = lastCacheEntry.pixelsRef.get();
            }
        }

        // returns a new wrapper if the byte[] payload was cached
        if (cachedPixels != null) {
            return new GenericBlurredShape(cachedPixels, center, outerRadiusX, outerRadiusY);
        }

        // otherwise, generate the heavy payload
        byte[] newPixels = generatePixels(type, innerRadiusX, innerRadiusY, outerRadiusX, outerRadiusY);

        // update cache
        synchronized (LOCK) {
            lastCacheEntry = new CacheEntry(type, innerRadiusX, innerRadiusY, outerRadiusX, outerRadiusY, newPixels);
        }

        return new GenericBlurredShape(newPixels, center, outerRadiusX, outerRadiusY);
    }

    // maps an instance to the shared pixel array
    private GenericBlurredShape(byte[] pixels, Point2D center, double outerRadiusX, double outerRadiusY) {
        this.pixels = pixels;
        this.imgWidth = (int) (2 * outerRadiusX);
        this.imgHeight = (int) (2 * outerRadiusY);
        this.imgTx = center.getX() - outerRadiusX;
        this.imgTy = center.getY() - outerRadiusY;
    }

    private static byte[] generatePixels(int type,
                                         double innerRadiusX, double innerRadiusY,
                                         double outerRadiusX, double outerRadiusY) {
        int imgWidth = (int) (2 * outerRadiusX);
        int imgHeight = (int) (2 * outerRadiusY);
        BufferedImage img = new BufferedImage(imgWidth, imgHeight, TYPE_BYTE_GRAY);
        Graphics2D g2 = img.createGraphics();
        Colors.fillWith(Color.WHITE, g2, imgWidth, imgHeight);

        // the shape bounds within the mask image
        double shapeStartX = (outerRadiusX - innerRadiusX) / 2.0;
        double shapeStartY = (outerRadiusY - innerRadiusY) / 2.0;
        double shapeEndX = 2 * outerRadiusX - shapeStartX;
        double shapeEndY = 2 * outerRadiusY - shapeStartY;

        // use the type ID to extract the shape from the factory
        Shape shape = createShapeForType(type, new Drag(shapeStartX, shapeStartY, shapeEndX, shapeEndY));
        g2.setClip(shape);
        Colors.fillWith(Color.BLACK, g2, imgWidth, imgHeight);
        g2.dispose();

        var blurFilter = createBlurFilter(shapeStartX, shapeStartY);
        img = blurFilter.filter(img, null);

        return ImageUtils.getGrayPixels(img);
    }

    private static Shape createShapeForType(int type, Drag drag) {
        return switch (type) {
            case BlurredShape.TYPE_RECTANGLE -> ShapeType.RECTANGLE.createShape(drag, null);
            case BlurredShape.TYPE_HEART -> ShapeType.HEART.createShape(drag, null);
            case BlurredShape.TYPE_DIAMOND -> ShapeType.DIAMOND.createShape(drag, null);
            case BlurredShape.TYPE_HEXAGON -> ShapeType.STAR.createShape(drag, new StarSettings(3, 100));
            case BlurredShape.TYPE_OCTAGON -> ShapeType.STAR.createShape(drag, new StarSettings(4, 100));
            case BlurredShape.TYPE_STAR -> ShapeType.STAR.createShape(drag, new StarSettings());
            default -> throw new IllegalArgumentException("type: " + type);
        };
    }

    private static BoxBlurFilter createBlurFilter(double shapeStartX, double shapeStartY) {
        int iterations = 3;
        // cast first to int in order to avoid fractional blurring
        float hRadius = (int) (shapeStartX / iterations);
        float vRadius = (int) (shapeStartY / iterations);
        var blurFilter = new BoxBlurFilter(hRadius, vRadius, iterations, "");
        blurFilter.setPremultiplyAlpha(false);

        // it would be complicated to set up better progress tracking
        // because we would have to know in advance whether we can cache
        blurFilter.setProgressTracker(ProgressTracker.NO_OP_TRACKER);
        return blurFilter;
    }

    @Override
    public double isOutside(int x, int y) {
        // the coordinates relative to the image
        int imgX = (int) (x - imgTx);
        int imgY = (int) (y - imgTy);

        // outside the image bounds we are definitely outside the shape
        if (imgX < 0 || imgX >= imgWidth || imgY < 0 || imgY >= imgHeight) {
            return 1;
        }

        // inside the image bounds return the normalized pixel intensity
        int pixel = pixels[imgX + imgWidth * imgY];
        // transform from unsigned byte to int
        return (pixel & 0xff) / 255.0;
    }
}

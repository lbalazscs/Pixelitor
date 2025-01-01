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

import com.jhlabs.image.BoxBlurFilter;
import pixelitor.colors.Colors;
import pixelitor.tools.util.Drag;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.function.Function;

import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;

/**
 * A generic {@link BlurredShape} with a customizable boundary.
 */
public class GenericBlurredShape implements BlurredShape {
    // the position of the image within the shape
    private double imgTx;
    private double imgTy;

    private final int imgWidth, imgHeight;

    private final Function<Drag, Shape> shapeFactory;
    private final double innerRadiusX;
    private final double innerRadiusY;
    private final double outerRadiusX;
    private final double outerRadiusY;
    private final byte[] pixels;

    // cache the most recent instance since the image pixels don't
    // need recalculation when only the shape center changes
    private static WeakReference<GenericBlurredShape> instanceCache;

    public static GenericBlurredShape of(Function<Drag, Shape> shapeFactory,
                                         Point2D center,
                                         double innerRadiusX, double innerRadiusY,
                                         double outerRadiusX, double outerRadiusY) {
        GenericBlurredShape cachedShape = instanceCache != null ? instanceCache.get() : null;

        if (cachedShape != null
            && shapeFactory == cachedShape.shapeFactory
            && innerRadiusX == cachedShape.innerRadiusX
            && innerRadiusY == cachedShape.innerRadiusY
            && outerRadiusX == cachedShape.outerRadiusX
            && outerRadiusY == cachedShape.outerRadiusY) {

            // reuse the image of the last blurred shape
            cachedShape.updateCenter(center);
            return cachedShape;
        }

        GenericBlurredShape newShape = new GenericBlurredShape(shapeFactory, center,
            innerRadiusX, innerRadiusY, outerRadiusX, outerRadiusY);
        instanceCache = new WeakReference<>(newShape);
        return newShape;
    }

    private GenericBlurredShape(Function<Drag, Shape> shapeFactory,
                                Point2D center,
                                double innerRadiusX, double innerRadiusY,
                                double outerRadiusX, double outerRadiusY) {
        this.shapeFactory = shapeFactory;
        this.innerRadiusX = innerRadiusX;
        this.innerRadiusY = innerRadiusY;
        this.outerRadiusX = outerRadiusX;
        this.outerRadiusY = outerRadiusY;

        updateCenter(center);

        imgWidth = (int) (2 * outerRadiusX);
        imgHeight = (int) (2 * outerRadiusY);
        BufferedImage img = new BufferedImage(imgWidth, imgHeight, TYPE_BYTE_GRAY);
        Graphics2D g2 = img.createGraphics();
        Colors.fillWith(Color.WHITE, g2, imgWidth, imgHeight);

        // the shape bounds within the mask image
        double shapeStartX = (outerRadiusX - innerRadiusX) / 2.0;
        double shapeStartY = (outerRadiusY - innerRadiusY) / 2.0;
        double shapeEndX = 2 * outerRadiusX - shapeStartX;
        double shapeEndY = 2 * outerRadiusY - shapeStartY;

        Shape shape = shapeFactory.apply(
            new Drag(shapeStartX, shapeStartY, shapeEndX, shapeEndY));
        g2.setClip(shape);
        Colors.fillWith(Color.BLACK, g2, imgWidth, imgHeight);
        g2.dispose();

        var blurFilter = createBlurFilter(shapeStartX, shapeStartY);
        img = blurFilter.filter(img, null);

        pixels = ImageUtils.getGrayPixelByteArray(img);
    }

    private static BoxBlurFilter createBlurFilter(double shapeStartX, double shapeStartY) {
        int iterations = 3;
        // cast first to int in order to avoid fractional blurring
        float hRadius = (int) (shapeStartX / iterations);
        float vRadius = (int) (shapeStartY / iterations);
        var blurFilter = new BoxBlurFilter(
            hRadius, vRadius, iterations, "");
        blurFilter.setPremultiplyAlpha(false);

        // it would be complicated to set up better progress tracking
        // because we would have to know in advance whether we can cache
        blurFilter.setProgressTracker(ProgressTracker.NULL_TRACKER);
        return blurFilter;
    }

    private void updateCenter(Point2D newCenter) {
        imgTx = newCenter.getX() - outerRadiusX;
        imgTy = newCenter.getY() - outerRadiusY;
    }

    @Override
    public double isOutside(int x, int y) {
        // the coordinates relative to the image
        int imgX = (int) (x - imgTx);
        int imgY = (int) (y - imgTy);

        // outside the image bounds we are definitely outside the shape
        if (imgX < 0 || imgX > imgWidth || imgY < 0 || imgY > imgHeight) {
            return 1;
        }

        // inside the image bounds return the normalized pixel intensity
        try {
            int pixel = pixels[imgX + imgWidth * imgY];
            // transform from unsigned byte to int
            return (pixel & 0xff) / 255.0;
        } catch (ArrayIndexOutOfBoundsException e) {
            // TODO this shouldn't happen as the outside case was already handled
            return 1;
        }
    }
}

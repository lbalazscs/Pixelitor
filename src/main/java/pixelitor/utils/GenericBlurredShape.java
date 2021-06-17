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

import com.jhlabs.image.BoxBlurFilter;
import pixelitor.colors.Colors;
import pixelitor.tools.shapes.ShapeType;
import pixelitor.tools.util.Drag;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;

import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;

/**
 * A {@link BlurredShape} which can take any shape
 */
public class GenericBlurredShape implements BlurredShape {
    private double imgTx;
    private double imgTy;
    private final int imgWidth, imgHeight;

    private final ShapeType shapeType;
    private final double innerRadiusX;
    private final double innerRadiusY;
    private final double outerRadiusX;
    private final double outerRadiusY;
    private final byte[] pixels;

    private static WeakReference<GenericBlurredShape> lastRef;

    public static GenericBlurredShape of(ShapeType shapeType,
                                         Point2D center,
                                         double innerRadiusX, double innerRadiusY,
                                         double outerRadiusX, double outerRadiusY) {
        if (lastRef == null) {
            GenericBlurredShape last = new GenericBlurredShape(shapeType,
                center,
                innerRadiusX, innerRadiusY,
                outerRadiusX, outerRadiusY);
            lastRef = new WeakReference<>(last);
            return last;
        }

        GenericBlurredShape last = lastRef.get();
        if (last != null
            && shapeType == last.shapeType
            && innerRadiusX == last.innerRadiusX
            && innerRadiusY == last.innerRadiusY
            && outerRadiusX == last.outerRadiusX
            && outerRadiusY == last.outerRadiusY) {
            // if only the center changed,
            // there is no need to recreate the image
            last.recenter(center);
            return last;
        }

        // there was a radius or softness change
        last = new GenericBlurredShape(shapeType, center,
            innerRadiusX, innerRadiusY,
            outerRadiusX, outerRadiusY);
        lastRef = new WeakReference<>(last);
        return last;
    }

    private GenericBlurredShape(ShapeType shapeType,
                                Point2D center,
                                double innerRadiusX, double innerRadiusY,
                                double outerRadiusX, double outerRadiusY) {
        this.shapeType = shapeType;
        this.innerRadiusX = innerRadiusX;
        this.innerRadiusY = innerRadiusY;
        this.outerRadiusX = outerRadiusX;
        this.outerRadiusY = outerRadiusY;

        recenter(center);

        imgWidth = (int) (2 * outerRadiusX);
        imgHeight = (int) (2 * outerRadiusY);
        BufferedImage img = new BufferedImage(imgWidth, imgHeight, TYPE_BYTE_GRAY);
        Graphics2D g2 = img.createGraphics();
        Colors.fillWith(Color.WHITE, g2, imgWidth, imgHeight);

        // the shape coordinates within the blurred image
        double shapeStartX = (outerRadiusX - innerRadiusX) / 2.0;
        double shapeStartY = (outerRadiusY - innerRadiusY) / 2.0;
        double shapeEndX = 2 * outerRadiusX - shapeStartX;
        double shapeEndY = 2 * outerRadiusY - shapeStartY;

        Shape shape = shapeType.createShape(
            new Drag(shapeStartX, shapeStartY, shapeEndX, shapeEndY), null);
        g2.setClip(shape);
        Colors.fillWith(Color.BLACK, g2, imgWidth, imgHeight);
        g2.dispose();

        int numIterations = 3;
        // cast first to int in order to avoid fractional blurring
        float hRadius = (int) (shapeStartX / numIterations);
        float vRadius = (int) (shapeStartY / numIterations);
        var blurFilter = new BoxBlurFilter(
            hRadius, vRadius, numIterations, "");
        blurFilter.setPremultiplyAlpha(false);

        // it would be complicated to set up a better progress tracking
        // because we would have to know in advance whether we can cache
        blurFilter.setProgressTracker(ProgressTracker.NULL_TRACKER);
        img = blurFilter.filter(img, null);

        pixels = ImageUtils.getGrayPixelsAsByteArray(img);
    }

    private void recenter(Point2D center) {
        // the blurred image translation relative to the source
        imgTx = center.getX() - outerRadiusX;
        imgTy = center.getY() - outerRadiusY;
    }

    @Override
    public double isOutside(int x, int y) {
        if (x < imgTx) {
            return 1;
        }
        if (y < imgTy) {
            return 1;
        }
        if (x > imgTx + imgWidth) {
            return 1;
        }
        if (y > imgTy + imgHeight) {
            return 1;
        }

        int xx = (int) (x - imgTx);
        int yy = (int) (y - imgTy);
        int pixel;
        try {
            pixel = pixels[xx + imgWidth * yy];
        } catch (ArrayIndexOutOfBoundsException e) {
            return 1;
        }
        // transform from unsigned byte to int
        return (pixel & 0xff) / 255.0;
    }
}

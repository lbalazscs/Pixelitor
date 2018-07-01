/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.tools.shapes.ShapeType;
import pixelitor.tools.util.ImDrag;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;

/**
 * A {@link BlurredShape} which can take any shape
 */
public class BlurredAnyShape implements BlurredShape {
    private double imgTx;
    private double imgTy;
    private final int imgWidth, imgHeight;

    private final ShapeType shapeType;
    private final double innerRadiusX;
    private final double innerRadiusY;
    private final double outerRadiusX;
    private final double outerRadiusY;
    final byte[] pixels;

    private static WeakReference<BlurredAnyShape> lastRef;

    public static BlurredAnyShape get(ShapeType shapeType, double centerX, double centerY, double innerRadiusX, double innerRadiusY, double outerRadiusX, double outerRadiusY) {
        if (lastRef == null) {
            BlurredAnyShape last = new BlurredAnyShape(shapeType, centerX, centerY, innerRadiusX, innerRadiusY, outerRadiusX, outerRadiusY);
            lastRef = new WeakReference<>(last);
            return last;
        }

        BlurredAnyShape last = lastRef.get();
        if (last != null
                && shapeType == last.shapeType
                && innerRadiusX == last.innerRadiusX
                && innerRadiusY == last.innerRadiusY
                && outerRadiusX == last.outerRadiusX
                && outerRadiusY == last.outerRadiusY) {
            // if only the center changed,
            // there is no need to recreate the image
            last.recenter(centerX, centerY);
            return last;
        }

        // there was a radius or softness change
        last = new BlurredAnyShape(shapeType, centerX, centerY, innerRadiusX, innerRadiusY, outerRadiusX, outerRadiusY);
        lastRef = new WeakReference<>(last);
        return last;
    }

    private BlurredAnyShape(ShapeType shapeType, double centerX, double centerY, double innerRadiusX, double innerRadiusY, double outerRadiusX, double outerRadiusY) {
        this.shapeType = shapeType;
        this.innerRadiusX = innerRadiusX;
        this.innerRadiusY = innerRadiusY;
        this.outerRadiusX = outerRadiusX;
        this.outerRadiusY = outerRadiusY;

        recenter(centerX, centerY);

        imgWidth = (int) (2 * outerRadiusX);
        imgHeight = (int) (2 * outerRadiusY);
        BufferedImage img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, imgWidth, imgHeight);

        // the shape coordinates within the blurred image
        double shapeStartX = (outerRadiusX - innerRadiusX) / 2.0;
        double shapeStartY = (outerRadiusY - innerRadiusY) / 2.0;
        double shapeEndX = 2 * outerRadiusX - shapeStartX;
        double shapeEndY = 2 * outerRadiusY - shapeStartY;

        Shape shape = shapeType.getShape(new ImDrag(shapeStartX, shapeStartY, shapeEndX, shapeEndY));
        g2.setClip(shape);
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, imgWidth, imgHeight);
        g2.dispose();

        // cast first to int in order to avoid fractional blurring
        int numIterations = 3;
        float hRadius = (float) ((int) (shapeStartX / numIterations));
        float vRadius = (float) ((int) (shapeStartY / numIterations));
        BoxBlurFilter blurFilter = new BoxBlurFilter(hRadius, vRadius, numIterations, "");
        blurFilter.setPremultiplyAlpha(false);

        // it would be complicated to set up a better progress tracking
        // because we would have to know in advance whether we can cache
        blurFilter.setProgressTracker(ProgressTracker.NULL_TRACKER);
        img = blurFilter.filter(img, null);

        pixels = ImageUtils.getGrayPixelsAsByteArray(img);
    }

    private void recenter(double centerX, double centerY) {
        // the blurred image translation relative to the source
        imgTx = centerX - outerRadiusX;
        imgTy = centerY - outerRadiusY;
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
        int pixel = 0;
        try {
            pixel = pixels[xx + imgWidth * yy];
        } catch (ArrayIndexOutOfBoundsException e) {
            return 1;
        }
        // transform from unsigned byte to int
        return (pixel & 0xff) / 255.0;
    }
}

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

package pixelitor.layers;

import com.jhlabs.image.PointFilter;
import pixelitor.Canvas;
import pixelitor.colors.Colors;
import pixelitor.utils.ProgressTracker;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.image.BufferedImage;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;

/**
 * Defines how a new layer mask is initialized when created.
 */
public enum MaskInitMethod {
    REVEAL_ALL("Reveal All", false) {
        @Override
        BufferedImage createMaskImage(Layer layer, Shape selShape) {
            // a fully white mask reveals the entire layer
            return createSolidMask(layer, Color.WHITE);
        }
    }, HIDE_ALL("Hide All", false) {
        @Override
        BufferedImage createMaskImage(Layer layer, Shape selShape) {
            // a fully black mask hides the entire layer
            return createSolidMask(layer, Color.BLACK);
        }
    }, REVEAL_SELECTION("Reveal Selection", true) {
        @Override
        BufferedImage createMaskImage(Layer layer, Shape selShape) {
            // black hiding mask, but the selection is shown
            return createSelectionMask(layer, Color.BLACK, Color.WHITE, selShape);
        }
    }, HIDE_SELECTION("Hide Selection", true) {
        @Override
        BufferedImage createMaskImage(Layer layer, Shape selShape) {
            // white revealing mask, but the selection is hidden
            return createSelectionMask(layer, Color.WHITE, Color.BLACK, selShape);
        }
    }, FROM_TRANSPARENCY("From Transparency", false) {
        @Override
        BufferedImage createMaskImage(Layer layer, Shape selShape) {
            return createMaskFromLayer(layer, true);
        }
    }, FROM_LAYER("From Layer", false) {
        @Override
        BufferedImage createMaskImage(Layer layer, Shape selShape) {
            return createMaskFromLayer(layer, false);
        }
    };

    // Returns a canvas-sized image filled with the given color.
    private static BufferedImage createSolidMask(Layer layer, Color color) {
        Canvas canvas = layer.getComp().getCanvas();
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        BufferedImage grayImage = new BufferedImage(width, height, TYPE_BYTE_GRAY);
        Graphics2D g = grayImage.createGraphics();

        Colors.fillWith(color, g, width, height);

        g.dispose();
        return grayImage;
    }

    // Returns a canvas-sized image filled with the background color,
    // and fills the given shape with the foreground color.
    private static BufferedImage createSelectionMask(Layer layer, Color bg, Color fg, Shape shape) {
        Canvas canvas = layer.getComp().getCanvas();
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        BufferedImage grayImage = new BufferedImage(width, height, TYPE_BYTE_GRAY);
        Graphics2D g = grayImage.createGraphics();

        // fill background
        Colors.fillWith(bg, g, width, height);

        // fill shape
        g.setColor(fg);
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.fill(shape);

        g.dispose();
        return grayImage;
    }

    // Returns a canvas-size grayscale image representing
    // the visible contents of the layer
    private static BufferedImage createMaskFromLayer(Layer layer,
                                                     boolean onlyTransparency) {
        BufferedImage image = layer.toImage(false, false);
        if (image != null) {
            return createMaskFromImage(image, onlyTransparency);
        } else {
            // no pixel data to convert => fallback to a white mask
            assert layer instanceof AdjustmentLayer;
            return REVEAL_ALL.createMaskImage(layer, null);
        }
    }

    // creates a grayscale version of the given image
    private static BufferedImage createMaskFromImage(BufferedImage image,
                                                     boolean onlyTransparency) {
        BufferedImage grayImage = new BufferedImage(
            image.getWidth(), image.getHeight(), TYPE_BYTE_GRAY);
        Graphics2D g = grayImage.createGraphics();

        if (onlyTransparency) {
            var transparencyToGrayFilter = new PointFilter("") {
                @Override
                public int processPixel(int x, int y, int rgb) {
                    // map alpha (0-255) to RGB grayscale (R=a, G=a, B=a)
                    int a = (rgb >>> 24) & 0xFF;
                    return 0xFF_00_00_00 | a << 16 | a << 8 | a;
                }
            };
            transparencyToGrayFilter.setProgressTracker(ProgressTracker.NULL_TRACKER);
            BufferedImage argbMaskImage = transparencyToGrayFilter.filter(image, null);
            g.drawImage(argbMaskImage, 0, 0, null);
        } else {
            // fill the background with white so that transparent parts become white
            Colors.fillWith(Color.WHITE, g, image.getWidth(), image.getHeight());
            g.drawImage(image, 0, 0, null);
        }
        g.dispose();
        return grayImage;
    }

    private final String displayName;
    private final boolean needsSelection;

    MaskInitMethod(String displayName, boolean needsSelection) {
        this.displayName = displayName;
        this.needsSelection = needsSelection;
    }

    abstract BufferedImage createMaskImage(Layer layer, Shape selShape);

    @Override
    public String toString() {
        return displayName;
    }

    public boolean needsSelection() {
        return needsSelection;
    }
}

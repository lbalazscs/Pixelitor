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

import java.awt.*;
import java.awt.image.BufferedImage;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;

/**
 * Ways to create a new layer mask
 */
public enum LayerMaskAddType {
    REVEAL_ALL("Reveal All", false) {
        @Override
        BufferedImage createBWImage(Layer layer, Shape selShape) {
            // a fully white image
            return createFilledImage(layer, Color.WHITE, null, null);
        }
    }, HIDE_ALL("Hide All", false) {
        @Override
        BufferedImage createBWImage(Layer layer, Shape selShape) {
            // a fully black image
            return createFilledImage(layer, Color.BLACK, null, null);
        }
    }, REVEAL_SELECTION("Reveal Selection", true) {
        @Override
        BufferedImage createBWImage(Layer layer, Shape selShape) {
            // back image, but the selection is white
            return createFilledImage(layer, Color.BLACK, Color.WHITE, selShape);
        }
    }, HIDE_SELECTION("Hide Selection", true) {
        @Override
        BufferedImage createBWImage(Layer layer, Shape selShape) {
            // white image, but the selection is black
            return createFilledImage(layer, Color.WHITE, Color.BLACK, selShape);
        }
    }, FROM_TRANSPARENCY("From Transparency", false) {
        @Override
        BufferedImage createBWImage(Layer layer, Shape selShape) {
            return createMaskFromLayer(layer, true);
        }
    }, FROM_LAYER("From Layer", false) {
        @Override
        BufferedImage createBWImage(Layer layer, Shape selShape) {
            return createMaskFromLayer(layer, false);
        }
    }, PATTERN("Pattern", false) { // only for debugging

        @Override
        BufferedImage createBWImage(Layer layer, Shape selShape) {
            Canvas canvas = layer.getComp().getCanvas();
            BufferedImage bi = createFilledImage(layer, Color.WHITE, null, null);
            Graphics2D g = bi.createGraphics();
            int width = canvas.getWidth();
            int height = canvas.getHeight();
            float cx = width / 2.0f;
            float cy = height / 2.0f;
            float radius = Math.min(cx, cy);
            float[] fractions = {0.5f, 1.0f};
            Paint gradient = new RadialGradientPaint(cx, cy, radius, fractions,
                new Color[]{Color.WHITE, Color.BLACK});
            g.setPaint(gradient);
            g.fillRect(0, 0, width, height);
            g.dispose();
            return bi;
        }
    };

    // Returns a canvas-sized image filled with the background color.
    // If a foreground color is given, then fills the shape with it.
    private static BufferedImage createFilledImage(Layer layer, Color bg, Color fg, Shape selShape) {
        Canvas canvas = layer.getComp().getCanvas();
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        BufferedImage bwImage = new BufferedImage(width, height, TYPE_BYTE_GRAY);
        Graphics2D g = bwImage.createGraphics();

        // fill background
        Colors.fillWith(bg, g, width, height);

        // fill foreground
        if (fg != null) {
            g.setColor(fg);
            if (selShape != null) {
                g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
                g.fill(selShape);
            } else {
                throw new IllegalArgumentException("fg fill is given, but no shape");
            }
        }
        g.dispose();
        return bwImage;
    }

    // Returns a canvas-size grayscale image representing
    // the visible contents of the layer
    private static BufferedImage createMaskFromLayer(Layer layer,
                                                     boolean onlyTransparency) {
        BufferedImage image = layer.toImage(false, false);
        if (image != null) {
            return createMaskFromImage(image, onlyTransparency);
        } else {
            // adjustment layer or smart filter, there is nothing better
            assert layer instanceof AdjustmentLayer;
            return REVEAL_ALL.createBWImage(layer, null);
        }
    }

    // creates a grayscale version of the given image
    private static BufferedImage createMaskFromImage(BufferedImage image,
                                                     boolean onlyTransparency) {
        BufferedImage bwImage = new BufferedImage(
            image.getWidth(), image.getHeight(), TYPE_BYTE_GRAY);
        Graphics2D g = bwImage.createGraphics();

        if (onlyTransparency) {
            PointFilter transparencyToBWFilter = new PointFilter("") {
                @Override
                public int processPixel(int x, int y, int rgb) {
                    int a = (rgb >>> 24) & 0xFF;
                    return 0xFF_00_00_00 | a << 16 | a << 8 | a;
                }
            };
            transparencyToBWFilter.setProgressTracker(ProgressTracker.NULL_TRACKER);
            BufferedImage argbBWImage = transparencyToBWFilter.filter(image, null);
            g.drawImage(argbBWImage, 0, 0, null);
        } else {
            // fill the background with white so that transparent parts become white
            Colors.fillWith(Color.WHITE, g, image.getWidth(), image.getHeight());
            g.drawImage(image, 0, 0, null);
        }
        g.dispose();
        return bwImage;
    }

    private final String displayName;
    private final boolean needsSelection;

    LayerMaskAddType(String guiName, boolean needsSelection) {
        this.displayName = guiName;
        this.needsSelection = needsSelection;
    }

    abstract BufferedImage createBWImage(Layer layer, Shape selShape);

    @Override
    public String toString() {
        return displayName;
    }

    public boolean needsSelection() {
        return needsSelection;
    }
}

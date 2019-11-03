/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Canvas;
import pixelitor.selection.Selection;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
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
        BufferedImage getBWImage(Layer layer, Canvas canvas, Selection selection) {
            // a fully white image
            return createFilledImage(canvas, Color.WHITE, null, null);
        }
    }, HIDE_ALL("Hide All", false) {
        @Override
        BufferedImage getBWImage(Layer layer, Canvas canvas, Selection selection) {
            // a fully black image
            return createFilledImage(canvas, Color.BLACK, null, null);
        }
    }, REVEAL_SELECTION("Reveal Selection", true) {
        @Override
        BufferedImage getBWImage(Layer layer, Canvas canvas, Selection selection) {
            // back image, but the selection is white
            return createFilledImage(canvas, Color.BLACK, Color.WHITE, selection.getShape());
        }
    }, HIDE_SELECTION("Hide Selection", true) {
        @Override
        BufferedImage getBWImage(Layer layer, Canvas canvas, Selection selection) {
            // white image, but the selection is black
            return createFilledImage(canvas, Color.WHITE, Color.BLACK, selection.getShape());
        }
    }, FROM_TRANSPARENCY("From Transparency", false) {
        @Override
        BufferedImage getBWImage(Layer layer, Canvas canvas, Selection selection) {
            return createMaskFromLayer(layer, true, canvas);
        }
    }, FROM_LAYER("From Layer", false) {
        @Override
        BufferedImage getBWImage(Layer layer, Canvas canvas, Selection selection) {
            return createMaskFromLayer(layer, false, canvas);
        }
    }, PATTERN ("Pattern", false) { // only for debugging

        @Override
        BufferedImage getBWImage(Layer layer, Canvas canvas, Selection selection) {
            BufferedImage bi = createFilledImage(canvas, Color.WHITE, null, null);
            Graphics2D g = bi.createGraphics();
            int width = canvas.getImWidth();
            int height = canvas.getImHeight();
            float cx = width / 2.0f;
            float cy = height / 2.0f;
            float radius = Math.min(cx, cy);
            float[] fractions = {0.5f, 1.0f};
            Paint gradient = new RadialGradientPaint(cx, cy, radius, fractions, new Color[]{
                    Color.WHITE, Color.BLACK
            });
            g.setPaint(gradient);
            g.fillRect(0, 0, width, height);
            g.dispose();
            return bi;
        }
    };

    private static BufferedImage createFilledImage(Canvas canvas, Color bg, Color fg, Shape shape) {
        int width = canvas.getImWidth();
        int height = canvas.getImHeight();
        BufferedImage bwImage = new BufferedImage(width, height, TYPE_BYTE_GRAY);
        Graphics2D g = bwImage.createGraphics();

        // fill background
        g.setColor(bg);
        g.fillRect(0, 0, width, height);

        // fill foreground
        if(fg != null) {
            g.setColor(fg);
            if (shape != null) {
                g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
                g.fill(shape);
            } else {
                g.fillRect(0, 0, width, height);
            }
        }
        g.dispose();
        return bwImage;
    }

    private static BufferedImage createMaskFromLayer(Layer layer,
                                                     boolean onlyTransparency,
                                                     Canvas canvas) {
        if (layer instanceof ImageLayer) {
            ImageLayer imageLayer = (ImageLayer) layer;
            BufferedImage image = imageLayer.getCanvasSizedSubImage();
            return createMaskFromImage(image, onlyTransparency, canvas);
        } else if (layer instanceof TextLayer) {
            TextLayer textLayer = (TextLayer) layer;
            // the rasterized image is canvas-sized, exactly as we want it
            BufferedImage rasterizedImage = textLayer.createRasterizedImage();
            return createMaskFromImage(rasterizedImage, onlyTransparency, canvas);
        } else {
            // there is nothing better
            return REVEAL_ALL.getBWImage(layer, canvas, null);
        }
    }

    private static BufferedImage createMaskFromImage(BufferedImage image,
                                                     boolean onlyTransparency,
                                                     Canvas canvas) {
        int width = canvas.getImWidth();
        int height = canvas.getImHeight();

        assert width == image.getWidth();
        assert height == image.getHeight();

        BufferedImage bwImage = new BufferedImage(width, height, TYPE_BYTE_GRAY);
        Graphics2D g = bwImage.createGraphics();

        // fill the background with white so that transparent parts become white
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        if (onlyTransparency) {
            // with DstOut only the source alpha will matter
            g.setComposite(AlphaComposite.DstOut);
        }

        g.drawImage(image, 0, 0, null);
        g.dispose();
        return bwImage;
    }

    private final String guiName;
    private final boolean needsSelection;

    LayerMaskAddType(String guiName, boolean needsSelection) {
        this.guiName = guiName;
        this.needsSelection = needsSelection;
    }

    abstract BufferedImage getBWImage(Layer layer, Canvas canvas, Selection selection);

    @Override
    public String toString() {
        return guiName;
    }

    /**
     * Returns true if the action needs selection and there is no selection.
     */
    public boolean missingSelection(Selection selection) {
        if(needsSelection) {
            return selection == null;
        } else {
            return false;
        }
    }

    public boolean needsSelection() {
        return needsSelection;
    }
}

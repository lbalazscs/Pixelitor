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
import pixelitor.progress.ProgressTracker;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * Static utility methods related to brush images.
 */
public class BrushImageUtils {
    private BrushImageUtils() {
    }

    public static BufferedImage createGrayRandomPointsBrushImage(int diameter, float density) {
        if (density < 0.0f || density > 1.0f) {
            throw new IllegalArgumentException("density is " + density);
        }

        BufferedImage brushImage = new BufferedImage(diameter, diameter, TYPE_BYTE_GRAY);

        int radius = diameter / 2;
        int radiusSq = radius * radius;
        Random random = new Random();

        byte[] pixels = ImageUtils.getGrayPixels(brushImage);
        for (int x = 0; x < diameter; x++) {
            for (int y = 0; y < diameter; y++) {
                int dx = x - radius;
                int dy = y - radius;
                int centerDistSq = dx * dx + dy * dy;
                int index = x + y * diameter;
                if (centerDistSq < radiusSq) {
                    float rn = random.nextFloat();
                    if (density > rn) {
                        pixels[index] = (byte) random.nextInt(256);
                    } else {
                        // (byte) 255 is -1 in signed representation, but the ComponentColorModel
                        // for grayscale treats it as unsigned, ensuring that it is interpreted as white
                        pixels[index] = (byte) 255;  // white
                    }
                } else {
                    pixels[index] = (byte) 255; // white
                }
            }
        }

        return brushImage;
    }

    public static BufferedImage createSoftGrayBrushImage(int size) {
        BufferedImage brushImage = new BufferedImage(size, size, TYPE_BYTE_GRAY);
        Graphics2D g = brushImage.createGraphics();

        // fill a black circle over the white background
        Colors.fillWith(WHITE, g, size, size);
        g.setColor(BLACK);
        int softness = size / 4;
        g.fillOval(softness, softness, size - 2 * softness, size - 2 * softness);
        g.dispose();

        // blur it
        var blur = new BoxBlurFilter("Blur", softness, softness, 1);
        blur.setProgressTracker(ProgressTracker.NO_OP_TRACKER);
        brushImage = blur.filter(brushImage, brushImage);

        return brushImage;
    }

    public static BufferedImage createSoftTransparencyImage(int size) {
        BufferedImage mask = createSoftGrayBrushImage(size);

        // the color doesn't matter, as only the alpha channel will be used
        return maskToTransparency(mask, 0);
    }

    /**
     * Converts a grayscale mask image into an ARGB image with the
     * corresponding alpha channel. The color argument must have 0 alpha.
     */
    public static BufferedImage maskToTransparency(BufferedImage mask, int colorNoAlpha) {
        BufferedImage dest = new BufferedImage(mask.getWidth(), mask.getHeight(), TYPE_INT_ARGB);
        byte[] srcPixels = ImageUtils.getGrayPixels(mask);
        int[] destPixels = ImageUtils.getPixels(dest);

        for (int i = 0; i < destPixels.length; i++) {
            int srcGray = srcPixels[i] & 0xFF; // convert signed byte to unsigned int

            // the color comes from the given color, the alpha from the
            // mask's gray value (white => transparent, black => opaque)
            int alpha = 0xFF - srcGray;
            destPixels[i] = alpha << 24 | colorNoAlpha;
        }
        return dest;
    }
}

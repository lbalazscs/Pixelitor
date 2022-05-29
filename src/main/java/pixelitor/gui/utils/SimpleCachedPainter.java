/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.gui.utils;

import javax.swing.*;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Transparency;
import java.awt.image.VolatileImage;
import java.lang.ref.SoftReference;

/**
 * Utility class that can be used to draw the same graphics at different times.
 * It manages the hardware-accelerated volatile image,
 * subclasses need to implement only the painting.
 *
 * It is "simple" in the sense that it manages a single cached image.
 * More sophisticated cached painters are in the sun.* packages.
 */
public abstract class SimpleCachedPainter implements Painter<Object> {
    private final int transparency;
    private SoftReference<VolatileImage> cache;

    /**
     * The transparency parameter must correspond to
     * the constants defined in {@link Transparency}
     */
    protected SimpleCachedPainter(int transparency) {
        this.transparency = transparency;
    }

    /**
     * Subclasses must implement this to do the actual painting
     */
    public abstract void doPaint(Graphics2D g, int width, int height);

    @Override
    public void paint(Graphics2D g, Object o, int width, int height) {
        GraphicsConfiguration gc = g.getDeviceConfiguration();
        if (cache == null) { // called for the first time or was invalidated
            createAndUseCachedImage(g, gc, width, height);
            return;
        }

        VolatileImage vi = cache.get();
        if (vi == null) { // soft reference collected
            createAndUseCachedImage(g, gc, width, height);
            return;
        }

        if (vi.getWidth() != width || vi.getHeight() != height) { // size changed
            vi.flush();
            createAndUseCachedImage(g, gc, width, height);
            return;
        }
        // at this point we have a cached image with the right size

        int safetyCounter = 0; // to be 100% sure that this is not an infinite loop
        do {
            int valCode = vi.validate(gc); // check before rendering
            if (valCode == VolatileImage.IMAGE_OK) {
                // simplest case, just use the image
                g.drawImage(vi, 0, 0, null);
            } else if (valCode == VolatileImage.IMAGE_RESTORED) {
                // memory loss, but the image object can be reused
                renderAndUseCachedImage(vi, g, width, height);
            } else if (valCode == VolatileImage.IMAGE_INCOMPATIBLE) {
                // surface incompatibility: the image has to be recreated
                vi.flush();
                vi = createAndUseCachedImage(g, gc, width, height);
            }
        } while (vi.contentsLost() && safetyCounter++ < 3); // check after rendering
    }

    protected VolatileImage createAndUseCachedImage(Graphics2D g,
                                                    GraphicsConfiguration gc,
                                                    int width, int height) {
        VolatileImage img = gc.createCompatibleVolatileImage(width, height, transparency);
        renderAndUseCachedImage(img, g, width, height);

        return img;
    }

    protected void renderAndUseCachedImage(VolatileImage img, Graphics2D g,
                                           int width, int height) {
        // render on the image
        Graphics2D imgGraphics = img.createGraphics();
        if (transparency != Transparency.OPAQUE) {
            clearBackground(imgGraphics, width, height);
        }
        doPaint(imgGraphics, width, height);
        imgGraphics.dispose();

        // use it for drawing
        g.drawImage(img, 0, 0, null);

        // store it for future use
        cache = new SoftReference<>(img);
    }

    private static void clearBackground(Graphics2D g, int width, int height) {
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, width, height);
        g.setComposite(AlphaComposite.SrcOver);
    }

    protected void invalidateCache() {
        if (cache != null) {
            VolatileImage image = cache.get();
            if (image != null) {
                image.flush();
            }
            cache = null;
        }
    }
}

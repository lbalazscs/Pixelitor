/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.colors.Colors;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * Utility to draw on images.
 */
public class Drawer {
    private final BufferedImage image;
    private boolean useAA;
    private Color fillColor;

    private Drawer(BufferedImage image) {
        this.image = image;
    }

    public static Drawer on(BufferedImage image) {
        return new Drawer(image);
    }

    public Drawer useAA() {
        useAA = true;
        return this;
    }

    public Drawer fillWith(Color c) {
        fillColor = c;
        return this;
    }

    public void draw(Consumer<Graphics2D> task) {
        Graphics2D g2 = image.createGraphics();

        if (fillColor != null) {
            Colors.fillWith(fillColor, g2, image.getWidth(), image.getHeight());
        }
        if (useAA) {
            g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        }

        task.accept(g2);

        g2.dispose();
    }
}

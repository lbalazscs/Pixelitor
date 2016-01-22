/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import javax.swing.border.AbstractBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;

/**
 * A border that has a gradient fill.
 */
public class GradientBorder extends AbstractBorder {
    private static final int WIDTH = 4;

    private final Color leftColor;
    private final Color rightColor;
    private Insets insets;

    public GradientBorder(Color leftColor, Color rightColor) {
        this.leftColor = leftColor;
        this.rightColor = rightColor;
    }

    @Override
    public Insets getBorderInsets(Component c) {
        //noinspection SuspiciousNameCombination
        return new Insets(WIDTH, WIDTH, WIDTH, WIDTH);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public Insets getBorderInsets(Component c, Insets i) {
        i.left = WIDTH;
        i.right = WIDTH;
        i.top = WIDTH;
        i.bottom = WIDTH;

        return i; // reuse the same object for efficiency
    }

    @Override
    public boolean isBorderOpaque() {
        return true;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2d = (Graphics2D) g;
        Paint paint = new GradientPaint(x, y, leftColor, width, 0, rightColor);

        if (insets == null) {
            insets = getBorderInsets(c);
        } else {
            insets = getBorderInsets(c, insets);
        }

        g2d.setPaint(paint);

        // Draw rectangles around the component, but do not draw
        // in the component area itself.
        g2d.fillRect(x, y, width, insets.top);
        g2d.fillRect(x, y, insets.left, height);
        g2d.fillRect(x + width - insets.right, y, insets.right, height);
        g2d.fillRect(x, y + height - insets.bottom, width, insets.bottom);
    }
}
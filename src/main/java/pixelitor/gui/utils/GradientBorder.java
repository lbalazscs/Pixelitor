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

package pixelitor.gui.utils;

import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import java.awt.*;

/**
 * A {@link Border} implementation that draws a gradient-filled border.
 */
public class GradientBorder extends AbstractBorder {
    private static final int THICKNESS = 4;

    private final Color leftColor;
    private final Color rightColor;

    public GradientBorder(Color leftColor, Color rightColor) {
        assert leftColor.getAlpha() == 255 && rightColor.getAlpha() == 255;

        this.leftColor = leftColor;
        this.rightColor = rightColor;
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(THICKNESS, THICKNESS, THICKNESS, THICKNESS);
        return insets;
    }

    @Override
    public boolean isBorderOpaque() {
        return true;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g;
        Paint prevPaint = g2.getPaint();
        g2.setPaint(new GradientPaint(x, y, leftColor, x + width, y, rightColor));

        // draw rectangles to create the border, leaving the component's area untouched
        int sideHeight = height - 2 * THICKNESS;
        g2.fillRect(x, y, width, THICKNESS); // top
        g2.fillRect(x, y + height - THICKNESS, width, THICKNESS); // bottom
        g2.fillRect(x, y + THICKNESS, THICKNESS, sideHeight); // left
        g2.fillRect(x + width - THICKNESS, y + THICKNESS, THICKNESS, sideHeight); // right

        g2.setPaint(prevPaint);
    }
}

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
import java.awt.*;

import static java.awt.RenderingHints.*;

/**
 * An abstract superclass for vector icons which look good
 * on HiDPI screens with any scaling
 */
public abstract class VectorIcon implements Icon, Cloneable {
    protected Color color;
    private final int width;
    private final int height;

    private static final Color LIGHT_BG = new Color(214, 217, 223);
    private static final Color DARK_BG = new Color(42, 42, 42);
    private static final Color LIGHT_FG = new Color(19, 30, 43);

    protected VectorIcon(Color color, int width, int height) {
        this.color = color;
        this.width = width;
        this.height = height;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g;

        g2.translate(x, y);
        g2.setColor(color);
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(KEY_STROKE_CONTROL, VALUE_STROKE_PURE);

        paintIcon(g2);
    }

    protected abstract void paintIcon(Graphics2D g);

    @Override
    public final int getIconWidth() {
        return width;
    }

    @Override
    public final int getIconHeight() {
        return height;
    }

    public VectorIcon copy(Color color) {
        VectorIcon copy = clone();
        copy.color = color;
        return copy;
    }

    @Override
    public VectorIcon clone() {
        try {
            return (VectorIcon) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public static VectorIcon createNonTransparentThemed(Shape shape) {
        return new VectorIcon(Color.WHITE, 24, 24) {
            @Override
            protected void paintIcon(Graphics2D g) {
                boolean darkTheme = Themes.getCurrent().isDark();
                g.setColor(darkTheme ? DARK_BG : LIGHT_BG);
                g.fillRect(0, 0, 24, 24);

                g.setColor(darkTheme ? Themes.LIGHT_ICON_COLOR : LIGHT_FG);

                g.fill(shape);
            }
        };
    }
}

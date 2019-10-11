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

package pixelitor.utils;

import java.awt.Font;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory for Unicode-character-based shapes
 */
public class UnicodeShapes {
    private static final Font font;
    private static final FontRenderContext frc;
    private static final LineMetrics lineMetrics;

    static {
        font = new Font(Font.SANS_SERIF, Font.PLAIN, 100);
        frc = new FontRenderContext(new AffineTransform(), true, true);
        lineMetrics = font.getLineMetrics("a", frc);
    }

    private static final Map<String, Shape> unscaledCache = new HashMap<>();

    private UnicodeShapes() {
        // do not instantiate
    }

    /**
     * Extracts the shape from a string (consisting typically of
     * one special unicode character) and scales and translates
     * it to the given size and position.
     */
    public static Shape extract(String s, double x, double y, double width, double height) {
        Shape unscaled = unscaledCache.computeIfAbsent(s, s1 -> {
            GlyphVector glyphs = font.createGlyphVector(frc, s1);
            return glyphs.getOutline(0, lineMetrics.getAscent());
        });

        AffineTransform at = AffineTransform.getTranslateInstance(x, y);
        Rectangle2D stringBounds = font.getStringBounds(s, frc);
        double sx = width / stringBounds.getWidth();
        double sy = height / stringBounds.getHeight();
        at.scale(sx, sy);
        return at.createTransformedShape(unscaled);
    }

    public static Shape extract(int codePoint, double x, double y, double width, double height) {
        String s = new String(Character.toChars(codePoint));
        return extract(s, x, y, width, height);
    }

    // works incorrectly on Linux, returns true for undisplayed characters
    public static boolean isSupported(int codePoint) {
        boolean ok = font.canDisplay(codePoint);
//        System.out.printf("UnicodeShapes::isSupported: codePoint = 0x%x, ok = %s%n",
//                codePoint, ok);
        return ok;
    }
}

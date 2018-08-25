/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.util;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;

public class DragDisplay {
    private static final AlphaComposite BG_COMPOSITE = AlphaComposite.SrcOver.derive(0.65f);
    private static final BasicStroke BG_STROKE = new BasicStroke(1.0f);
    private static final int BG_RECT_RADIUS = 20;
    private static final int BG_TEXT_HOR_DIST = 8;
    private static final int BG_TEXT_VER_DIST = 7;

    public static final int BG_WIDTH = 84;
    public static final int ONE_LINER_BG_HEIGHT = 22;
    public static final int TWO_LINER_BG_HEIGHT = 47;
    private final Graphics2D g;
    private final Composite origComposite;
    private final Stroke origStroke;
    private final Shape origClip;

    public DragDisplay(Graphics2D g) {
        this.g = g;
        origComposite = g.getComposite();
        origStroke = g.getStroke();
        origClip = g.getClip();

        g.setStroke(BG_STROKE);
        g.setClip(null);
    }

    private void drawBg(int x, int y, int height) {
        g.setComposite(BG_COMPOSITE);
        g.setColor(Color.BLACK);
        g.fillRoundRect(x, y - height, BG_WIDTH, height, BG_RECT_RADIUS, BG_RECT_RADIUS);
        g.setColor(Color.WHITE);
        g.drawRoundRect(x, y - height, BG_WIDTH, height, BG_RECT_RADIUS, BG_RECT_RADIUS);
        g.setComposite(origComposite);
    }

    /**
     * x and y are the bottom left coordinates of the background rectangle
     */
    public void drawOneLine(String s, int x, int y) {
        drawBg(x, y, ONE_LINER_BG_HEIGHT);
        g.drawString(s, x + BG_TEXT_HOR_DIST, y - BG_TEXT_VER_DIST);
    }

    public void drawTwoLines(String s1, String s2, int x, int y) {
        drawBg(x, y, TWO_LINER_BG_HEIGHT);
        g.drawString(s1, x + BG_TEXT_HOR_DIST, y - 23 - BG_TEXT_VER_DIST);
        g.drawString(s2, x + BG_TEXT_HOR_DIST, y - BG_TEXT_VER_DIST);
    }

    public void finish() {
        g.setStroke(origStroke);
        g.setClip(origClip);
    }
}

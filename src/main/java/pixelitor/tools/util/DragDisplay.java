/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.utils.ImageUtils;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Support for drawing drag information (pixels, angles) in the tools.
 * All coordinates are in component space, unless they have the im prefix in their name.
 */
public class DragDisplay {
    private static final AlphaComposite BG_COMPOSITE = AlphaComposite.SrcOver.derive(0.65f);
    private static final BasicStroke BG_STROKE = new BasicStroke(1.0f);
    private static final int BG_RECT_RADIUS = 20;
    private static final int BG_TEXT_HOR_DIST = 8;
    private static final int BG_TEXT_VER_DIST = 7;

    public static final int BG_WIDTH_PIXEL = 84;
    public static final int BG_WIDTH_ANGLE = 70; // enough for an angle
    public static final int ONE_LINER_BG_HEIGHT = 22;
    public static final int TWO_LINER_BG_HEIGHT = 47;
    public static final int MOUSE_DISPLAY_DISTANCE = 10;
    private final Graphics2D g;
    private final Composite origComposite;
    private final Stroke origStroke;
    private final Shape origClip;
    private final int bgWidth;

    public DragDisplay(Graphics2D g, int bgWidth) {
        this.g = g;
        origComposite = g.getComposite();
        origStroke = g.getStroke();
        origClip = g.getClip();
        this.bgWidth = bgWidth;

        g.setStroke(BG_STROKE);
        g.setClip(null);
    }

    public static String getHeightDisplayString(double imHeight) {
        return "\u2195 = " + Math.abs((int) imHeight) + " px";
    }

    public static String getWidthDisplayString(double imWidth) {
        return "\u2194 = " + Math.abs((int) imWidth) + " px";
    }

    public static void displayRelativeMovement(Graphics2D g, int imDx, int imDy,
                                               float x, float y) {
        String dxString;
        if (imDx >= 0) {
            dxString = "\u2192 = " + imDx + " px";
        } else {
            dxString = "\u2190 = " + (-imDx) + " px";
        }
        String dyString;
        if (imDy >= 0) {
            dyString = "\u2193 = " + imDy + " px";
        } else {
            dyString = "\u2191 = " + (-imDy) + " px";
        }

        DragDisplay dd = new DragDisplay(g, BG_WIDTH_PIXEL);

        dd.drawTwoLines(dxString, dyString, x, y);

        dd.finish();
    }

    private void drawBg(float x, float y, int height) {
        g.setComposite(BG_COMPOSITE);
        g.setColor(Color.BLACK);
        RoundRectangle2D rect = new RoundRectangle2D.Float(
            x, y - height, bgWidth, height, BG_RECT_RADIUS, BG_RECT_RADIUS);
        g.fill(rect);
        g.setColor(Color.WHITE);
        g.draw(rect);
        g.setComposite(origComposite);
    }

    /**
     * x and y are the bottom left coordinates of the background rectangle.
     */
    public void drawOneLine(String s, float x, float y) {
        drawBg(x, y, ONE_LINER_BG_HEIGHT);
        g.drawString(s, x + BG_TEXT_HOR_DIST, y - BG_TEXT_VER_DIST);
    }

    public void drawTwoLines(String s1, String s2, float x, float y) {
        drawBg(x, y, TWO_LINER_BG_HEIGHT);
        g.drawString(s1, x + BG_TEXT_HOR_DIST, y - 23 - BG_TEXT_VER_DIST);
        g.drawString(s2, x + BG_TEXT_HOR_DIST, y - BG_TEXT_VER_DIST);
    }

    public void finish() {
        g.setStroke(origStroke);
        g.setClip(origClip);
    }

    /**
     * The first time {@link DragDisplay} is used, it initializes
     * some fonts (sun.font.CompositeFont.doDeferredInitialisation),
     * and it is better to do this before the GUI starts,
     * otherwise it would block the EDT
     */
    public static void initializeFont() {
        BufferedImage tmp = ImageUtils.createSysCompatibleImage(10, 10);
        Graphics2D g2 = tmp.createGraphics();
        DragDisplay dd = new DragDisplay(g2, BG_WIDTH_PIXEL);

        dd.drawOneLine("x", 0, 10);

        g2.dispose();
        tmp.flush();
    }
}

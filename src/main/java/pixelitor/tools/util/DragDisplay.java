/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
 * The rendering of drag-related information (distances, angles)
 * in tools. All coordinates are in component space unless
 * they have the 'im' prefix (image space) in their name.
 */
public class DragDisplay {
    private static final AlphaComposite BG_COMPOSITE = AlphaComposite.SrcOver.derive(0.65f);
    private static final BasicStroke BG_BORDER_STROKE = new BasicStroke(1.0f);
    private static final int BG_CORNER_RADIUS = 20;
    private static final int TEXT_HOR_PADDING = 8;
    private static final int TEXT_VER_PADDING = 7;

    public static final int BG_WIDTH_PIXELS = 84;
    public static final int BG_WIDTH_ANGLES = 70; // enough for an angle
    public static final int SINGLE_LINE_HEIGHT = 22;
    public static final int DOUBLE_LINE_HEIGHT = 47;
    public static final int OFFSET_FROM_MOUSE = 10;
    
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

        g.setStroke(BG_BORDER_STROKE);
        g.setClip(null);
    }

    /**
     * Generates a display string for vertical height.
     */
    public static String formatHeightString(double imHeight) {
        return "↕ = " + Math.abs((int) imHeight) + " px";
    }

    /**
     * Generates a display string for horizontal width.
     */
    public static String formatWidthString(double imWidth) {
        return "↔ = " + Math.abs((int) imWidth) + " px";
    }

    /**
     * Displays the relative movement information with directional arrows.
     */
    public static void displayRelativeMovement(Graphics2D g,
                                               int imDx, int imDy,
                                               float x, float y) {
        String horMovement = (imDx >= 0)
            ? "→ = " + imDx + " px"
            : "← = " + (-imDx) + " px";

        String verMovement = (imDy >= 0)
            ? "↓ = " + imDy + " px"
            : "↑ = " + (-imDy) + " px";

        DragDisplay dd = new DragDisplay(g, BG_WIDTH_PIXELS);
        dd.drawTwoLines(horMovement, verMovement, x, y);
        dd.cleanup();
    }

    /**
     * Draws the semi-transparent background rectangle with border.
     *
     * @param x      Left coordinate of the background
     * @param y      Bottom coordinate of the background
     * @param height Height of the background rectangle
     */
    private void drawBackground(float x, float y, int height) {
        g.setComposite(BG_COMPOSITE);
        g.setColor(Color.BLACK);
        RoundRectangle2D rect = new RoundRectangle2D.Float(
            x, y - height, bgWidth, height, BG_CORNER_RADIUS, BG_CORNER_RADIUS);
        g.fill(rect);
        g.setColor(Color.WHITE);
        g.draw(rect);
        g.setComposite(origComposite);
    }

    /**
     * Draws a single-line message at the given position.
     *
     * @param text Text to display
     * @param x Left coordinate of the background
     * @param y Bottom coordinate of the background
     */
    public void drawOneLine(String text, float x, float y) {
        drawBackground(x, y, SINGLE_LINE_HEIGHT);
        g.drawString(text, x + TEXT_HOR_PADDING, y - TEXT_VER_PADDING);
    }

    /**
     * Draws a two-line message at the given position.
     *
     * @param line1 First line of text
     * @param line2 Second line of text
     * @param x     Left coordinate of the background
     * @param y     Bottom coordinate of the background
     */
    public void drawTwoLines(String line1, String line2, float x, float y) {
        drawBackground(x, y, DOUBLE_LINE_HEIGHT);
        g.drawString(line1, x + TEXT_HOR_PADDING, y - 23 - TEXT_VER_PADDING);
        g.drawString(line2, x + TEXT_HOR_PADDING, y - TEXT_VER_PADDING);
    }

    public void cleanup() {
        g.setStroke(origStroke);
        g.setClip(origClip);
    }

    /**
     * The first time {@link DragDisplay} is used, it triggers font
     * initialization (sun.font.CompositeFont.doDeferredInitialisation).
     * This method should be called before the GUI starts
     * to prevent blocking the EDT.
     */
    public static void initializeFont() {
        BufferedImage tmp = ImageUtils.createSysCompatibleImage(10, 10);
        Graphics2D g2 = tmp.createGraphics();
        DragDisplay dd = new DragDisplay(g2, BG_WIDTH_PIXELS);

        dd.drawOneLine("x", 0, 10);

        g2.dispose();
        tmp.flush();
    }
}

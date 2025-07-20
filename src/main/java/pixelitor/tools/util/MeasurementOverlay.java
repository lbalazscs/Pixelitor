/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * The rendering of drag-related information (distances, angles)
 * in tools. All coordinates are in component space unless
 * they have the 'im' prefix (image space) in their name.
 */
public class MeasurementOverlay {
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

    public MeasurementOverlay(Graphics2D g, int bgWidth) {
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
                                               Point2D pos) {
        String horMovement = (imDx >= 0)
            ? "→ = " + imDx + " px"
            : "← = " + (-imDx) + " px";

        String verMovement = (imDy >= 0)
            ? "↓ = " + imDy + " px"
            : "↑ = " + (-imDy) + " px";

        MeasurementOverlay overlay = new MeasurementOverlay(g, BG_WIDTH_PIXELS);
        overlay.drawTwoLines(horMovement, verMovement, pos);
        overlay.cleanup();
    }

    /**
     * Draws the semi-transparent background rectangle with border.
     *
     * @param pos    Bottom-left coordinate of the background
     * @param height Height of the background rectangle
     */
    private void drawBackground(Point2D pos, int height) {
        g.setComposite(BG_COMPOSITE);
        g.setColor(Color.BLACK);
        RoundRectangle2D rect = new RoundRectangle2D.Double(
            pos.getX(), pos.getY() - height, bgWidth, height, BG_CORNER_RADIUS, BG_CORNER_RADIUS);
        g.fill(rect);
        g.setColor(Color.WHITE);
        g.draw(rect);
        g.setComposite(origComposite);
    }

    /**
     * Draws a single-line message at the given position.
     *
     * @param text Text to display
     * @param pos    Bottom-left coordinate of the background
     */
    public void drawOneLine(String text, Point2D pos) {
        drawBackground(pos, SINGLE_LINE_HEIGHT);
        float drawX = (float) (pos.getX() + TEXT_HOR_PADDING);
        float drawY = (float) (pos.getY() - TEXT_VER_PADDING);
        g.drawString(text, drawX, drawY);
    }

    /**
     * Draws a two-line message at the given position.
     *
     * @param line1 First line of text
     * @param line2 Second line of text
     * @param x     Left coordinate of the background
     * @param y     Bottom coordinate of the background
     */
    public void drawTwoLines(String line1, String line2, Point2D pos) {
        drawBackground(pos, DOUBLE_LINE_HEIGHT);

        float drawX = (float) (pos.getX() + TEXT_HOR_PADDING);
        float drawYBottom = (float) (pos.getY() - TEXT_VER_PADDING);
        float drawYTop = drawYBottom - 23.0f;

        g.drawString(line1, drawX, drawYTop);
        g.drawString(line2, drawX, drawYBottom);
    }

    public void cleanup() {
        g.setStroke(origStroke);
        g.setClip(origClip);
    }

    /**
     * The first time {@link MeasurementOverlay} is used, it triggers font
     * initialization (sun.font.CompositeFont.doDeferredInitialisation).
     * This method should be called before the GUI starts
     * to prevent blocking the EDT.
     */
    public static void initializeFont() {
        BufferedImage tmp = ImageUtils.createSysCompatibleImage(10, 10);
        Graphics2D g2 = tmp.createGraphics();
        MeasurementOverlay overlay = new MeasurementOverlay(g2, BG_WIDTH_PIXELS);

        overlay.drawOneLine("x", new Point2D.Double(0, 0));

        g2.dispose();
        tmp.flush();
    }
}

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

package pixelitor.filters.curves;

import pixelitor.colors.Colors;
import pixelitor.filters.util.Channel;
import pixelitor.filters.util.ColorSpace;
import pixelitor.gui.utils.Themes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.EnumMap;
import java.util.Map;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * Manages and renders tone curves for sRGB and Oklab color spaces.
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurves {
    public static final int CURVE_SIZE = 255;
    private static final int CURVE_PADDING = 10;
    private static final int AXIS_PADDING = 20;
    public static final int PANEL_SIZE = CURVE_SIZE + 2 * CURVE_PADDING + AXIS_PADDING;

    private static final int GRADIENT_BAR_THICKNESS = 10;
    private static final int GRID_DIVISIONS = 4;
    private static final BasicStroke GRID_STROKE = new BasicStroke(1);

    private final Map<Channel, ToneCurve> curves = new EnumMap<>(Channel.class);
    private ColorSpace activeColorSpace = ColorSpace.SRGB;
    private Channel activeChannel = activeColorSpace.getPrimaryChannel();

    public ToneCurves() {
        for (Channel channel : Channel.values()) {
            curves.put(channel, new ToneCurve(channel));
        }
    }

    public ToneCurve getCurve(Channel channel) {
        return curves.get(channel);
    }

    public ToneCurve getActiveCurve() {
        return curves.get(activeChannel);
    }

    /**
     * Sets the active channel.
     *
     * @return true if the active channel changed
     */
    public boolean setActiveChannel(Channel newActiveChannel) {
        if (newActiveChannel == null || newActiveChannel.getColorSpace() != activeColorSpace || activeChannel == newActiveChannel) {
            return false;
        }
        activeChannel = newActiveChannel;
        return true;
    }

    public Channel getActiveChannel() {
        return activeChannel;
    }

    public void setColorSpace(ColorSpace colorSpace) {
        if (this.activeColorSpace == colorSpace) {
            return;
        }
        this.activeColorSpace = colorSpace;
        this.activeChannel = colorSpace.getPrimaryChannel();
    }

    public ColorSpace getColorSpace() {
        return activeColorSpace;
    }

    /**
     * Resets the active color space and active channel to their default values (sRGB and RGB).
     */
    public void selectDefaults() {
        this.activeColorSpace = ColorSpace.SRGB;
        this.activeChannel = ColorSpace.SRGB.getPrimaryChannel();
    }

    /**
     * Resets every curve in every color space to the identity curve.
     */
    public void reset() {
        for (ToneCurve curve : curves.values()) {
            curve.reset();
        }
    }

    public void randomize() {
        for (Channel channel : activeColorSpace.getChannels()) {
            getCurve(channel).reset();
        }
        // randomize only the primary channel of the active color space
        getCurve(activeColorSpace.getPrimaryChannel()).randomize();
    }

    /**
     * Converts panel coordinates to normalized curve coordinates in the [0, 1] range.
     */
    public static Point2D.Float toCurveSpace(float panelX, float panelY) {
        float x = panelX - CURVE_PADDING - AXIS_PADDING;
        float y = panelY - CURVE_PADDING;

        // invert y-axis and normalize
        y = CURVE_SIZE - y;
        x /= CURVE_SIZE;
        y /= CURVE_SIZE;

        return new Point2D.Float(x, y);
    }

    /**
     * Draws the grid, scales, and all relevant curves.
     */
    public void draw(Graphics2D g) {
        boolean darkTheme = Themes.getActive().isDark();

        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        // clear background
        Colors.fillWith(darkTheme ? Color.BLACK : Color.WHITE, g, PANEL_SIZE, PANEL_SIZE);

        // apply padding and transform for a bottom-left origin
        var origTransform = g.getTransform();
        g.translate(CURVE_PADDING + AXIS_PADDING, CURVE_PADDING);
        g.translate(0, CURVE_SIZE);
        g.scale(1.0, -1.0);

        drawGrid(g);
        drawDiagonal(g);
        drawScales(g);
        drawCurves(g, darkTheme);

        g.setTransform(origTransform);
    }

    /**
     * Draws the grid lines in alternating light and dark colors.
     */
    private void drawGrid(Graphics2D g) {
        Path2D lightPath = new Path2D.Double();
        Path2D darkPath = new Path2D.Double();

        double gridCellSize = (double) CURVE_SIZE / GRID_DIVISIONS;

        for (int i = 0; i <= GRID_DIVISIONS; i++) {
            Path2D path = (i % 2 == 0) ? darkPath : lightPath;
            // horizontal line
            path.moveTo(0, i * gridCellSize);
            path.lineTo(CURVE_SIZE, i * gridCellSize);

            // vertical line
            path.moveTo(i * gridCellSize, 0);
            path.lineTo(i * gridCellSize, CURVE_SIZE);
        }

        g.setStroke(GRID_STROKE);

        g.setColor(Color.LIGHT_GRAY);
        g.draw(lightPath);

        g.setColor(Color.GRAY);
        g.draw(darkPath);
    }

    /**
     * Draws the diagonal reference line from bottom-left to top-right.
     */
    private void drawDiagonal(Graphics2D g) {
        g.setColor(Color.GRAY);
        g.setStroke(GRID_STROKE);
        g.drawLine(0, 0, CURVE_SIZE, CURVE_SIZE);
    }

    /**
     * Draws the gradient scales along the horizontal and vertical axes.
     */
    private void drawScales(Graphics2D g) {
        Color startColor = switch (activeChannel) {
            case OK_A, OK_B, RGB, OK_L -> activeChannel.getDarkColor();
            case RED, GREEN, BLUE -> Color.BLACK;
        };
        Color endColor = switch (activeChannel) {
            case OK_A, OK_B, RGB, OK_L -> activeChannel.getLightColor();
            case RED, GREEN, BLUE -> activeChannel.getDrawColor(true, false);
        };

        // horizontal gradient
        var horBar = new Rectangle2D.Float(0, -AXIS_PADDING, CURVE_SIZE, GRADIENT_BAR_THICKNESS);
        var horGradient = new GradientPaint(0, 0, startColor, CURVE_SIZE, 0, endColor);
        g.setPaint(horGradient);
        g.fill(horBar);
        g.setColor(Color.LIGHT_GRAY);
        g.draw(horBar);

        // vertical gradient
        var verBar = new Rectangle2D.Float(-AXIS_PADDING, 0, GRADIENT_BAR_THICKNESS, CURVE_SIZE);
        var verGradient = new GradientPaint(0, 0, startColor, 0, CURVE_SIZE, endColor);
        g.setPaint(verGradient);
        g.fill(verBar);
        g.setColor(Color.LIGHT_GRAY);
        g.draw(verBar);
    }

    /**
     * Draws the curve for each channel of the active color space,
     * with the active one on top.
     */
    private void drawCurves(Graphics2D g, boolean darkTheme) {
        // draw the inactive curves of the active color space first
        for (Channel channel : activeColorSpace.getChannels()) {
            if (channel != activeChannel) {
                getCurve(channel).draw(g, false, darkTheme);
            }
        }

        // then draw the active curve on top
        getActiveCurve().draw(g, true, darkTheme);
    }
}

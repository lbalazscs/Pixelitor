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
    private final Map<Channel, ToneCurve> srgbCurvesByChannel = new EnumMap<>(Channel.class);
    private final Map<Channel, ToneCurve> oklabCurvesByChannel = new EnumMap<>(Channel.class);
    private ColorSpace activeColorSpace = ColorSpace.SRGB;
    private Channel activeChannel;

    private int panelWidth = 295;
    private int panelHeight = 295;
    private int curveWidth = 255;
    private int curveHeight = 255;

    private static final int CURVE_PADDING = 10;
    private static final int AXIS_PADDING = 20;
    private static final int AXIS_SIZE = 10;
    private static final int GRID_DENSITY = 4;
    private static final BasicStroke GRID_STROKE = new BasicStroke(1);

    public ToneCurves() {
        // initialize curves for each channel
        for (Channel channel : Channel.getRGBChoices()) {
            srgbCurvesByChannel.put(channel, new ToneCurve(channel));
        }
        for (Channel channel : Channel.getLABChoices()) {
            oklabCurvesByChannel.put(channel, new ToneCurve(channel));
        }
        setActiveChannel(Channel.RGB);
    }

    private Map<Channel, ToneCurve> getActiveCurveMap() {
        return switch (activeColorSpace) {
            case SRGB -> srgbCurvesByChannel;
            case OKLAB -> oklabCurvesByChannel;
        };
    }

    public ToneCurve getCurve(Channel channel) {
        if (srgbCurvesByChannel.containsKey(channel)) {
            return srgbCurvesByChannel.get(channel);
        }
        return oklabCurvesByChannel.get(channel);
    }

    public ToneCurve getActiveCurve() {
        return getActiveCurveMap().get(activeChannel);
    }

    public void setActiveChannel(Channel newActiveChannel) {
        if (activeChannel == newActiveChannel) {
            return;
        }

        if (activeChannel != null && getActiveCurveMap().containsKey(activeChannel)) {
            getActiveCurve().setActive(false); // deactivate the previous
        }
        activeChannel = newActiveChannel;
        if (getActiveCurveMap().containsKey(activeChannel)) {
            getActiveCurve().setActive(true);
        }
    }

    public Channel getActiveChannel() {
        return activeChannel;
    }

    public void setColorSpace(ColorSpace colorSpace) {
        if (this.activeColorSpace == colorSpace) {
            return;
        }
        // deactivate old curve
        if (activeChannel != null && getActiveCurve() != null) {
            getActiveCurve().setActive(false);
        }

        this.activeColorSpace = colorSpace;

        // set a default active channel for the new space
        setActiveChannel(colorSpace.getPrimaryChannel());
    }

    public ColorSpace getColorSpace() {
        return activeColorSpace;
    }

    /**
     * Sets the panel dimensions and recalculates the curve area size.
     */
    public void setSize(int newPanelWidth, int newPanelHeight) {
        this.panelWidth = newPanelWidth;
        this.panelHeight = newPanelHeight;
        curveWidth = newPanelWidth - 2 * CURVE_PADDING - AXIS_PADDING;
        curveHeight = newPanelHeight - 2 * CURVE_PADDING - AXIS_PADDING;

        for (ToneCurve curve : srgbCurvesByChannel.values()) {
            curve.setSize(curveWidth, curveHeight);
        }
        for (ToneCurve curve : oklabCurvesByChannel.values()) {
            curve.setSize(curveWidth, curveHeight);
        }
    }

    public void reset() {
        for (ToneCurve curve : srgbCurvesByChannel.values()) {
            curve.reset();
        }
        for (ToneCurve curve : oklabCurvesByChannel.values()) {
            curve.reset();
        }
    }

    public void randomize() {
        for (ToneCurve curve : getActiveCurveMap().values()) {
            curve.reset();
        }
        // randomize only the primary channel of the active color space
        getCurve(activeColorSpace.getPrimaryChannel()).randomize();
    }

    /**
     * Converts panel coordinates to normalized curve coordinates (0-1).
     */
    public void normalizePoint(Point2D.Float p) {
        p.x -= CURVE_PADDING + AXIS_PADDING;
        p.y -= CURVE_PADDING;

        // invert y-axis and normalize
        p.y = curveHeight - p.y;
        p.x /= curveWidth;
        p.y /= curveHeight;
    }

    /**
     * Draws the grid, scales, and all relevant curves.
     */
    public void draw(Graphics2D g) {
        boolean darkTheme = Themes.getActive().isDark();

        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        // clear background
        Colors.fillWith(darkTheme ? Color.BLACK : Color.WHITE, g, panelWidth, panelHeight);

        // apply padding and transform for a bottom-left origin
        var origTransform = g.getTransform();
        g.translate(CURVE_PADDING + AXIS_PADDING, CURVE_PADDING);
        g.translate(0, curveHeight);
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

        double gridCellWidth = (double) curveWidth / GRID_DENSITY;
        double gridCellHeight = (double) curveHeight / GRID_DENSITY;

        for (int i = 0; i <= GRID_DENSITY; i++) {
            Path2D path = (i % 2 == 0) ? darkPath : lightPath;
            // horizontal line
            path.moveTo(0, i * gridCellHeight);
            path.lineTo(curveWidth, i * gridCellHeight);

            // vertical line
            path.moveTo(i * gridCellWidth, 0);
            path.lineTo(i * gridCellWidth, curveHeight);
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
        g.drawLine(0, 0, curveWidth, curveHeight);
    }

    /**
     * Draws the gradient scales along the horizontal and vertical axes.
     */
    private void drawScales(Graphics2D g) {
        Color startColor = null;
        Color endColor = null;

        // the activeChannel is initialized in the constructor and should not be null here.
        switch (activeChannel) {
            case OK_A, OK_B, RGB, OK_L -> {
                startColor = activeChannel.getDarkColor();
                endColor = activeChannel.getLightColor();
            }
            case RED, GREEN, BLUE -> {
                startColor = Color.BLACK;
                endColor = activeChannel.getDrawColor(true, false);
            }
        }

        // horizontal gradient
        var rectHor = new Rectangle2D.Float(0, -AXIS_PADDING, curveWidth, AXIS_SIZE);
        var gradientHor = new GradientPaint(0, 0, startColor, curveWidth, 0, endColor);
        g.setPaint(gradientHor);
        g.fill(rectHor);
        g.setColor(Color.LIGHT_GRAY);
        g.draw(rectHor);

        // vertical gradient
        var rectVer = new Rectangle2D.Float(-AXIS_PADDING, 0, AXIS_SIZE, curveHeight);
        var gradientVer = new GradientPaint(0, 0, startColor, 0, curveHeight, endColor);
        g.setPaint(gradientVer);
        g.fill(rectVer);
        g.setColor(Color.LIGHT_GRAY);
        g.draw(rectVer);
    }

    /**
     * Draws the curve for each channel, with the active one on top.
     */
    private void drawCurves(Graphics2D g, boolean darkTheme) {
        // draw the inactive curves of the active colorspace first
        for (var entry : getActiveCurveMap().entrySet()) {
            if (entry.getKey() != activeChannel) {
                entry.getValue().draw(g, darkTheme);
            }
        }

        // then draw the active curve on top
        if (getActiveCurve() != null) {
            getActiveCurve().draw(g, darkTheme);
        }
    }
}

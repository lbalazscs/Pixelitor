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

package pixelitor.filters.curves;

import pixelitor.colors.Colors;
import pixelitor.filters.levels.Channel;
import pixelitor.gui.utils.Themes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.EnumMap;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * Manages RGB and individual color channel curves (Red, Green, Blue)
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurves {
    private final EnumMap<Channel, ToneCurve> curvesByChannel
        = new EnumMap<>(Channel.class);
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
        // Initializes curves for each channel, setting RGB as the default active channel
        curvesByChannel.put(Channel.RGB, new ToneCurve(Channel.RGB));
        curvesByChannel.put(Channel.RED, new ToneCurve(Channel.RED));
        curvesByChannel.put(Channel.GREEN, new ToneCurve(Channel.GREEN));
        curvesByChannel.put(Channel.BLUE, new ToneCurve(Channel.BLUE));
        setActiveChannel(Channel.RGB);
    }

    public ToneCurve getCurve(Channel channel) {
        return curvesByChannel.get(channel);
    }

    public ToneCurve getActiveCurve() {
        return curvesByChannel.get(activeChannel);
    }

    public void setActiveChannel(Channel channel) {
        if (activeChannel != channel) {
            if (activeChannel != null) {
                curvesByChannel.get(activeChannel).setActive(false);
            }
            curvesByChannel.get(channel).setActive(true);
            activeChannel = channel;
        }
    }

    public Channel getActiveChannel() {
        return activeChannel;
    }

    /**
     * Resizes the tone curve area according to the given new panel width and height
     */
    public void setSize(int newPanelWidth, int newPanelHeight) {
        this.panelWidth = newPanelWidth;
        this.panelHeight = newPanelHeight;
        curveWidth = newPanelWidth - 2 * CURVE_PADDING - AXIS_PADDING;
        curveHeight = newPanelHeight - 2 * CURVE_PADDING - AXIS_PADDING;
        for (var entry : curvesByChannel.entrySet()) {
            entry.getValue().setSize(curveWidth, curveHeight);
        }
    }

    public void reset() {
        for (var entry : curvesByChannel.entrySet()) {
            entry.getValue().reset();
        }
    }

    /**
     * Converts a point from user input coordinates
     * to normalized curve coordinates.
     */
    public void normalizePoint(Point2D.Float p) {
        p.x -= CURVE_PADDING + AXIS_PADDING;
        p.y -= CURVE_PADDING;

        p.y = curveHeight - p.y;
        p.x /= curveWidth;
        p.y /= curveHeight;
    }

    /**
     * Draws the tone curve grid, scales, and curves for all channels
     * onto the given Graphics2D.
     */
    public void draw(Graphics2D g) {
        boolean darkTheme = Themes.getCurrent().isDark();

        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        // clear background
        Colors.fillWith(darkTheme ? Color.BLACK : Color.WHITE, g, panelWidth, panelHeight);

        // Apply padding, adjust for y-axis inversion
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
     * Draws the tone curve grid lines in alternating light and dark colors.
     */
    private void drawGrid(Graphics2D g) {
        Path2D lightPath2D = new Path2D.Float();
        Path2D darkPath2D = new Path2D.Float();

        float gridWidth = (float) curveWidth / GRID_DENSITY;
        float gridHeight = (float) curveHeight / GRID_DENSITY;
        for (int i = 0; i <= GRID_DENSITY; i++) {
            Path2D path2D = i % 2 == 0 ? darkPath2D : lightPath2D;
            // horizontal
            path2D.moveTo(0, i * gridHeight);
            path2D.lineTo(curveWidth, i * gridHeight);

            // vertical
            path2D.moveTo(i * gridWidth, 0);
            path2D.lineTo(i * gridWidth, curveHeight);
        }

        g.setStroke(GRID_STROKE);

        g.setColor(Color.LIGHT_GRAY);
        g.draw(lightPath2D);

        g.setColor(Color.GRAY);
        g.draw(darkPath2D);
    }

    /**
     * Draws the diagonal line for reference from the
     * bottom-left to the top-right of the curve area.
     */
    private void drawDiagonal(Graphics2D g) {
        g.setColor(Color.GRAY);
        g.setStroke(GRID_STROKE);
        g.drawLine(0, 0, curveWidth, curveHeight);
    }

    /**
     * Draws the gradient scales along the horizontal and
     * vertical axes based on the active channel.
     */
    private void drawScales(Graphics2D g) {
        Color gradientEndColor = (activeChannel == Channel.RGB)
            ? Color.WHITE
            : activeChannel.getDrawColor(true, false);

        // Horizontal gradient
        var rectHor = new Rectangle2D.Float(0, -AXIS_PADDING, curveWidth, AXIS_SIZE);
        var gradientHor = new GradientPaint(0, 0, Color.BLACK, curveWidth, 0, gradientEndColor);
        g.setPaint(gradientHor);
        g.fill(rectHor);
        g.setColor(Color.LIGHT_GRAY);
        g.draw(rectHor);

        // Vertical gradient
        var rectVer = new Rectangle2D.Float(-AXIS_PADDING, 0, AXIS_SIZE, curveHeight);
        var gradientVer = new GradientPaint(0, 0, Color.BLACK, 0, curveHeight, gradientEndColor);
        g.setPaint(gradientVer);
        g.fill(rectVer);
        g.setColor(Color.LIGHT_GRAY);
        g.draw(rectVer);
    }

    /**
     * Draws the curves for each channel.
     */
    private void drawCurves(Graphics2D g, boolean darkTheme) {
        // draw the inactive curves first...
        for (var entry : curvesByChannel.entrySet()) {
            if (entry.getKey() != activeChannel) {
                entry.getValue().draw(g, darkTheme);
            }
        }

        // ...then the active curve on top.
        curvesByChannel.get(activeChannel).draw(g, darkTheme);
    }
}

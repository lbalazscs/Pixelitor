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

package pixelitor.filters.curves;

import pixelitor.colors.Colors;
import pixelitor.filters.levels.Channel;

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
 * Represents set of [RGB,R,G,B] curves
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurves {
    private final EnumMap<Channel, ToneCurve> curvesByChannel
        = new EnumMap<>(Channel.class);
    private Channel activeChannel;
    private final BasicStroke gridStroke = new BasicStroke(1);
    private int width = 295;
    private int height = 295;
    private int curveWidth = 255;
    private int curveHeight = 255;
    private static final int CURVE_PADDING = 10;
    private static final int AXIS_PADDING = 20;
    private static final int AXIS_SIZE = 10;
    private static final int GRID_DENSITY = 4;

    public ToneCurves() {
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

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        curveWidth = width - 2 * CURVE_PADDING - AXIS_PADDING;
        curveHeight = height - 2 * CURVE_PADDING - AXIS_PADDING;
        for (var entry : curvesByChannel.entrySet()) {
            entry.getValue().setSize(curveWidth, curveHeight);
        }
    }

    public void reset() {
        for (var entry : curvesByChannel.entrySet()) {
            entry.getValue().reset();
        }
    }

    public void normalizePoint(Point2D.Float p) {
        p.x -= CURVE_PADDING + AXIS_PADDING;
        p.y -= CURVE_PADDING;

        p.y = curveHeight - p.y;
        p.x /= curveWidth;
        p.y /= curveHeight;
    }

    public void draw(Graphics2D g) {
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        // clear background
        Colors.fillWith(Color.WHITE, g, width, height);

        // apply CURVE_PADDING, and prepare for y-axis up drawing
        var origTransform = g.getTransform();
        g.translate(CURVE_PADDING + AXIS_PADDING, CURVE_PADDING);
        g.translate(0, curveHeight);
        g.scale(1.0, -1.0);

        drawGrid(g);
        drawDiagonal(g);
        drawScales(g);
        drawCurves(g);

        g.setTransform(origTransform);
    }

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

        g.setStroke(gridStroke);

        g.setColor(Color.LIGHT_GRAY);
        g.draw(lightPath2D);

        g.setColor(Color.GRAY);
        g.draw(darkPath2D);
    }

    private void drawScales(Graphics2D g) {
        Color gradientEndColor;
        if (activeChannel == Channel.RGB) {
            gradientEndColor = Color.WHITE;
        } else {
            gradientEndColor = activeChannel.getDrawColor(true);
        }

        // draw horizontal gradient
        var rectH = new Rectangle2D.Float(0, -AXIS_PADDING, curveWidth, AXIS_SIZE);
        var gradientH = new GradientPaint(0, 0, Color.BLACK, curveWidth, 0, gradientEndColor);
        g.setPaint(gradientH);
        g.fill(rectH);
        g.setColor(Color.LIGHT_GRAY);
        g.draw(rectH);

        // draw vertical gradient
        var rectV = new Rectangle2D.Float(-AXIS_PADDING, 0, AXIS_SIZE, curveHeight);
        gradientH = new GradientPaint(0, 0, Color.BLACK, 0, curveHeight, gradientEndColor);
        g.setPaint(gradientH);
        g.fill(rectV);
        g.setColor(Color.LIGHT_GRAY);
        g.draw(rectV);
    }

    private void drawDiagonal(Graphics2D g) {
        g.setColor(Color.GRAY);
        g.setStroke(gridStroke);
        g.drawLine(0, 0, curveWidth, curveHeight);
    }

    private void drawCurves(Graphics2D g) {
        // on back draw inactive curves
        for (var entry : curvesByChannel.entrySet()) {
            if (entry.getKey() != activeChannel) {
                entry.getValue().draw(g);
            }
        }

        // on top draw active curve
        curvesByChannel.get(activeChannel).draw(g);
    }
}

/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.EnumMap;

/**
 * Represents set of [RGB,R,G,B] curves
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurves {
    private final EnumMap<Channel, ToneCurve> curvesByChannel
        = new EnumMap<>(Channel.class);
    private Channel activeChannel;
    private Graphics2D gr;
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

    public void normalizePoint(Point.Float p) {
        p.x -= CURVE_PADDING + AXIS_PADDING;
        p.y -= CURVE_PADDING;

        p.y = curveHeight - p.y;
        p.x /= curveWidth;
        p.y /= curveHeight;
    }

    public void setG2D(Graphics2D gr) {
        this.gr = gr;
        for (var entry : curvesByChannel.entrySet()) {
            entry.getValue().setG2D(gr);
        }
    }

    public void draw() {
        gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // clear background
        Colors.fillWith(Color.WHITE, gr, width, height);

        // apply CURVE_PADDING, and prepare for y-axis up drawing
        var transform = gr.getTransform();
        var curveTransform = new AffineTransform();
        curveTransform.translate(CURVE_PADDING + AXIS_PADDING, CURVE_PADDING);
        curveTransform.translate(0, curveHeight);
        curveTransform.scale(1.0, -1.0);
        gr.setTransform(curveTransform);

        drawGrid();
        drawDiagonal();
        drawScales();
        drawCurves();

        gr.setTransform(transform);
    }

    private void drawGrid() {
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

        gr.setStroke(gridStroke);

        gr.setColor(Color.LIGHT_GRAY);
        gr.draw(lightPath2D);

        gr.setColor(Color.GRAY);
        gr.draw(darkPath2D);
    }

    private void drawScales() {
        Color gradientEndColor;
        if (activeChannel == Channel.RGB) {
            gradientEndColor = Color.WHITE;
        } else {
            gradientEndColor = activeChannel.getDrawColor(true);
        }

        // draw horizontal gradient
        var rectH = new Rectangle.Float(0, -AXIS_PADDING, curveWidth, AXIS_SIZE);
        var gradientH = new GradientPaint(0, 0, Color.BLACK, curveWidth, 0, gradientEndColor);
        gr.setPaint(gradientH);
        gr.fill(rectH);
        gr.setColor(Color.LIGHT_GRAY);
        gr.draw(rectH);

        // draw vertical gradient
        var rectV = new Rectangle.Float(-AXIS_PADDING, 0, AXIS_SIZE, curveHeight);
        gradientH = new GradientPaint(0, 0, Color.BLACK, 0, curveHeight, gradientEndColor);
        gr.setPaint(gradientH);
        gr.fill(rectV);
        gr.setColor(Color.LIGHT_GRAY);
        gr.draw(rectV);
    }

    private void drawDiagonal() {
        gr.setColor(Color.GRAY);
        gr.setStroke(gridStroke);
        gr.drawLine(0, 0, curveWidth, curveHeight);
    }

    private void drawCurves() {
        // on back draw inactive curves
        for (var entry : curvesByChannel.entrySet()) {
            if (entry.getKey() != activeChannel) {
                entry.getValue().draw();
            }
        }

        // on top draw active curve
        curvesByChannel.get(activeChannel).draw();
    }
}

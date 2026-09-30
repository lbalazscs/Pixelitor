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

package pixelitor.tools.brushes;

import pixelitor.layers.Drawable;
import pixelitor.tools.AbstractBrushTool;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.debug.DebugNode;

import java.awt.Graphics2D;
import java.util.Objects;

/**
 * Base implementation for brushes that directly apply marks, as opposed to decorators.
 */
public abstract class AbstractBrush implements Brush {
    protected Graphics2D targetG;
    protected Drawable dr;

    protected double radius = AbstractBrushTool.DEFAULT_BRUSH_RADIUS;
    protected double diameter;

    // tracks the maximum radius used during a stroke for undo bounds calculation
    private double maxRadiusDuringStroke = 0;
    protected PPoint prevPos;

    // true when the mouse is down
    private boolean drawing;

    protected AbstractBrush(double radius) {
        setRadius(radius);
    }

    @Override
    public void setRadius(double radius) {
        this.radius = radius;
        this.diameter = 2 * radius;
        if (radius > maxRadiusDuringStroke) {
            maxRadiusDuringStroke = radius;
        }
    }

    @Override
    public double getMaxEffectiveRadius() {
        // add a small margin to account for potential rounding errors
        return maxRadiusDuringStroke + 1.0;
    }

    @Override
    public void setTarget(Drawable dr, Graphics2D g) {
        this.dr = Objects.requireNonNull(dr);
        targetG = Objects.requireNonNull(g);
    }

    /**
     * Requests a repaint of the view covering the area between the
     * previous and current points, expanded by the brush diameter.
     * Should be called *before* updating the previous point with {@link #setPrevPos(PPoint)}.
     */
    protected void repaintSegment(PPoint p) {
        dr.repaintRegion(prevPos, p, diameter);
    }

    /**
     * Sets the previous point.
     * Should be called *after* any operations that require the old previous point, like {@link #repaintSegment(PPoint)}.
     */
    @Override
    public void setPrevPos(PPoint p) {
        prevPos = p;
    }

    @Override
    public PPoint getPrevPos() {
        return prevPos;
    }

    @Override
    public void startStrokeAt(PPoint p) {
        // when starting a new stroke, the previous
        // point should not be set to (0, 0)
        // because it causes unnecessary repainting
        setPrevPos(p);

        initDrawing(p);

        // reset at the beginning of the brush stroke
        maxRadiusDuringStroke = radius;
    }

    @Override
    public void initDrawing(PPoint p) {
        assert !drawing : "already initialized in " + getClass().getSimpleName();
        drawing = true;
    }

    @Override
    public void connectWithLineTo(PPoint p) {
        assert !drawing : "already initialized in " + getClass().getSimpleName();

        if (prevPos == null) {
            // can happen if the first click (in the tool or
            // after a symmetry activation) is a shift-click
            startStrokeAt(p);
        } else {
            initDrawing(p);

            // most brushes connect with lines anyway, but if
            // there is extra smoothing, this must be overridden
            continueTo(p);
        }
    }

    @Override
    public void finishBrushStroke() {
        assert drawing : "uninitialized brush stroke in " + getClass().getSimpleName();
        drawing = false;
    }

    public double getRadius() {
        return radius;
    }

    @Override
    public boolean isDrawing() {
        return drawing;
    }

    public void settingsChanged() {
        // subclasses can override this to react to settings changes
    }

    @Override
    public DebugNode createDebugNode(String key) {
        var node = new DebugNode(key, this);
        node.addClass();
        node.addDouble("radius", radius);
        node.addNullableDebuggable("prevPos", prevPos);
        node.addBoolean("drawing", drawing);
        node.addDouble("maxRadiusDuringStroke", maxRadiusDuringStroke);

        return node;
    }
}

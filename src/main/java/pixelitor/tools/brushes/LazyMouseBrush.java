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

import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.View;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.layers.Drawable;
import pixelitor.tools.util.PPoint;

import java.awt.Graphics2D;
import java.util.function.IntSupplier;

/**
 * A brush decorator that implements the "lazy mouse" feature,
 * smoothing strokes by lagging behind the mouse cursor.
 */
public class LazyMouseBrush extends BrushDecorator {
    private static final int MIN_LAZY_DIST = 10;
    private static final int DEFAULT_LAZY_DIST = 30;
    private static final int MAX_LAZY_DIST = 200;
    private static final int FALLBACK_SPACING = 3;

    // the target: the user's current mouse cursor position (image space)
    private double mouseX;
    private double mouseY;

    // the current drawing position of the delegate brush (image space)
    private double drawX;
    private double drawY;

    private View view;
    private double spacing;

    // supplies the current lazy mouse distance in image-space pixels;
    // it is read on every use, so that changes in the owning
    // tool's GUI take effect immediately
    private final IntSupplier lazyDist;

    public LazyMouseBrush(Brush delegate, IntSupplier lazyDist) {
        super(delegate);
        this.lazyDist = lazyDist;

        // copy the previous position of the delegate so that
        // a Shift-click as the first action after enabling
        // lazy mouse starts the line where the stroke ended
        PPoint previous = delegate.getPrevPos();
        if (previous != null) {
            drawX = previous.getImX();
            drawY = previous.getImY();
        }

        updateSpacing();
    }

    private void updateSpacing() {
        spacing = delegate.getPreferredSpacing();
        if (spacing == 0) {
            // fall back to the default if the delegate doesn't specify spacing
            spacing = FALLBACK_SPACING;
        }
    }

    @Override
    public void setTarget(Drawable dr, Graphics2D g) {
        delegate.setTarget(dr, g);
        view = dr.getComp().getView();
    }

    @Override
    public void startStrokeAt(PPoint p) {
        delegate.startStrokeAt(p);

        mouseX = p.getImX();
        mouseY = p.getImY();

        drawX = mouseX;
        drawY = mouseY;

        updateSpacing();
    }

    @Override
    public void continueTo(PPoint p) {
        advanceTo(p);
    }

    /**
     * Advances the delegate brush toward the target point in steps
     * until the draw position is within the lazy distance of the mouse.
     */
    private void advanceTo(PPoint targetPoint) {
        mouseX = targetPoint.getImX();
        mouseY = targetPoint.getImY();

        double dx = mouseX - drawX;
        double dy = mouseY - drawY;
        double distSq = dx * dx + dy * dy;

        // The loop below stops once the draw position is within
        // sqrt(d² + s²) of the mouse (d = lazy distance, s = spacing).
        // For small spacing this is about d, so the lag is roughly d.
        // The s² term keeps the loop safe: each step moves by s along
        // a fixed direction, so the stop distance must exceed s, or a
        // step could overshoot the mouse and run away from it.
        // d² + s² > s² guarantees this.
        double d = lazyDist.getAsInt();
        double stopDistSq = d * d + spacing * spacing;
        if (distSq <= stopDistSq) {
            return; // within the lazy distance: the loop wouldn't run
        }

        // here dist > spacing > 0, so the division is safe
        double dist = Math.sqrt(distSq);
        double scale = spacing / dist;
        double stepDx = dx * scale;
        double stepDy = dy * scale;

        while (distSq > stopDistSq) {
            drawX += stepDx;
            drawY += stepDy;
            PPoint drawPoint = PPoint.fromIm(drawX, drawY, view);
            delegate.continueTo(drawPoint);

            dx = mouseX - drawX;
            dy = mouseY - drawY;
            distSq = dx * dx + dy * dy;
        }
    }

    @Override
    public void connectWithLineTo(PPoint p) {
        assert !isDrawing();
        assert hasPrevPos();
        initDrawing(p);

        advanceTo(p);
    }

    /**
     * Creates a new parameter for the lazy mouse distance.
     * Each tool owns its own instance and passes it to
     * its {@link LazyMouseBrush} via an {@link IntSupplier}.
     */
    public static RangeParam createDistParam() {
        return new RangeParam("Distance (px)",
            MIN_LAZY_DIST, DEFAULT_LAZY_DIST, MAX_LAZY_DIST,
            false, SliderSpinner.LabelPosition.NONE);
    }

    public PPoint getDrawLocation() {
        return PPoint.fromIm(drawX, drawY, view);
    }
}

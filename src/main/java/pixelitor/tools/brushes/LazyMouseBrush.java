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

package pixelitor.tools.brushes;

import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.View;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.layers.Drawable;
import pixelitor.tools.util.PPoint;

import java.awt.Graphics2D;

/**
 * A brush decorator that implements the "lazy mouse" feature,
 * smoothing strokes by lagging behind the mouse cursor.
 */
public class LazyMouseBrush extends BrushDecorator {
    private static final int MIN_LAZY_DIST = 10;
    private static final int DEFAULT_LAZY_DIST = 30;
    private static final int MAX_LAZY_DIST = 200;
    private static final int DEFAULT_SPACING = 3;

    // the target: the user's current mouse cursor position (image space)
    private double mouseX;
    private double mouseY;

    // the current drawing position of the delegate brush (image space)
    private double drawX;
    private double drawY;

    private View view;
    private double spacing;

    // the lazy mouse distance is shared between the tools
    private static int lazyDist = DEFAULT_LAZY_DIST;
    private static double lazyDist2 = DEFAULT_LAZY_DIST * DEFAULT_LAZY_DIST;

    public LazyMouseBrush(Brush delegate) {
        super(delegate);

        // copy the previous position of the delegate so that
        // if this object starts with shift-clicked lines, the
        // old positions are continued
        PPoint previous = delegate.getPrevious();
        if (previous != null) {
            drawX = previous.getImX();
            drawY = previous.getImY();
        }

        updateSpacing();
    }

    public static void setLazyDist(int value) {
        lazyDist = value;
        lazyDist2 = value * value;
    }

    private void updateSpacing() {
        spacing = delegate.getPreferredSpacing();
        if (spacing == 0) {
            // fall back to the default if the delegate doesn't specify spacing
            spacing = DEFAULT_SPACING;
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
     * Advances the delegate brush toward the target point in steps.
     */
    private void advanceTo(PPoint targetPoint) {
        mouseX = targetPoint.getImX();
        mouseY = targetPoint.getImY();

        double dx = mouseX - drawX;
        double dy = mouseY - drawY;
        double dist2 = dx * dx + dy * dy;

        if (dist2 <= spacing * spacing) {
            return; // Skip if within a single step
        }

        double dist = Math.sqrt(dist2);
        double unitDx = dx / dist;
        double unitDy = dy / dist;
        double stepDx = unitDx * spacing;
        double stepDy = unitDy * spacing;

        double remainingDist2 = lazyDist2 + spacing * spacing;

        while (dist2 > remainingDist2) {
            drawX += stepDx;
            drawY += stepDy;
            PPoint drawPoint = PPoint.fromIm(drawX, drawY, view);
            delegate.continueTo(drawPoint);

            dx = mouseX - drawX;
            dy = mouseY - drawY;
            dist2 = dx * dx + dy * dy;
        }
    }

    @Override
    public void lineConnectTo(PPoint p) {
        assert !isDrawing();
        assert hasPrevious();
        initDrawing(p);

        advanceTo(p);
    }

    public static RangeParam createDistParam() {
        RangeParam param = new RangeParam("Distance (px)", MIN_LAZY_DIST, lazyDist, MAX_LAZY_DIST,
            false, SliderSpinner.LabelPosition.NONE);
        param.setAdjustmentListener(() -> setLazyDist(param.getValue()));
        return param;
    }

    public PPoint getDrawLocation() {
        return PPoint.fromIm(drawX, drawY, view);
    }
}

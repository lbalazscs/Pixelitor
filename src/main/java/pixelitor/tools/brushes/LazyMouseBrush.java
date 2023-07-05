/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
 * A brush with the "lazy mouse" feature enabled is
 * a decorator for a delegate brush
 */
public class LazyMouseBrush extends BrushDecorator {
    private static final int MIN_DIST = 10;
    private static final int DEFAULT_DIST = 30;
    private static final int MAX_DIST = 200;

    private static final int DEFAULT_SPACING = 3;

    private double mouseX;
    private double mouseY;
    private double drawX;
    private double drawY;
    private View view;
    private double spacing;

    // the lazy mouse distance is shared between the tools
    private static int dist = DEFAULT_DIST;
    private static double dist2 = DEFAULT_DIST * DEFAULT_DIST;

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

        calcSpacing();
    }

    public static void setDist(int value) {
        dist = value;
        dist2 = value * value;
    }

    private void calcSpacing() {
        spacing = delegate.getPreferredSpacing();
        if (spacing == 0) {
            spacing = DEFAULT_SPACING;
        }
    }

    @Override
    public void setTarget(Drawable dr, Graphics2D g) {
        delegate.setTarget(dr, g);

        view = dr.getComp().getView();
    }

    @Override
    public void startAt(PPoint p) {
        delegate.startAt(p);

        mouseX = p.getImX();
        mouseY = p.getImY();

        drawX = mouseX;
        drawY = mouseY;

        calcSpacing();
    }

    @Override
    public void continueTo(PPoint p) {
        advanceTo(p);
    }

    private void advanceTo(PPoint p) {
        mouseX = p.getImX();
        mouseY = p.getImY();

        double dx = mouseX - drawX;
        double dy = mouseY - drawY;

        double d2 = dx * dx + dy * dy;

        double angle = Math.atan2(dy, dx);
        double advanceDX = spacing * Math.cos(angle);
        double advanceDY = spacing * Math.sin(angle);

        // It is important to consider here the spacing in order to avoid
        // infinite loops for large spacings (shape brush + large radius).
        // The math might not be 100% correct, but it looks OK.
        double minValue = dist2 + spacing * spacing;

        while (d2 > minValue) {
            drawX += advanceDX;
            drawY += advanceDY;

            PPoint drawPoint = PPoint.fromIm(drawX, drawY, view);
            delegate.continueTo(drawPoint);

            dx = mouseX - drawX;
            dy = mouseY - drawY;
            d2 = dx * dx + dy * dy;
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
        RangeParam param = new RangeParam("Distance (px)", MIN_DIST, dist, MAX_DIST,
            false, SliderSpinner.TextPosition.NONE);
        param.setAdjustmentListener(() -> setDist(param.getValue()));
        return param;
    }

    public PPoint getDrawPoint() {
        return PPoint.fromIm(drawX, drawY, view);
    }
}

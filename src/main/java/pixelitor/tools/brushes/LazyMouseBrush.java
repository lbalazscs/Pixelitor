/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Composition;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.ImageComponent;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.debug.DebugNode;

import java.awt.Graphics2D;

/**
 * A brush with the "lazy mouse" feature enabled is
 * a decorator for a delegate brush
 */
public class LazyMouseBrush implements Brush {
    private static final int MIN_DIST = 10;
    private static final int DEFAULT_DIST = 30;
    private static final int MAX_DIST = 200;

    private final Brush delegate;
    private double mouseX;
    private double mouseY;
    private double drawX;
    private double drawY;
    private ImageComponent ic;
    private double spacing;

    // the lazy mouse distance is shared between the tools
    private static int minDist = DEFAULT_DIST;
    private static double minDist2 = DEFAULT_DIST * DEFAULT_DIST;

    public LazyMouseBrush(Brush delegate) {
        this.delegate = delegate;
        spacing = delegate.getPreferredSpacing();
        if (spacing == 0) {
            spacing = 3;
        }
    }

    private static void setDist(int value) {
        minDist = value;
        minDist2 = value * value;
    }

    @Override
    public void setTarget(Composition comp, Graphics2D g) {
        delegate.setTarget(comp, g);

        ic = comp.getIC();
    }

    @Override
    public void setRadius(int radius) {
        delegate.setRadius(radius);
    }

    @Override
    public void onStrokeStart(PPoint p) {
        delegate.onStrokeStart(p);

        mouseX = p.getImX();
        mouseY = p.getImY();

        drawX = mouseX;
        drawY = mouseY;
    }

    @Override
    public void onNewStrokePoint(PPoint p) {
        mouseX = p.getImX();
        mouseY = p.getImY();

        double dx = mouseX - drawX;
        double dy = mouseY - drawY;

        double dist2 = dx * dx + dy * dy;

        // TODO calculate without trigonometry
        double angle = Math.atan2(dy, dx);
        double advanceDX = spacing * Math.cos(angle);
        double advanceDY = spacing * Math.sin(angle);

        while (dist2 > minDist2) {
            this.drawX += advanceDX;
            this.drawY += advanceDY;

            delegate.onNewStrokePoint(
                    new PPoint.Image(ic, this.drawX, this.drawY));

            dx = mouseX - this.drawX;
            dy = mouseY - this.drawY;
            dist2 = dx * dx + dy * dy;
        }
    }

    @Override
    public void lineConnectTo(PPoint p) {
        // TODO
        onNewStrokePoint(p);
    }

    @Override
    public DebugNode getDebugNode() {
        return delegate.getDebugNode();
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }

    @Override
    public double getPreferredSpacing() {
        return delegate.getPreferredSpacing();
    }

    public static RangeParam createParam() {
        RangeParam param = new RangeParam(
                "Distance (px)", MIN_DIST, minDist, MAX_DIST);
        param.setAdjustmentListener(() ->
                LazyMouseBrush.setDist(param.getValue()));
        return param;
    }
}

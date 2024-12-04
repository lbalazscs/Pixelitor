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

package pixelitor.tools.brushes;

import pixelitor.layers.Drawable;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.debug.DebugNode;

import java.awt.Graphics2D;

/**
 * A base class for brush decorators.
 * It implements all {@link Brush} methods by delegating them.
 */
public abstract class BrushDecorator implements Brush {
    protected final Brush delegate;

    protected BrushDecorator(Brush delegate) {
        this.delegate = delegate;
    }

    @Override
    public void startAt(PPoint p) {
        delegate.startAt(p);
    }

    @Override
    public void continueTo(PPoint p) {
        delegate.continueTo(p);
    }

    @Override
    public void lineConnectTo(PPoint p) {
        delegate.lineConnectTo(p);
    }

    @Override
    public void finishBrushStroke() {
        delegate.finishBrushStroke();
    }

    @Override
    public boolean isDrawing() {
        return delegate.isDrawing();
    }

    @Override
    public void initDrawing(PPoint p) {
        delegate.initDrawing(p);
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }

    @Override
    public PPoint getPrevious() {
        return delegate.getPrevious();
    }

    @Override
    public void setPrevious(PPoint previous) {
        delegate.setPrevious(previous);
    }

    @Override
    public boolean hasPrevious() {
        return delegate.hasPrevious();
    }

    @Override
    public void setTarget(Drawable dr, Graphics2D g) {
        delegate.setTarget(dr, g);
    }

    @Override
    public void setRadius(double radius) {
        delegate.setRadius(radius);
    }

    @Override
    public double getMaxEffectiveRadius() {
        return delegate.getMaxEffectiveRadius();
    }

    @Override
    public double getPreferredSpacing() {
        return delegate.getPreferredSpacing();
    }

    @Override
    public DebugNode createDebugNode(String key) {
        return delegate.createDebugNode(key);
    }
}

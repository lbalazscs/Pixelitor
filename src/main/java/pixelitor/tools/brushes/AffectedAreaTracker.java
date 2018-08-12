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
import pixelitor.tools.util.PPoint;
import pixelitor.utils.debug.DebugNode;

import java.awt.Graphics2D;

/**
 * A decorator for other brushes that tracks their affected area.
 */
public class AffectedAreaTracker implements Brush {
    private Brush delegate;
    private final AffectedArea affectedArea;

    public AffectedAreaTracker(Brush delegate, AffectedArea affectedArea) {
        this.delegate = delegate;
        this.affectedArea = affectedArea;
    }

    @Override
    public void setTarget(Composition comp, Graphics2D g) {
        delegate.setTarget(comp, g);
    }

    @Override
    public void setRadius(int radius) {
        delegate.setRadius(radius);
    }

    @Override
    public void onStrokeStart(PPoint p) {
        affectedArea.initAt(p);
        delegate.onStrokeStart(p);
    }

    @Override
    public void onNewStrokePoint(PPoint p) {
        affectedArea.updateWith(p);
        delegate.onNewStrokePoint(p);
    }

    @Override
    public void lineConnectTo(PPoint p) {
        affectedArea.updateWith(p);
        delegate.lineConnectTo(p);
    }

    @Override
    public double getPreferredSpacing() {
        return delegate.getPreferredSpacing();
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = new DebugNode("Affected Area Tracker", this);

        node.add(delegate.getDebugNode());
        node.add(affectedArea.getDebugNode());

        return node;
    }
}

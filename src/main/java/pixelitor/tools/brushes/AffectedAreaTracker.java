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

import pixelitor.tools.util.PPoint;
import pixelitor.utils.debug.DebugNode;

/**
 * A decorator for other brushes that tracks their affected area.
 */
public class AffectedAreaTracker extends BrushDecorator {
    private final AffectedArea affectedArea;

    public AffectedAreaTracker(Brush delegate, AffectedArea affectedArea) {
        super(delegate);
        this.affectedArea = affectedArea;
    }

    @Override
    public void startAt(PPoint p) {
        affectedArea.initAt(p);
        delegate.startAt(p);
    }

    @Override
    public void continueTo(PPoint p) {
        affectedArea.updateWith(p);
        delegate.continueTo(p);
    }

    @Override
    public void lineConnectTo(PPoint p) {
        affectedArea.updateWith(p);
        delegate.lineConnectTo(p);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        var node = new DebugNode(key, this);

        node.addClass();
        node.add(delegate.createDebugNode(key));
        node.add(affectedArea.createDebugNode("affected area"));

        return node;
    }
}

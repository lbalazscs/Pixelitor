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

package pixelitor.tools.transform;

import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.history.MultiEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.utils.debug.DebugNode;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

/**
 * A container to apply a single transformation to multiple Transformable objects.
 */
public class CompositeTransformable implements Transformable {
    private final List<Transformable> targets = new ArrayList<>();
    private final Composition comp;

    public CompositeTransformable(Composition comp) {
        this.comp = comp;
    }

    public void add(Transformable t) {
        targets.add(t);
    }

    @Override
    public void imTransform(AffineTransform transform) {
        for (Transformable t : targets) {
            t.imTransform(transform);
        }
    }

    @Override
    public void updateUI(View view) {
        if (!targets.isEmpty()) {
            targets.getFirst().updateUI(view);
        }
    }

    @Override
    public void prepareForTransform() {
        for (Transformable t : targets) {
            t.prepareForTransform();
        }
    }

    @Override
    public PixelitorEdit finalizeTransform() {
        MultiEdit edits = new MultiEdit("Free Transform", comp);
        for (Transformable t : targets) {
            edits.add(t.finalizeTransform());
        }
        return edits;
    }

    @Override
    public void cancelTransform() {
        for (Transformable t : targets) {
            t.cancelTransform();
        }
    }

    @Override
    public DebugNode createDebugNode(String name) {
        DebugNode node = new DebugNode(name, this);
        for (int i = 0; i < targets.size(); i++) {
            node.add(targets.get(i).createDebugNode("target_" + i));
        }
        return node;
    }
}


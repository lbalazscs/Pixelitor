/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.move;

import pixelitor.gui.View;
import pixelitor.selection.Selection;
import pixelitor.tools.transform.Transformable;
import pixelitor.utils.debug.DebugNode;

import java.awt.geom.AffineTransform;

record SelectionTransformable(Selection selection) implements Transformable {
    @Override
    public void imTransform(AffineTransform transform) {
        selection.transformWhileDragging(transform);
    }

    @Override
    public void updateUI(View view) {
        view.repaint();
    }

    @Override
    public DebugNode createDebugNode() {
        DebugNode node = new DebugNode("SelectionTransformable", this);
        node.add(selection.createDebugNode("selection"));
        return node;
    }
}

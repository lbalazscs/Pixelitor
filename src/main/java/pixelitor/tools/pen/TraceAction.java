/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.pen;

import pixelitor.Composition;
import pixelitor.gui.OpenComps;
import pixelitor.layers.Drawable;
import pixelitor.menus.MenuAction;
import pixelitor.tools.AbstractBrushTool;
import pixelitor.utils.Messages;
import pixelitor.utils.test.RandomGUITest;

import java.awt.Shape;

/**
 * Strokes a shape with an {@link AbstractBrushTool}
 */
public class TraceAction extends MenuAction {
    private final AbstractBrushTool brushTool;

    public TraceAction(String name, AbstractBrushTool brushTool) {
        super(name);
        this.brushTool = brushTool;
    }

    @Override
    public void onClick() {
        OpenComps.onActiveComp(this::trace);
    }

    private void trace(Composition comp) {
        if (!comp.activeIsDrawable()) {
            Messages.showNotDrawableError();
            return;
        }

        Path path = comp.getActivePath();
        if (path == null) {
            if (!RandomGUITest.isRunning()) {
                Messages.showInfo("No path",
                        "There is no path in the composition");
            }
            return;
        }

        Shape shape = path.toImageSpaceShape();
        if (shape != null) {
            Drawable dr = comp.getActiveDrawableOrThrow();
            brushTool.trace(dr, shape);
        }
    }
}

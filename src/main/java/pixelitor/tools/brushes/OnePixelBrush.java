/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import pixelitor.utils.debug.DebugNode;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_OFF;

/**
 * A brush that edits one pixel at a time
 */
public class OnePixelBrush extends AbstractBrush {
    private final OnePixelBrushSettings settings;

    public OnePixelBrush(OnePixelBrushSettings settings) {
        super(1); // this radius value is not used by this brush
        this.settings = settings;
    }

    @Override
    public void onDragStart(double x, double y) {
        updateComp(x, y);
        setPrevious(x, y);

        // make sure a pixel is changed without dragging
        onNewMousePoint(x, y);
    }

    @Override
    public void onNewMousePoint(double x, double y) {
        if (!settings.hasAA()) {
            targetG.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_OFF);
        }

        targetG.drawLine((int) previousX, (int) previousY, (int) x, (int) y);
        updateComp(x, y);
        setPrevious(x, y);
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        node.addBooleanChild("Anti-aliasing", settings.hasAA());

        return node;
    }
}

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

import pixelitor.tools.util.PPoint;
import pixelitor.utils.debug.DebugNode;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_OFF;

/**
 * A brush that edits one pixel at a time
 */
public class OnePixelBrush extends AbstractBrush {
    private final OnePixelBrushSettings settings;

    public OnePixelBrush(OnePixelBrushSettings settings) {
        super(1); // the radius value is not used by this brush
        this.settings = settings;
    }

    @Override
    public void startStrokeAt(PPoint p) {
        super.startStrokeAt(p);
        repaintComp(p);
        setPrevious(p);

        // ensure that a click has an effect
        continueTo(p);
    }

    @Override
    public void continueTo(PPoint p) {
        if (!settings.hasAA()) {
            targetG.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_OFF);
        }

        previous.drawLineTo(p, targetG);
        repaintComp(p);
        setPrevious(p);
    }

    @Override
    public double getPreferredSpacing() {
        return 0;
    }

    @Override
    public void dispose() {
        // do nothing
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);
        node.addBoolean("anti-aliasing", settings.hasAA());
        return node;
    }
}

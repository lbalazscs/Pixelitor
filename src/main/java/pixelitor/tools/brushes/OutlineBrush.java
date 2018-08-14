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

import pixelitor.tools.shapes.StrokeType;
import pixelitor.tools.util.PPoint;

public abstract class OutlineBrush extends StrokeBrush {
    private final OutlineBrushSettings settings;
    private double origRadius;

    protected OutlineBrush(OutlineBrushSettings settings, int radius, int cap, int join) {
        super(radius, StrokeType.OUTLINE, cap, join);
        this.settings = settings;
    }

    @Override
    public void setRadius(int radius) {
        super.setRadius(radius);
        origRadius = radius;
    }

    @Override
    public void onNewStrokePoint(PPoint p) {
        if (settings.dependsOnSpeed()) {
            double dist = previous.imDist(p);

            double scaled = origRadius / Math.sqrt(dist);

            radius = (int) scaled;
            diameter = (int) (2 * scaled);

            currentStroke = createStroke((float) scaled);
        }

        super.onNewStrokePoint(p);
    }
}

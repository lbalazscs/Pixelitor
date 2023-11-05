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

package pixelitor.tools.transform;

import pixelitor.gui.View;
import pixelitor.utils.debug.Debuggable;

import java.awt.geom.AffineTransform;

/**
 * A callback interface that is passed to a {@link TransformBox} so that something
 * can be automatically transformed with the {@link AffineTransform} created by the box.
 */
public interface Transformable extends Debuggable {
    /**
     * Apply the given transform in image space.
     */
    void imTransform(AffineTransform transform);

    /**
     * Update the UI. A simple repaint isn't enough for the
     * shapes tool, because the image has to be recalculated.
     */
    void updateUI(View view);
}

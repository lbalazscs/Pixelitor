/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

/**
 * Specifies the way the dabs of a {@link DabsBrush}
 * are placed in response to mouse events.
 *
 * Unlike brush settings, these objects can't be shared
 * between symmetry brushes, because they call back a specific brush.
 */
public interface DabsStrategy {
    void onStrokeStart(PPoint p);

    void onNewStrokePoint(PPoint p);

    void settingsChanged();

    void rememberPrevious(PPoint previous);
}

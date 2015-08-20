/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

/**
 * A dabs strategy specifies the way the dabs of a
 * DabsBrush are placed in response to GUI events.
 *
 * Unlike brush settings, these objects cannot be shared
 * between symmetry brushes, because they call back a specific brush.
 */
public interface DabsStrategy {
    void onDragStart(double x, double y);

    void onNewMousePoint(double x, double y);

    void settingsChanged();
}

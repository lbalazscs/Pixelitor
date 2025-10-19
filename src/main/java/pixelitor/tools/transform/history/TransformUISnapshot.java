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

package pixelitor.tools.transform.history;

import pixelitor.tools.move.MoveMode;
import pixelitor.tools.transform.TransformBox;

/**
 * A snapshot of the UI state of a free-transform session.
 * This is stored in a history edit to allow the interactive TransformBox
 * to be restored when an action is undone.
 *
 * @param memento    The state of the TransformBox, including its original bounds,
 *                   handle positions, and angle.
 * @param moveMode   The MoveMode that was active during the transformation.
 */
public record TransformUISnapshot(
    TransformBox.Memento memento,
    MoveMode moveMode
) {
}

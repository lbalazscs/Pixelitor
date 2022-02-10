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
package pixelitor.filters.gui;

import java.io.Serializable;

/**
 * Captures the state of a filter parameter at a given moment
 * (like the "Memento" design pattern)
 *
 * The self-bounded type parameter is for ensuring that
 * interpolate receives and returns the actual type
 */
public interface ParamState<S extends ParamState<S>> extends Serializable {
    /**
     * Calculate a new interpolated ParamState object,
     * where the current object represents the starting state
     * and the given argument represents the end state
     */
    S interpolate(S endState, double progress);

    /**
     * Returns a string representation suitable for saving presets to text files.
     */
    String toSaveString();
}

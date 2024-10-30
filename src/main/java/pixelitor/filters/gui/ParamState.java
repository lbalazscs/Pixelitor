/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
 * Captures the state of a {@link FilterParam} at a given moment
 * (like the "Memento" design pattern). If the {@link FilterParam}
 * is animatable, then this class also supports smooth transitions
 * between different states through interpolation.
 *
 * The self-bounded type parameter S ensures type safety when
 * interpolating between states: a parameter state can only be
 * interpolated with another state of the same concrete type.
 */
public interface ParamState<S extends ParamState<S>> extends Serializable {
    /**
     * Calculates a new interpolated state between this state and the
     * end state. The current state is the starting point (progress = 0.0)
     * and the end state represents the target (progress = 1.0).
     */
    S interpolate(S endState, double progress);

    /**
     * Returns a string representation suitable for saving this state to a text file.
     */
    String toSaveString();
}

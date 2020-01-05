/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import java.awt.Rectangle;

/**
 * The model for a filter parameter, which (unlike a button) holds
 * some value.
 *
 * For practical reasons implementations of this are also found
 * outside filters as models of GUI elements, but then only
 * a subset of their functionality is used.
 */
public interface FilterParam extends FilterSetting, Resettable {

    /**
     * Sets a random value without triggering the filter
     */
    void randomize();

    /**
     * A filter param can implement this to
     * adjust the maximum and default values
     * according to the size of the current image
     */
    void considerImageSize(Rectangle bounds);

    /**
     * Captures the state of this parameter into the returned
     * "memento" object.
     * Implemented only for parameters that can be animated.
     */
    ParamState<?> copyState();

    /**
     * Sets the internal state according to the given {@link ParamState}
     * Implemented only for parameters that can be animated.
     */
    void setState(ParamState<?> state);

    /**
     * True if the value can be interpolated in some useful way.
     * All implementing classes return either always true or always false.
     */
    boolean canBeAnimated();

    /**
     * Whether a filter parameter was configured to be
     * affected when the user presses "Randomize"
     */
    boolean ignoresRandomize();

    void setToolTip(String tip);
}

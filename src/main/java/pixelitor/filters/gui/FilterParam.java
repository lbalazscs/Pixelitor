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

package pixelitor.filters.gui;

import java.awt.Rectangle;

/**
 * A filter parameter that can be configured by the user
 */
public interface FilterParam extends FilterSetting, Resettable {

    /**
     * Sets a random value without triggering the filter
     */
    void randomize();

    void considerImageSize(Rectangle bounds);

    ParamState copyState();

    void setState(ParamState state);

    boolean canBeAnimated();

    boolean ignoresRandomize();

    void setToolTip(String tip);
}

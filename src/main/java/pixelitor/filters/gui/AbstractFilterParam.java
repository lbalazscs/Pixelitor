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

import java.util.Objects;

/**
 * A convenience parent class for filter parameter implementations.
 */
public abstract class AbstractFilterParam implements FilterParam {
    private final String name;
    protected ParamAdjustmentListener adjustmentListener;

    AbstractFilterParam(String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        this.adjustmentListener = listener;
    }

    @Override
    public String getName() {
        return name;
    }
}

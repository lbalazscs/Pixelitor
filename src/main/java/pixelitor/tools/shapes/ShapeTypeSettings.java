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

package pixelitor.tools.shapes;

import pixelitor.filters.gui.FilterParam;
import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.UserPreset;
import pixelitor.utils.Configurable;

import java.util.function.Consumer;

/**
 * The settings of a configurable {@link ShapeType}.
 */
public abstract class ShapeTypeSettings extends Configurable {
    public abstract void forEachParam(Consumer<FilterParam> consumer);

    void setAdjustmentListener(ParamAdjustmentListener listener) {
        forEachParam(param -> param.setAdjustmentListener(listener));
    }

    public void loadStateFrom(UserPreset preset) {
        forEachParam(param -> param.loadStateFrom(preset));
    }

    public void saveStateTo(UserPreset preset) {
        forEachParam(param -> param.saveStateTo(preset));
    }

    public abstract ShapeTypeSettings copy();
}


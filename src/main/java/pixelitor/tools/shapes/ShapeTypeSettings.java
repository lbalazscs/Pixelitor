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
import pixelitor.filters.gui.ParamState;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.utils.Configurable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The settings of a configurable {@link ShapeType}.
 */
public abstract class ShapeTypeSettings extends Configurable {
    public abstract List<FilterParam> getParams();

    @Override
    protected JPanel createConfigPanel() {
        return GUIUtils.arrangeVertically(getParams());
    }

    public List<ParamState<?>> copyState() {
        List<FilterParam> params = getParams();
        List<ParamState<?>> state = new ArrayList<>(params.size());
        for (FilterParam param : params) {
            state.add(param.copyState());
        }
        return state;
    }

    public void loadStateFrom(List<ParamState<?>> state) {
        List<FilterParam> params = getParams();
        for (int i = 0; i < params.size(); i++) {
            params.get(i).loadStateFrom(state.get(i), true);
        }
    }

    @Override
    protected void forEachParam(Consumer<FilterParam> consumer) {
        List<FilterParam> params = getParams();
        for (FilterParam param : params) {
            consumer.accept(param);
        }
    }
}


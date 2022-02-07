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

package pixelitor.utils;

import pixelitor.filters.gui.FilterParam;
import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.UserPreset;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * Abstract superclass for settings that can be configured using a cached JPanel
 */
public abstract class Configurable {
    private final Lazy<JPanel> configPanel = Lazy.of(this::createConfigPanel);

    public JPanel getConfigPanel() {
        return configPanel.get();
    }

    protected abstract JPanel createConfigPanel();

    protected abstract void forEachParam(Consumer<FilterParam> consumer);

    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        forEachParam(param -> param.setAdjustmentListener(listener));
    }

    public void loadStateFrom(UserPreset preset) {
        forEachParam(param -> param.loadStateFrom(preset));
    }

    public void saveStateTo(UserPreset preset) {
        forEachParam(param -> param.saveStateTo(preset));
    }
}

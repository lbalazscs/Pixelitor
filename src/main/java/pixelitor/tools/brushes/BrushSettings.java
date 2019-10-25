/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.tools.Tool;
import pixelitor.utils.Lazy;

import javax.swing.*;

/**
 * The settings of a brush.
 * This must be a separate class from the main brush class
 * because the settings are shared between different symmetry-instances
 * of the same brush type
 */
public abstract class BrushSettings {
    protected Tool tool;
    private final Lazy<JPanel> configPanel = Lazy.of(this::createConfigPanel);

    public JPanel getConfigPanel() {
        return configPanel.get();
    }

    protected abstract JPanel createConfigPanel();

    public void setTool(Tool tool) {
        this.tool = tool;
    }
}

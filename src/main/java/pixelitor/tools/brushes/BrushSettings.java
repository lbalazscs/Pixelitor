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

package pixelitor.tools.brushes;

import pixelitor.tools.Tool;
import pixelitor.utils.Lazy;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The settings of a brush.
 * This must be a separate from the main brush class
 * because the settings are shared between different
 * symmetry-instances of the same brush type
 */
public abstract class BrushSettings {
    protected Tool tool;
    private final Lazy<JPanel> configPanel = Lazy.of(this::createConfigPanel);
    private final List<AbstractBrush> brushes = new ArrayList<>(4);

    public JPanel getConfigPanel() {
        return configPanel.get();
    }

    protected abstract JPanel createConfigPanel();

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public void registerBrush(DabsBrush brush) {
        brushes.add(brush);

        assert brushes.size() <= 4;
    }

    public void unregisterBrush(DabsBrush brush) {
        brushes.remove(brush);
    }

    /**
     * Notify the brushes sharing this object that the settings have changed
     */
    protected void notifyBrushes() {
        for (AbstractBrush brush : brushes) {
            brush.settingsChanged();
        }
    }
}

/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.Configurable;

import java.util.ArrayList;
import java.util.List;

/**
 * The settings of a {@link Brush}.
 * Settings are shared between different symmetry-instances for
 * a given tool.
 */
public abstract class BrushSettings extends Configurable {
    protected Tool tool;

    // the symmetry brushes that share this settings object
    private final List<AbstractBrush> brushes = new ArrayList<>(4);

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public void registerBrush(AbstractBrush brush) {
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

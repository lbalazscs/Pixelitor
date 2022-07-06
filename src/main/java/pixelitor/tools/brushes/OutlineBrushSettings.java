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

package pixelitor.tools.brushes;

import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.FilterParam;

import javax.swing.*;
import java.awt.FlowLayout;
import java.util.function.Consumer;

public class OutlineBrushSettings extends BrushSettings {
    private static final String SPEED_TEXT = "Radius depends on mouse speed";
    private final BooleanParam dependsOnSpeedParam =
        new BooleanParam(SPEED_TEXT, true);
    private JComponent speedGUI;

    @Override
    protected void forEachParam(Consumer<FilterParam> consumer) {
        consumer.accept(dependsOnSpeedParam);
    }

    @Override
    protected JPanel createConfigPanel() {
        JPanel p = new JPanel(new FlowLayout());
        p.add(new JLabel(SPEED_TEXT));
        if (speedGUI == null) {
            speedGUI = dependsOnSpeedParam.createGUI("dependsOnSpeed");
        }
        p.add(speedGUI);

        return p;
    }

    public boolean dependsOnSpeed() {
        return dependsOnSpeedParam.isChecked();
    }
}

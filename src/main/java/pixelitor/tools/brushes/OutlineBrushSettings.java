/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import javax.swing.*;
import java.awt.FlowLayout;

public class OutlineBrushSettings implements BrushSettings {
    private static final boolean DEFAULT_SPEED_DEPENDENCE = true;

    private JCheckBox dependsOnSpeedCB;

    @Override
    public JPanel getConfigPanel() {
        JPanel p = new JPanel(new FlowLayout());
        p.add(new JLabel("Radius depends on mouse speed"));
        if (dependsOnSpeedCB == null) {
            dependsOnSpeedCB = new JCheckBox("", DEFAULT_SPEED_DEPENDENCE);
        }
        p.add(dependsOnSpeedCB);

        return p;
    }

    public boolean dependsOnSpeed() {
        if (dependsOnSpeedCB != null) {
            return dependsOnSpeedCB.isSelected();
        }
        return DEFAULT_SPEED_DEPENDENCE;
    }
}

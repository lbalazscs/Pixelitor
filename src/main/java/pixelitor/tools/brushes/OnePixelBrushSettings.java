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

/**
 * The settings of a {@link OnePixelBrush}
 */
public class OnePixelBrushSettings implements BrushSettings {
    private final JCheckBox aa = new JCheckBox();

    @Override
    public JPanel getConfigPanel() {
        JPanel p = new JPanel(new FlowLayout());
        p.add(new JLabel("Anti-aliasing"));
        p.add(aa);

        return p;
    }

    public boolean hasAA() {
        return aa.isSelected();
    }
}

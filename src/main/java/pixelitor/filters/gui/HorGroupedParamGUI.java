/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import javax.swing.*;

/**
 * A UI for {@link CompositeParam} models that arranges child
 * parameters horizontally.
 */
public class HorGroupedParamGUI extends JPanel implements ParamGUI {
    public HorGroupedParamGUI(CompositeParam model) {
        for (FilterParam child : model.getChildren()) {
            add(new JLabel(child.getName() + ":"));
            add(child.createGUI());
        }
    }

    @Override
    public void updateGUI() {

    }

    @Override
    public void setEnabled(boolean b) {
//        for (JComponent childGui : childGuis) {
//            childGui.setEnabled(b);
//        }
    }

    @Override
    public void setToolTip(String tip) {

    }

    @Override
    public int getNumLayoutColumns() {
        return 1;
    }
}

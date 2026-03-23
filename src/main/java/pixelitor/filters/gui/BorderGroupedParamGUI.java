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

import pixelitor.gui.utils.GridBagHelper;

import javax.swing.*;
import java.awt.GridBagLayout;

/**
 * A UI for {@link CompositeParam} models that arranges child
 * parameters vertically, within a common titled border.
 */
public class BorderGroupedParamGUI extends JPanel implements ParamGUI {
    public BorderGroupedParamGUI(CompositeParam model, FilterParam[] children) {
        setBorder(BorderFactory.createTitledBorder(model.getName()));

        setLayout(new GridBagLayout());
        GridBagHelper gbh = new GridBagHelper(this);

        for (FilterParam child : children) {
            gbh.addParam(child);
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

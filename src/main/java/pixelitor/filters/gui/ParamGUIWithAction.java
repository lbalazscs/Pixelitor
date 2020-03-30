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

package pixelitor.filters.gui;

import javax.swing.*;
import java.awt.FlowLayout;

import static java.awt.FlowLayout.LEFT;

/**
 * A {@link ParamGUI} with an added {@link FilterButtonModel}
 */
public class ParamGUIWithAction extends JPanel implements ParamGUI {
    private final ParamGUI paramGUI;

    public ParamGUIWithAction(ParamGUI paramGUI, FilterButtonModel action) {
        super(new FlowLayout(LEFT));

        this.paramGUI = paramGUI;

        add((JComponent) paramGUI);
        add(action.createGUI());
    }

    @Override
    public void updateGUI() {
        paramGUI.updateGUI();
    }

    @Override
    public void setToolTip(String tip) {
        paramGUI.setToolTip(tip);
    }

    @Override
    public int getNumLayoutColumns() {
        return paramGUI.getNumLayoutColumns();
    }
}

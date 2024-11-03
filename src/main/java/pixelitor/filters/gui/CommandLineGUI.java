/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.ParametrizedFilter;
import pixelitor.layers.Filterable;

import javax.swing.*;
import java.awt.BorderLayout;

/**
 * A specialized {@link ParametrizedFilterGUI} for filters that allow
 * the user to execute an external filtering command.
 */
public class CommandLineGUI extends ParametrizedFilterGUI {
    public CommandLineGUI(ParametrizedFilter filter, Filterable layer, boolean addShowOriginal, boolean reset) {
        super(filter, layer, addShowOriginal, reset);
    }

    @Override
    public JPanel createFilterParamsPanel(ParamSet paramSet) {
        // create a panel with border layout so that the box can be extended vertically
        JPanel p = new JPanel(new BorderLayout());
        JComponent gui = paramSet.getParams().getFirst().createGUI();
        p.add(gui, BorderLayout.CENTER);
        return p;
    }
}

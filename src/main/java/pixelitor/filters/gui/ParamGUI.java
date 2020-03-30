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

/**
 * The GUI for a {@link FilterParam}
 */
public interface ParamGUI {
    /**
     * The model state has been changed
     * and the GUI has to be updated accordingly
     */
    void updateGUI();

    void setEnabled(boolean b);

    void setToolTip(String tip);

    /**
     * Return the number of layout columns, either 1 or 2.
     * If 2 is returned, then a label based on the name is added
     * to the GUI.
     */
    int getNumLayoutColumns();
}

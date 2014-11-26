/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.tools;

import pixelitor.ImageComponent;

import javax.swing.*;
import java.awt.Cursor;
import java.awt.event.MouseEvent;

/**
 *
 */
public class LassoTool extends Tool {
//    private JComboBox typeCombo;

    public LassoTool() {
        super('l', "Lasso", "lasso_tool_icon.gif", "",
                Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR), false, true, false, ClipStrategy.FULL_AREA);
    }

    @Override
    public void initSettingsPanel() {
        toolSettingsPanel.add(new JLabel("Type:"));

//        SelectionType[] types = new SelectionType[]{SelectionType.POLYGONAL_LASSO, SelectionType.LASSO};
//
//        typeCombo = new JComboBox(types);
//        toolSettingsPanel.add(typeCombo);
    }

    @Override
    public void toolMousePressed(MouseEvent e, ImageComponent ic) {

    }

    @Override
    public void toolMouseDragged(MouseEvent e, ImageComponent ic) {

    }

    @Override
    public void toolMouseReleased(MouseEvent e, ImageComponent ic) {

    }
}

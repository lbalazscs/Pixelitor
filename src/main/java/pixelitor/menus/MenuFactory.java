/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
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

package pixelitor.menus;

import javax.swing.*;

public final class MenuFactory {

    /**
     * Utility class with static methods
     */
    private MenuFactory() {
    }

    public static void createMenuItem(Action a, KeyStroke keyStroke, JMenu parent, MenuEnableCondition whenToEnable) {
        JMenuItem menuItem = whenToEnable.getMenuItem(a);
        parent.add(menuItem);
        if (keyStroke != null) {
            menuItem.setAccelerator(keyStroke);
        }
    }

    public static void createMenuItem(Action action, KeyStroke keyStroke, JMenu parent) {
        createMenuItem(action, keyStroke, parent, MenuEnableCondition.THERE_IS_OPEN_IMAGE);
    }

}

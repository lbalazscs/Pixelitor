/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
package pixelitor.menus.view;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * An abstract action that either shows or hides something, depending on the current visibility
 */
public abstract class ShowHideAction extends AbstractAction {
    private final String showName;
    private final String hideName;
    private JMenuItem menuItem;

    protected ShowHideAction(String showName, String hideName) {
        this.showName = showName;
        this.hideName = hideName;
        //noinspection AbstractMethodCallInConstructor
        if (getVisibilityAtStartUp()) {
            this.putValue(AbstractAction.NAME, hideName);
        } else {
            this.putValue(AbstractAction.NAME, showName);
        }
    }

    public String getName() {
        return (String) getValue(AbstractAction.NAME);
    }

    private void setName(String newName) {
        this.putValue(AbstractAction.NAME, newName);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String newName;
        if (getCurrentVisibility()) {
            setVisibilityAction(false);
            newName = showName;
        } else {
            setVisibilityAction(true);
            newName = hideName;
        }
        setName(newName);
        if(menuItem != null) {
            menuItem.setName(newName);
        }
    }

    public abstract boolean getVisibilityAtStartUp();

    public abstract boolean getCurrentVisibility();

    public abstract void setVisibilityAction(boolean value);

    public void setMenuItem(JMenuItem menuItem) {
        this.menuItem = menuItem;
    }
}

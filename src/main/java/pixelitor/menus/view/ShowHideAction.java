/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import pixelitor.menus.NamedAction;

import java.awt.event.ActionEvent;

/**
 * An abstract action that either shows or hides something, depending on the current visibility
 */
public abstract class ShowHideAction extends NamedAction {
    private final String showName;
    private final String hideName;

    protected ShowHideAction(String showName, String hideName) {
        this.showName = showName;
        this.hideName = hideName;
        //noinspection AbstractMethodCallInConstructor
        if (getVisibilityAtStartUp()) {
            setHideName();
        } else {
            setShowName();
        }
    }

    public void setHideName() {
        setName(hideName);
    }

    public void setShowName() {
        setName(showName);
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
    }

    public abstract boolean getVisibilityAtStartUp();

    public abstract boolean getCurrentVisibility();

    public abstract void setVisibilityAction(boolean value);
}

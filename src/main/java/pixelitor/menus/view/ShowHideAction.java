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

package pixelitor.menus.view;

import pixelitor.menus.NamedAction;

import java.awt.event.ActionEvent;

/**
 * An abstract action that either shows or hides something,
 * depending on the current visibility
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
            setVisibility(false);
            newName = showName;
        } else {
            setVisibility(true);
            newName = hideName;
        }
        setName(newName);
    }

    /**
     * The name is updated automatically when the visibility
     * changes due to the direct menu action.
     * However, the visibility can change in indirect ways
     * (for example by resetting the workspace), and then this
     * must be called.
     */
    public void updateName(boolean newVisibility) {
        if (newVisibility) {
            setName(hideName);
        } else {
            setName(showName);
        }
    }

    public abstract boolean getVisibilityAtStartUp();

    public abstract boolean getCurrentVisibility();

    /**
     * Hides or shows the controlled GUI area
     */
    public abstract void setVisibility(boolean value);
}

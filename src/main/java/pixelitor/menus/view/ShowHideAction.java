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
package pixelitor.menus.view;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 *
 */
public abstract class ShowHideAction extends AbstractAction {
    private final String showName;
    private final String hideName;

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

    @Override
    public void actionPerformed(ActionEvent e) {
        if (getCurrentVisibility()) {
            setVisibilityAction(false);
            this.putValue(AbstractAction.NAME, showName);
        } else {
            setVisibilityAction(true);
            this.putValue(AbstractAction.NAME, hideName);
        }
    }

    public abstract boolean getVisibilityAtStartUp();

    public abstract boolean getCurrentVisibility();

    public abstract void setVisibilityAction(boolean value);

}

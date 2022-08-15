/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.utils.NamedAction;

import javax.swing.*;

/**
 * An abstract {@link Action} that either shows or hides something,
 * depending on the current visibility
 */
public abstract class ShowHideAction extends NamedAction.Checked {
    private final String showText;
    private final String hideText;

    protected ShowHideAction(String showText, String hideText) {
        this.showText = showText;
        this.hideText = hideText;

        //noinspection AbstractMethodCallInConstructor
        updateText(getStartupVisibility());
    }

    public void setHideText() {
        setText(hideText);
    }

    public void setShowText() {
        setText(showText);
    }

    @Override
    protected void onClick() {
        boolean currentVisibility = getCurrentVisibility();
        setVisibility(!currentVisibility);
        updateText(!currentVisibility);
    }

    /**
     * The name is updated via actionPerformed when the visibility
     * changes due to direct menu action.
     * In other cases this method can be called.
     */
    public void updateText(boolean newVisibility) {
        if (newVisibility) {
            setHideText();
        } else {
            setShowText();
        }
    }

    public abstract boolean getStartupVisibility();

    public abstract boolean getCurrentVisibility();

    /**
     * Hides or shows the controlled GUI area
     */
    public abstract void setVisibility(boolean value);
}

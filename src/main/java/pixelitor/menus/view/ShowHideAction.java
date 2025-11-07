/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.WorkSpace;
import pixelitor.gui.utils.NamedAction;
import pixelitor.utils.Texts;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * An abstract {@link Action} that toggles the visibility of something.
 */
public abstract class ShowHideAction extends NamedAction {
    private final String showText;
    private final String hideText;
    protected final WorkSpace workSpace;

    protected ShowHideAction(String showKey, String hideKey, WorkSpace workSpace) {
        this.showText = Texts.i18n(showKey);
        this.hideText = Texts.i18n(hideKey);
        this.workSpace = workSpace;

        //noinspection AbstractMethodCallInConstructor
        updateText(isVisible());
    }

    public void setHideText() {
        setText(hideText);
    }

    public void setShowText() {
        setText(showText);
    }

    @Override
    protected void onClick(ActionEvent e) {
        boolean currentVisibility = isVisible();
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

    public abstract boolean isVisible();

    /**
     * Hides or shows the controlled GUI area
     */
    public abstract void setVisibility(boolean value);
}

/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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

package pixelitor.tools;

import pixelitor.Composition;
import pixelitor.ImageDisplay;

import java.awt.Cursor;
import java.awt.event.MouseEvent;

/**
 * The move tool.
 */
public class MoveTool extends Tool {
    public MoveTool() {
        super('v', "Move", "move_tool_icon.gif",
                "drag to move the active layer, Alt-drag to move a duplicate of the active layer",
                Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR), false, true, true, ClipStrategy.IMAGE_ONLY);
    }

    @Override
    public void initSettingsPanel() {

    }

    @Override
    public void toolMousePressed(MouseEvent e, ImageDisplay ic) {
        ic.getComp().startTranslation(e.isAltDown());
    }

    @Override
    public void toolMouseDragged(MouseEvent e, ImageDisplay ic) {
        Composition c = ic.getComp();
        int relativeX = userDrag.getHorizontalDifference();
        int relativeY = userDrag.getVerticalDifference();
        c.moveActiveContentRelative(relativeX, relativeY, true);
    }

    @Override
    public void toolMouseReleased(MouseEvent e, ImageDisplay ic) {
        ic.getComp().endTranslation();
    }

    /**
     * Moves the active layer programmatically.
     */
    public static void move(Composition comp, int relativeX, int relativeY) {
        comp.startTranslation(false);
        comp.moveActiveContentRelative(relativeX, relativeY, false);
        comp.imageChanged(true, true);
        comp.endTranslation();
    }

}
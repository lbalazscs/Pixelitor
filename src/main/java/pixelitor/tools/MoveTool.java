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

package pixelitor.tools;

import pixelitor.Composition;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;

import javax.swing.*;
import java.awt.Cursor;
import java.awt.event.MouseEvent;

/**
 * The move tool.
 */
public class MoveTool extends Tool {
    public MoveTool() {
        super('v', "Move", "move_tool_icon.png",
                "drag to move the active layer, Alt-drag (or right-mouse-drag) to move a duplicate of the active layer. Shift-drag to constrain the movement.",
                Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR), false, true, true, ClipStrategy.IMAGE_ONLY);
    }

    @Override
    public void initSettingsPanel() {

    }

    @Override
    public void mousePressed(MouseEvent e, ImageComponent ic) {
        ic.getComp().startMovement(e.isAltDown() || SwingUtilities.isRightMouseButton(e));
    }

    @Override
    public void mouseDragged(MouseEvent e, ImageComponent ic) {
        Composition c = ic.getComp();
        double relativeX = userDrag.getDX();
        double relativeY = userDrag.getDY();
        c.moveActiveContentRelative(relativeX, relativeY);
    }

    @Override
    public void mouseReleased(MouseEvent e, ImageComponent ic) {
        ic.getComp().endMovement();
    }

    /**
     * Moves the active layer programmatically.
     */
    public static void move(Composition comp, int relativeX, int relativeY) {
        comp.startMovement(false);
        comp.moveActiveContentRelative(relativeX, relativeY);
        comp.endMovement();
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        Composition comp = ImageComponents.getActiveCompOrNull();
        if (comp != null) {
            move(comp, key.getMoveX(), key.getMoveY());
            return true;
        }
        return false;
    }
}
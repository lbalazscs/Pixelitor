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

package pixelitor.tools;

import pixelitor.Composition;
import pixelitor.gui.ImageComponents;
import pixelitor.utils.Cursors;

/**
 * The move tool.
 */
public class MoveTool extends DragTool {
    public MoveTool() {
        super('v', "Move", "move_tool_icon.png",
                "<b>drag</b> to move the active layer, <b>Alt-drag</b> (or <b>right-mouse-drag</b>) to move a duplicate of the active layer. <b>Shift-drag</b> to constrain the movement.",
                Cursors.MOVE, false, true, true, ClipStrategy.CANVAS);
    }

    @Override
    public void initSettingsPanel() {

    }

    @Override
    public void dragStarted(PMouseEvent e) {
        e.getComp().startMovement(e.isAltDown() || e.isRight());
    }

    @Override
    public void ongoingDrag(PMouseEvent e) {
        ImDrag imDrag = userDrag.toImDrag();
        double relX = imDrag.getDX();
        double relY = imDrag.getDY();
        e.getComp().moveActiveContentRelative(relX, relY);
    }

    @Override
    public void dragFinished(PMouseEvent e) {
        e.getComp().endMovement();
    }

    /**
     * Moves the active layer programmatically.
     */
    public static void move(Composition comp, int relX, int relY) {
        comp.startMovement(false);
        comp.moveActiveContentRelative(relX, relY);
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
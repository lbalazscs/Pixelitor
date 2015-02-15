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

package pixelitor.tools;

import pixelitor.ImageDisplay;
import pixelitor.utils.Dialogs;

import java.awt.Cursor;
import java.awt.event.MouseEvent;

import static pixelitor.tools.CloneTool.State.SOURCE_DEFINED;
import static pixelitor.tools.CloneTool.State.STARTED;

/**
 * The Clone Stamp tool
 */
public class CloneTool extends Tool {
    enum State {
        STARTED,
        SOURCE_DEFINED
    }

    private State state = STARTED;

    protected CloneTool() {
        super('k', "Clone", "clone_tool_icon.png",
                "Alt-click to select source, then paint with the copied pixels",
                Cursor.getDefaultCursor(), true, true, false, ClipStrategy.IMAGE_ONLY);
    }

    @Override
    public void initSettingsPanel() {

    }

    @Override
    public void toolMousePressed(MouseEvent e, ImageDisplay ic) {
        if(e.isAltDown()) {
            System.out.println("CloneTool::toolMousePressed: ALT DOWN");
            state = SOURCE_DEFINED;
        } else {
            System.out.println("CloneTool::toolMousePressed: NORMAL PRESS");
            if(state != SOURCE_DEFINED) {
                Dialogs.showErrorDialog("No source", "Define a source point first with Alt-Click");
            }
        }
    }

    @Override
    public void toolMouseDragged(MouseEvent e, ImageDisplay ic) {

    }

    @Override
    public void toolMouseReleased(MouseEvent e, ImageDisplay ic) {

    }
}

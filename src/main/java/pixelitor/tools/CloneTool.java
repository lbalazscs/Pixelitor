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
import pixelitor.tools.brushes.CloneBrush;
import pixelitor.tools.brushes.ImageBrushType;
import pixelitor.utils.BlendingModePanel;
import pixelitor.utils.Dialogs;

import java.awt.event.MouseEvent;

import static pixelitor.tools.CloneTool.State.SOURCE_DEFINED;
import static pixelitor.tools.CloneTool.State.STARTED;

/**
 * The Clone Stamp tool
 */
public class CloneTool extends BrushTool {
    enum State {
        STARTED,
        SOURCE_DEFINED
    }

    private State state = STARTED;
//    private CloneBrush cloneBrush;

    protected CloneTool() {
        super('k', "Clone", "clone_tool_icon.png",
                "Alt-click to select source, then paint with the copied pixels");
    }

    @Override
    public void initSettingsPanel() {
        addSizeSelector();

        blendingModePanel = new BlendingModePanel(true);
        toolSettingsPanel.add(blendingModePanel);
    }

    @Override
    protected void initBrush() {
        brush = new CloneBrush(ImageBrushType.SOFT);
    }

    @Override
    public void toolMousePressed(MouseEvent e, ImageDisplay ic) {
        int x = userDrag.getStartX();
        int y = userDrag.getStartY();

        CloneBrush cloneBrush = (CloneBrush) brush;

        if(e.isAltDown()) {
            state = SOURCE_DEFINED;
            cloneBrush.setSource(ic.getComp().getActiveImageLayer().getImage(), x, y);
        } else {
            if(state != SOURCE_DEFINED) {
                Dialogs.showErrorDialog("No source", "Define a source point first with Alt-Click");
            }
            cloneBrush.setDestination(x, y);

            // TODO refactor the reusable parts
            // super.toolMousePressed(e, ic);
        }
    }

//    @Override
//    public void toolMouseDragged(MouseEvent e, ImageDisplay ic) {
//
//    }
//
//    @Override
//    public void toolMouseReleased(MouseEvent e, ImageDisplay ic) {
//
//    }

    @Override
    protected boolean doColorPickerForwarding() {
        return false; // this tool uses Alt-click for source selection
    }

    @Override
    Symmetry getCurrentSymmetry() {
        return Symmetry.NONE;
    }

}

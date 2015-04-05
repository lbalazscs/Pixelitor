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

import com.bric.util.JVM;
import pixelitor.Build;
import pixelitor.ImageDisplay;
import pixelitor.tools.brushes.BrushAffectedArea;
import pixelitor.tools.brushes.CloneBrush;
import pixelitor.tools.brushes.ImageBrushType;
import pixelitor.utils.Dialogs;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static pixelitor.tools.CloneTool.State.CLONING;
import static pixelitor.tools.CloneTool.State.NO_SOURCE;
import static pixelitor.tools.CloneTool.State.SOURCE_DEFINED_FIRST_STROKE;

/**
 * The Clone Stamp tool
 */
public class CloneTool extends TmpLayerBrushTool {
    enum State {
        NO_SOURCE,
        SOURCE_DEFINED_FIRST_STROKE,
        CLONING
    }

    private State state = NO_SOURCE;
    private boolean sampleAllLayers = false;

    private CloneBrush cloneBrush;

    protected CloneTool() {
        super('k', "Clone", "clone_tool_icon.png",
                "Alt-click to select source, then paint with the copied pixels");
    }

    @Override
    public void initSettingsPanel() {
        addSizeSelector();

        addBlendingModePanel();

        toolSettingsPanel.addSeparator();

        JCheckBox alignedCB = new JCheckBox("Aligned", true);
        alignedCB.setName("alignedCB");
        toolSettingsPanel.add(alignedCB);
        alignedCB.addActionListener(e -> cloneBrush.setAligned(alignedCB.isSelected()));

        toolSettingsPanel.addSeparator();

        JCheckBox sampleAllLayersCB = new JCheckBox("Sample All Layers");
        sampleAllLayersCB.setName("sampleAllLayersCB");
        toolSettingsPanel.add(sampleAllLayersCB);
        sampleAllLayersCB.addActionListener(e -> sampleAllLayers = sampleAllLayersCB.isSelected());
    }

    @Override
    protected void initBrushVariables() {
        cloneBrush = new CloneBrush(ImageBrushType.SOFT);
        brush = new BrushAffectedArea(cloneBrush);
        brushAffectedArea = (BrushAffectedArea) brush;
    }

    @Override
    public void toolMousePressed(MouseEvent e, ImageDisplay ic) {
        int x = userDrag.getStartX();
        int y = userDrag.getStartY();

        if(e.isAltDown()) {
            setCloningSource(ic, x, y);
        } else {
            if (state == NO_SOURCE) {
                handleUndefinedSource(ic, x, y);
                return;
            }
            state = CLONING; // must be a new stroke after the source setting

            if (!withLine(e)) {  // when drawing with line, the destination should not change for mouse press
                cloneBrush.setCloningStartPoint(x, y);
            }

            super.toolMousePressed(e, ic);
        }
    }

    @Override
    public void toolMouseDragged(MouseEvent e, ImageDisplay ic) {
        if (state == CLONING) { // make sure that the first source-setting stroke does not clone
            super.toolMouseDragged(e, ic);
        }
    }

    private void handleUndefinedSource(ImageDisplay ic, int x, int y) {
        if (Build.CURRENT.isRobotTest()) {
            // special case: do not show dialogs for random robot tests,
            // just act as if this was an alt-click
            setCloningSource(ic, x, y);
        } else {
            String msg = "Define a source point first with Alt-Click.";
            if (JVM.isLinux) {
                msg += "\n(You might need to disable Alt-Click for window dragging in the window manager)";
            }
            Dialogs.showErrorDialog("No source", msg);
        }
    }

    protected void setCloningSource(ImageDisplay ic, int x, int y) {
        BufferedImage sourceImage;
        if (sampleAllLayers) {
            sourceImage = ic.getComp().getCompositeImage();
        } else {
            sourceImage = ic.getComp().getActiveImageLayer().getImage();
        }
        cloneBrush.setSource(sourceImage, x, y);
        state = SOURCE_DEFINED_FIRST_STROKE;
    }

    @Override
    protected boolean doColorPickerForwarding() {
        return false; // this tool uses Alt-click for source selection
    }

    @Override
    Symmetry getCurrentSymmetry() {
        return Symmetry.NONE;
    }
}

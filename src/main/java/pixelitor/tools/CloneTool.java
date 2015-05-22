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
import pixelitor.PixelitorWindow;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.tools.brushes.BrushAffectedArea;
import pixelitor.tools.brushes.CloneBrush;
import pixelitor.tools.brushes.CopyBrushType;
import pixelitor.utils.Dialogs;
import pixelitor.utils.GridBagHelper;
import pixelitor.utils.OKDialog;
import pixelitor.utils.ScalingMirror;

import javax.swing.*;
import java.awt.Cursor;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static pixelitor.tools.CloneTool.State.CLONING;
import static pixelitor.tools.CloneTool.State.NO_SOURCE;
import static pixelitor.tools.CloneTool.State.SOURCE_DEFINED_FIRST_STROKE;
import static pixelitor.utils.SliderSpinner.TextPosition.NONE;

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

    private final RangeParam scaleParam = new RangeParam("", 10, 400, 100, true, NONE);
    private final RangeParam rotationParam = new RangeParam("", -180, 180, 0, true, NONE);
    private final EnumParam<ScalingMirror> mirrorParam = new EnumParam<>("", ScalingMirror.class);

    protected CloneTool() {
        super('k', "Clone", "clone_tool_icon.png",
                "Alt-click to select source, then paint with the copied pixels",
                Cursor.getDefaultCursor());
    }

    @Override
    public void initSettingsPanel() {
        settingsPanel.addCopyBrushTypeSelector(
                CopyBrushType.SOFT,
                cloneBrush::typeChanged);

        addSizeSelector();

        addBlendingModePanel();

        settingsPanel.addSeparator();

        settingsPanel.addCheckBox("Aligned", true, "alignedCB",
                cloneBrush::setAligned);

        settingsPanel.addSeparator();

        settingsPanel.addCheckBox("Sample All Layers", false, "sampleAllLayersCB",
                selected -> sampleAllLayers = selected);

        settingsPanel.addSeparator();
        settingsPanel.addButton("Transform", e -> {
            JPanel p = new JPanel(new GridBagLayout());
            GridBagHelper gbh = new GridBagHelper(p);
            gbh.addLabelWithControl("Scale (%):", scaleParam.createGUI());
            gbh.addLabelWithControl("Rotate (Degrees):", rotationParam.createGUI());
            gbh.addLabelWithControl("Mirror:", mirrorParam.createGUI());
            new OKDialog(PixelitorWindow.getInstance(), p, "Clone Transform", "Close");
        });
    }

    @Override
    protected void initBrushVariables() {
        cloneBrush = new CloneBrush(getRadius(), CopyBrushType.SOFT);
        brush = new BrushAffectedArea(cloneBrush);
        brushAffectedArea = (BrushAffectedArea) brush;
    }

    @Override
    public void mousePressed(MouseEvent e, ImageDisplay ic) {
        int x = userDrag.getStartX();
        int y = userDrag.getStartY();

        if (e.isAltDown()) {
            setCloningSource(ic, x, y);
        } else {
            if (state == NO_SOURCE) {
                handleUndefinedSource(ic, x, y);
                return;
            }
            state = CLONING; // must be a new stroke after the source setting

            float scaleAbs = scaleParam.getValueAsPercentage();
            ScalingMirror mirror = (ScalingMirror) mirrorParam.getSelectedItem();
            cloneBrush.setScale(
                    mirror.getScaleX(scaleAbs),
                    mirror.getScaleY(scaleAbs));
            cloneBrush.setRotate(rotationParam.getValueInRadians());

            if (!withLine(e)) {  // when drawing with line, the destination should not change for mouse press
                cloneBrush.setCloningDestPoint(x, y);
            }

            super.mousePressed(e, ic);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e, ImageDisplay ic) {
        if (state == CLONING) { // make sure that the first source-setting stroke does not clone
            super.mouseDragged(e, ic);
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

    private void setCloningSource(ImageDisplay ic, int x, int y) {
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
    protected Symmetry getSymmetry() {
        return Symmetry.NONE;
    }
}

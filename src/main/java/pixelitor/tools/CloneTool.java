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

import com.bric.util.JVM;
import pixelitor.Composition;
import pixelitor.filters.gui.AddDefaultButton;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.OKDialog;
import pixelitor.layers.ImageLayer;
import pixelitor.tools.brushes.BrushAffectedArea;
import pixelitor.tools.brushes.CloneBrush;
import pixelitor.tools.brushes.CopyBrushType;
import pixelitor.utils.Messages;
import pixelitor.utils.ScalingMirror;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.Cursor;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Random;

import static pixelitor.gui.utils.SliderSpinner.TextPosition.NONE;
import static pixelitor.tools.CloneTool.State.CLONING;
import static pixelitor.tools.CloneTool.State.NO_SOURCE;
import static pixelitor.tools.CloneTool.State.SOURCE_DEFINED_FIRST_STROKE;

/**
 * The Clone Stamp tool
 */
public class CloneTool extends BlendingModeBrushTool {
    enum State {
        NO_SOURCE,
        SOURCE_DEFINED_FIRST_STROKE,
        CLONING
    }

    private State state = NO_SOURCE;
    private boolean sampleAllLayers = false;

    private CloneBrush cloneBrush;

    private final RangeParam scaleParam = new RangeParam("", 10, 100, 400, AddDefaultButton.YES, NONE);
    private final RangeParam rotationParam = new RangeParam("", -180, 0, 180, AddDefaultButton.YES, NONE);
    private final EnumParam<ScalingMirror> mirrorParam = new EnumParam<>("", ScalingMirror.class);

    protected CloneTool() {
        super('s', "Clone Stamp", "clone_tool_icon.png",
                "Alt-click (or right-click) to select the source, then paint with the copied pixels",
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
            if (RandomGUITest.isRunning()) {
                return;
            }

            JPanel p = new JPanel(new GridBagLayout());
            GridBagHelper gbh = new GridBagHelper(p);
            gbh.addLabelWithControl("Scale (%):", scaleParam.createGUI());
            gbh.addLabelWithControl("Rotate (Degrees):", rotationParam.createGUI());
            gbh.addLabelWithControl("Mirror:", mirrorParam.createGUI());
            toolDialog = new OKDialog(PixelitorWindow.getInstance(), p, "Clone Transform", "Close");
        });
    }

    @Override
    protected void initBrushVariables() {
        cloneBrush = new CloneBrush(getRadius(), CopyBrushType.SOFT);
        brush = new BrushAffectedArea(cloneBrush);
        brushAffectedArea = (BrushAffectedArea) brush;
    }

    @Override
    public void mousePressed(MouseEvent e, ImageComponent ic) {
        double x = userDrag.getStartX();
        double y = userDrag.getStartY();

        if (e.isAltDown() || SwingUtilities.isRightMouseButton(e)) {
            setCloningSource(ic, x, y);
        } else {
            boolean notWithLine = !withLine(e);

            if (state == NO_SOURCE) {
                handleUndefinedSource(ic, x, y);
                return;
            }
            startNewCloningStroke(x, y, notWithLine);

            super.mousePressed(e, ic);
        }
    }

    private void startNewCloningStroke(double x, double y, boolean notWithLine) {
        state = CLONING; // must be a new stroke after the source setting

        float scaleAbs = scaleParam.getValueAsPercentage();
        ScalingMirror mirror = mirrorParam.getSelected();
        cloneBrush.setScale(
                mirror.getScaleX(scaleAbs),
                mirror.getScaleY(scaleAbs));
        cloneBrush.setRotate(rotationParam.getValueInRadians());

        if (notWithLine) {  // when drawing with line, the destination should not change for mouse press
            cloneBrush.setCloningDestPoint(x, y);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e, ImageComponent ic) {
        if (state == CLONING) { // make sure that the first source-setting stroke does not clone
            super.mouseDragged(e, ic);
        }
    }

    private void handleUndefinedSource(ImageComponent ic, double x, double y) {
        if (RandomGUITest.isRunning()) {
            // special case: do not show dialogs for RandomGUITest,
            // just act as if this was an alt-click
            setCloningSource(ic, x, y);
        } else {
            String msg = "Define a source point first with Alt-Click (or with right-click).";
            if (JVM.isLinux) {
                msg += "\n(For Alt-Click you might need to disable Alt-Click for window dragging in the window manager)";
            }
            Messages.showError("No source", msg);
        }
    }

    private void setCloningSource(ImageComponent ic, double x, double y) {
        BufferedImage sourceImage;
        int dx = 0;
        int dy = 0;
        if (sampleAllLayers) {
            sourceImage = ic.getComp().getCompositeImage();
        } else {
            ImageLayer imageLayer = ic.getComp().getActiveMaskOrImageLayer();
            sourceImage = imageLayer.getImage();
            dx = -imageLayer.getTX();
            dy = -imageLayer.getTY();
        }
        cloneBrush.setSource(sourceImage, x + dx, y + dy);
        state = SOURCE_DEFINED_FIRST_STROKE;
    }

    @Override
    protected boolean doColorPickerForwarding() {
        return false; // this tool uses Alt-click for source selection
    }

    @Override
    protected Symmetry getSymmetry() {
        throw new UnsupportedOperationException("no symmetry");
    }

    @VisibleForTesting
    protected void setState(State state) {
        this.state = state;
    }

    @Override
    protected void prepareProgrammaticBrushStroke(ImageLayer layer, Point start) {
        super.prepareProgrammaticBrushStroke(layer, start);

        setupRandomSource(layer, start);
    }

    private void setupRandomSource(ImageLayer layer, Point start) {
        Composition comp = layer.getComp();

        int canvasWidth = comp.getCanvasWidth();
        int canvasHeight = comp.getCanvasHeight();
        Random rand = new Random();
        int sourceX = rand.nextInt(canvasWidth);
        int sourceY = rand.nextInt(canvasHeight);

        setCloningSource(comp.getIC(), sourceX, sourceY);
        startNewCloningStroke(start.x, start.y, true);
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        node.addStringChild("Brush", cloneBrush.getType().toString());
        node.addStringChild("State", state.toString());
        node.addBooleanChild("Sample All Layers", sampleAllLayers);
        node.addBooleanChild("Aligned", cloneBrush.isAligned());

        node.addFloatChild("Scale", scaleParam.getValueAsPercentage());
        node.addIntChild("Rotation", rotationParam.getValue());
        node.addStringChild("Mirror", mirrorParam.getSelected().toString());

        return node;
    }
}

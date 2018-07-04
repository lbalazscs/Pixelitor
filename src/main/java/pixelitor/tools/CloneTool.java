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

import com.bric.util.JVM;
import pixelitor.Composition;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.OKDialog;
import pixelitor.layers.Drawable;
import pixelitor.tools.brushes.BrushAffectedArea;
import pixelitor.tools.brushes.CloneBrush;
import pixelitor.tools.brushes.CopyBrushType;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Cursors;
import pixelitor.utils.Messages;
import pixelitor.utils.Mirror;
import pixelitor.utils.RandomUtils;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;

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
    private JDialog transformDialog;

    private CloneBrush cloneBrush;

    private final RangeParam scaleParam = new RangeParam("", 10, 100, 400, true, NONE);
    private final RangeParam rotationParam = new RangeParam("", -180, 0, 180, true, NONE);
    private final EnumParam<Mirror> mirrorParam = new EnumParam<>("", Mirror.class);

    protected CloneTool() {
        super('s', "Clone Stamp", "clone_tool_icon.png",
                "<b>Alt-click</b> (or <b>right-click</b>) to select the source, then <b>drag</b> to paint.",
                Cursors.CROSSHAIR);
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
        settingsPanel.addButton("Transform...", e -> {
            if (RandomGUITest.isRunning()) {
                return;
            }

            JPanel p = new JPanel(new GridBagLayout());
            GridBagHelper gbh = new GridBagHelper(p);
            gbh.addLabelWithControl("Scale (%):", scaleParam.createGUI());
            gbh.addLabelWithControl("Rotate (Degrees):", rotationParam.createGUI());
            gbh.addLabelWithControl("Mirror:", mirrorParam.createGUI());
            transformDialog = new OKDialog(PixelitorWindow.getInstance(), p, "Clone Transform", "Close");
        });
    }

    @Override
    protected void initBrushVariables() {
        cloneBrush = new CloneBrush(getRadius(), CopyBrushType.SOFT);
        brush = new BrushAffectedArea(cloneBrush);
        brushAffectedArea = (BrushAffectedArea) brush;
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        double x = e.getImX();
        double y = e.getImY();

        if (e.isAltDown() || e.isRight()) {
            setCloningSource(e.getIC(), x, y);
        } else {
            boolean notWithLine = !withLine(e);

            if (state == NO_SOURCE) {
                handleUndefinedSource(e.getIC(), x, y);
                return;
            }
            startNewCloningStroke(x, y, notWithLine);

            super.mousePressed(e);
        }
    }

    private void startNewCloningStroke(double x, double y, boolean notWithLine) {
        state = CLONING; // must be a new stroke after the source setting

        float scaleAbs = scaleParam.getValueAsPercentage();
        Mirror mirror = mirrorParam.getSelected();
        cloneBrush.setScale(
                mirror.getScaleX(scaleAbs),
                mirror.getScaleY(scaleAbs));
        cloneBrush.setRotate(rotationParam.getValueInRadians());

        if (notWithLine) {  // when drawing with line, the destination should not change for mouse press
            cloneBrush.setCloningDestPoint(x, y);
        }
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        if (state == CLONING) { // make sure that the first source-setting stroke does not clone
            super.mouseDragged(e);
        }
    }

    private void handleUndefinedSource(ImageComponent ic, double x, double y) {
        if (RandomGUITest.isRunning()) {
            // special case: do not show dialogs for RandomGUITest,
            // just act as if this was an alt-click
            setCloningSource(ic, x, y);
        } else {
            String msg = "<html>Define a source point first with <b>Alt-Click</b> or with <b>right-click</b>.";
            if (JVM.isLinux) {
                msg += "<br><br>(For <b>Alt-Click</b> you might need to disable " +
                        "<br><b>Alt-Click</b> for window dragging in the window manager)";
            }
            Messages.showError("No source point", msg);
        }
    }

    private void setCloningSource(ImageComponent ic, double x, double y) {
        BufferedImage sourceImage;
        int dx = 0;
        int dy = 0;
        if (sampleAllLayers) {
            sourceImage = ic.getComp().getCompositeImage();
        } else {
            Drawable dr = ic.getComp().getActiveDrawable();
            sourceImage = dr.getImage();
            dx = -dr.getTX();
            dy = -dr.getTY();
        }
        cloneBrush.setSource(sourceImage, x + dx, y + dy);
        state = SOURCE_DEFINED_FIRST_STROKE;
    }

    @Override
    public boolean doColorPickerForwarding() {
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
    protected void prepareProgrammaticBrushStroke(Drawable dr, PPoint start) {
        super.prepareProgrammaticBrushStroke(dr, start);

        setupRandomSource(dr, start);
    }

    private void setupRandomSource(Drawable dr, PPoint start) {
        Composition comp = dr.getComp();

        int canvasWidth = comp.getCanvasImWidth();
        int canvasHeight = comp.getCanvasImHeight();
        int sourceX = RandomUtils.nextInt(canvasWidth);
        int sourceY = RandomUtils.nextInt(canvasHeight);

        setCloningSource(comp.getIC(), sourceX, sourceY);
        startNewCloningStroke(start.getImX(), start.getImY(), true);
    }

    @Override
    protected void closeToolDialogs() {
        super.closeToolDialogs();
        GUIUtils.closeDialog(transformDialog);
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        node.addString("Brush", cloneBrush.getType().toString());
        node.addString("State", state.toString());
        node.addBoolean("Sample All Layers", sampleAllLayers);
        node.addBoolean("Aligned", cloneBrush.isAligned());

        node.addFloat("Scale", scaleParam.getValueAsPercentage());
        node.addInt("Rotation", rotationParam.getValue());
        node.addString("Mirror", mirrorParam.getSelected().toString());

        return node;
    }
}

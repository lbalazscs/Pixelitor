/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.layers.Drawable;
import pixelitor.tools.brushes.*;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.*;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static pixelitor.gui.GUIText.CLOSE_DIALOG;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.NONE;
import static pixelitor.tools.CloneTool.State.*;

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
        super("Clone Stamp", 'S', "clone_tool_icon.png",
                "<b>Alt-click</b> (or <b>right-click</b>) to select the source, " +
                    "then <b>drag</b> to paint. <b>Shift-click</b> to clone along a line.",
            Cursors.CROSSHAIR, false);
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
        settingsPanel.addCheckBox("Sample All Layers", false,
            "sampleAllLayersCB", selected -> sampleAllLayers = selected);

        settingsPanel.addSeparator();
        settingsPanel.addButton("Transform...", e -> transformButtonPressed(),
            "transformButton", "Transform while cloning");

        addLazyMouseDialogButton();
    }

    private void transformButtonPressed() {
        JPanel transformPanel = createTransformPanel();
        transformDialog = new DialogBuilder()
            .title("Clone Transform")
            .content(transformPanel)
            .notModal()
            .okText(CLOSE_DIALOG)
            .noCancelButton()
            .show();
    }

    private JPanel createTransformPanel() {
        var transformPanel = new JPanel(new GridBagLayout());
        var gbh = new GridBagHelper(transformPanel);
        gbh.addLabelAndControl("Scale (%):", scaleParam.createGUI("scale"));
        gbh.addLabelAndControl("Rotate (Degrees):", rotationParam.createGUI("rotate"));
        gbh.addLabelAndControl("Mirror:", mirrorParam.createGUI("mirror"));
        return transformPanel;
    }

    @Override
    protected void initBrushVariables() {
        cloneBrush = new CloneBrush(getRadius(), CopyBrushType.SOFT);
        affectedArea = new AffectedArea();
        brush = new AffectedAreaTracker(cloneBrush, affectedArea);
    }

    @Override
    protected void setLazyBrush() {
        if (lazyMouseCB.isSelected()) {
            lazyMouseBrush = new LazyMouseBrush(cloneBrush);
            brush = new AffectedAreaTracker(lazyMouseBrush, affectedArea);
            lazyMouse = true;
        } else {
            brush = new AffectedAreaTracker(cloneBrush, affectedArea);
            lazyMouseBrush = null;
            lazyMouse = false;
        }
    }

    @Override
    public void mousePressed(PMouseEvent e) {
//        double x = e.getImX();
//        double y = e.getImY();

        if (e.isAltDown() || e.isRight()) {
            setCloningSource(e);
        } else {
            if (state == NO_SOURCE) {
                handleUndefinedSource(e);
                return;
            }
            boolean lineConnect = e.isShiftDown() && brush.hasPrevious();
            startNewCloningStroke(e, lineConnect);

            super.mousePressed(e);
        }
    }

    private void startNewCloningStroke(PPoint p, boolean lineConnect) {
        state = CLONING;

        float scaleAbs = scaleParam.getPercentageValF();
        Mirror mirror = mirrorParam.getSelected();
        cloneBrush.setScale(
                mirror.getScaleX(scaleAbs),
                mirror.getScaleY(scaleAbs));
        cloneBrush.setRotate(rotationParam.getValueInRadians());

        // when drawing with line, a mouse press should not change the destination
        if (!lineConnect) {
            cloneBrush.setCloningDestPoint(p);
        }
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        // make sure that the first source-setting stroke does not clone
        if (state == CLONING) {
            super.mouseDragged(e);
        }
    }

    private void handleUndefinedSource(PMouseEvent e) {
        if (RandomGUITest.isRunning()) {
            // special case: do not show dialogs for RandomGUITest,
            // just act as if this was an alt-click
            setCloningSource(e);
        } else {
            String msg = "<html>Define a source point first with " +
                    "<b>Alt-Click</b> or with <b>right-click</b>.";
            if (JVM.isLinux) {
                msg += "<br><br>(For <b>Alt-Click</b> you might need to disable " +
                        "<br><b>Alt-Click</b> for window dragging in the window manager)";
            }
            Messages.showError("No source point", msg);
        }
    }

    private void setCloningSource(PPoint e) {
        var comp = e.getComp();
        BufferedImage sourceImage;
        int dx = 0;
        int dy = 0;
        if (sampleAllLayers) {
            sourceImage = comp.getCompositeImage();
        } else {
            Drawable dr = comp.getActiveDrawableOrThrow();
            sourceImage = dr.getImage();
            dx = -dr.getTx();
            dy = -dr.getTy();
        }
        double x = e.getImX();
        double y = e.getImY();
        cloneBrush.setSource(sourceImage, x + dx, y + dy);
        state = SOURCE_DEFINED_FIRST_STROKE;
    }

    @Override
    public boolean hasColorPickerForwarding() {
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
        var comp = dr.getComp();

        Rectangle canvasBounds = comp.getCanvasBounds();
        Point source = Rnd.nextPoint(canvasBounds);

        setCloningSource(PPoint.eagerFromIm(source, comp.getView()));
        startNewCloningStroke(start, true);
    }

    @Override
    protected void closeToolDialogs() {
        super.closeToolDialogs();
        GUIUtils.closeDialog(transformDialog, true);
    }

    @Override
    public DebugNode getDebugNode() {
        var node = super.getDebugNode();

        node.addString("brush", cloneBrush.getType().toString());
        node.addString("state", state.toString());
        node.addBoolean("sample all layers", sampleAllLayers);
        node.addBoolean("aligned", cloneBrush.isAligned());

        node.addFloat("scale", scaleParam.getPercentageValF());
        node.addInt("rotation", rotationParam.getValue());
        node.addString("mirror", mirrorParam.getSelected().toString());

        return node;
    }
}

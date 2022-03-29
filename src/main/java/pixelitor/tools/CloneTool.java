/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.layers.Drawable;
import pixelitor.tools.brushes.*;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Cursors;
import pixelitor.utils.Messages;
import pixelitor.utils.Mirror;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_ROUND;
import static pixelitor.gui.GUIText.CLOSE_DIALOG;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.NONE;
import static pixelitor.tools.CloneTool.State.*;

/**
 * The clone stamp tool.
 */
public class CloneTool extends BlendingModeBrushTool {
    private static final String ALIGNED_TEXT = "Aligned";
    private static final String SAMPLE_ALL_LAYERS_TEXT = "Sample All Layers";

    private EnumComboBoxModel<CopyBrushType> brushModel;
    private JCheckBox alignedCB;
    private JCheckBox sampleAllCB;

    enum State {
        NO_SOURCE,
        SOURCE_DEFINED_FIRST_STROKE,
        CLONING
    }

    private State state = NO_SOURCE;
    private boolean sampleAllLayers = false;

    private JButton showTransformDialogButton;
    private JDialog transformDialog;

    private CloneBrush cloneBrush;

    private final RangeParam scaleParam = new RangeParam(
        "Scale (%)", 10, 100, 400, true, NONE);
    private final RangeParam rotationParam = new RangeParam(
        "Rotate (Degrees)", -180, 0, 180, true, NONE);
    private final EnumParam<Mirror> mirrorParam = new EnumParam<>(
        "Mirror", Mirror.class);

    private boolean showUndefinedSourceDialog;

    protected CloneTool() {
        super("Clone Stamp", 'S',
            "<b>Alt-click</b> (or <b>right-click</b>) to select the source, " +
            "then <b>drag</b> to paint. <b>Shift-click</b> to clone along a line.",
            Cursors.CROSSHAIR, false);
    }

    @Override
    public void initSettingsPanel() {
        brushModel = settingsPanel.addCopyBrushTypeSelector(
            CopyBrushType.SOFT, cloneBrush::typeChanged);
        addSizeSelector();
        addBlendingModePanel();

        settingsPanel.addSeparator();
        alignedCB = settingsPanel.addCheckBox(ALIGNED_TEXT, true,
            "alignedCB", cloneBrush::setAligned);

        settingsPanel.addSeparator();
        sampleAllCB = settingsPanel.addCheckBox(SAMPLE_ALL_LAYERS_TEXT, false,
            "sampleAllLayersCB", selected -> sampleAllLayers = selected);

        settingsPanel.addSeparator();
        showTransformDialogButton = settingsPanel.addButton("Transform...",
            e -> transformButtonPressed(),
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
            .parentComponent(showTransformDialogButton)
            .show()
            .getDialog();
    }

    private JPanel createTransformPanel() {
        var transformPanel = new JPanel(new GridBagLayout());
        var gbh = new GridBagHelper(transformPanel);
        gbh.addLabelAndControl(scaleParam, "scale");
        gbh.addLabelAndControl(rotationParam, "rotate");
        gbh.addLabelAndControl(mirrorParam, "mirror");
        return transformPanel;
    }

    @Override
    protected void initBrushVariables() {
        cloneBrush = new CloneBrush(getRadius(), CopyBrushType.SOFT);
        affectedArea = new AffectedArea();
        brush = new AffectedAreaTracker(cloneBrush, affectedArea);
    }

    @Override
    protected void updateLazyBrushEnabledState() {
        if (lazyMouseEnabled.isChecked()) {
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
        if (e.isAltDown() || e.isRight()) {
            setCloningSource(e);
        } else {
            if (state == NO_SOURCE) {
                noSourceInMousePressed(e);
                return;
            }
            boolean lineConnect = e.isShiftDown() && brush.hasPrevious();
            startNewCloningStroke(e, lineConnect);

            super.mousePressed(e);
        }
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
        super.mouseReleased(e);
        if (showUndefinedSourceDialog) {
            showUndefinedSourceDialog = false;
            String msg = "<html>Define a source point first with " +
                         "<b>Alt-Click</b> or with <b>right-click</b>.";
            if (JVM.isLinux) {
                msg += "<br><br>(For <b>Alt-Click</b> you might need to disable " +
                       "<br><b>Alt-Click</b> for window dragging in the window manager)";
            }
            Messages.showError("No source point", msg, e.getView().getDialogParent());
        }
    }

    private void startNewCloningStroke(PPoint strokeStart, boolean lineConnect) {
        state = CLONING;

        float scaleAbs = scaleParam.getPercentageValF();
        Mirror mirror = mirrorParam.getSelected();
        cloneBrush.setScale(
            mirror.getScaleX(scaleAbs),
            mirror.getScaleY(scaleAbs));
        cloneBrush.setRotate(rotationParam.getValueInRadians());

        // when drawing with line, a mouse press should not change the destination
        if (!lineConnect) {
            cloneBrush.setCloningDestPoint(strokeStart);
        }
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        // make sure that the first source-setting stroke does not clone
        if (state == CLONING) {
            super.mouseDragged(e);
        }
    }

    private void noSourceInMousePressed(PMouseEvent e) {
        if (RandomGUITest.isRunning()) {
            // special case: don't show dialogs for RandomGUITest,
            // just act as if this was an alt-click
            setCloningSource(e);
        } else {
            // only set a flag that will be checked in mouseReleased,
            // otherwise the modal dialog swallows the mouse released event
            showUndefinedSourceDialog = true;
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
    protected void prepareProgrammaticBrushStroke(Drawable dr, PPoint strokeStart) {
        super.prepareProgrammaticBrushStroke(dr, strokeStart);

        PPoint randomPoint = dr.getComp().getRandomPointInCanvas();
        setCloningSource(randomPoint);

        startNewCloningStroke(strokeStart, false);
    }

    @Override
    protected void closeToolDialogs() {
        super.closeToolDialogs();
        GUIUtils.closeDialog(transformDialog, true);
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        super.saveStateTo(preset);

        preset.put("Brush Type", brushModel.getSelectedItem().toString());
        preset.putBoolean(ALIGNED_TEXT, alignedCB.isSelected());
        preset.putBoolean(SAMPLE_ALL_LAYERS_TEXT, sampleAllCB.isSelected());
        scaleParam.saveStateTo(preset);
        rotationParam.saveStateTo(preset);
        mirrorParam.saveStateTo(preset);
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        super.loadUserPreset(preset);

        brushModel.setSelectedItem(preset.getEnum("Brush Type", CopyBrushType.class));
        alignedCB.setSelected(preset.getBoolean(ALIGNED_TEXT));
        sampleAllCB.setSelected(preset.getBoolean(SAMPLE_ALL_LAYERS_TEXT));
        scaleParam.loadStateFrom(preset);
        rotationParam.loadStateFrom(preset);
        mirrorParam.loadStateFrom(preset);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        var node = super.createDebugNode(key);

        node.addAsString("brush", cloneBrush.getType());
        node.addAsString("state", state);
        node.addBoolean("sample all layers", sampleAllLayers);
        node.addBoolean("aligned", cloneBrush.isAligned());

        node.addFloat("scale", scaleParam.getPercentageValF());
        node.addInt("rotation", rotationParam.getValue());
        node.addAsString("mirror", mirrorParam.getSelected());

        return node;
    }

    @Override
    public Icon createIcon() {
        return new CloneToolIcon();
    }

    private static class CloneToolIcon extends Tool.ToolIcon {
        @Override
        public void paintIcon(Graphics2D g) {
            // based on clone_tool.svg

            // body
            Path2D shape = new Path2D.Float();
            shape.moveTo(0.9556584, 15.885808);
            shape.lineTo(4.6826134, 26.81291);
            shape.lineTo(23.317389, 26.81291);
            shape.lineTo(27.044344, 15.885808);
            shape.lineTo(14.029414, 15.885808);
            shape.lineTo(5.157098, 15.885808);
            shape.closePath();

            g.setStroke(new BasicStroke(1.4f, CAP_BUTT, JOIN_ROUND, 4));
            g.draw(shape);

            // triangle
            shape = new Path2D.Float();
            shape.moveTo(10.273045, 19.449001);
            shape.lineTo(14.0, 25.150095);
            shape.lineTo(17.726954, 19.449001);
            shape.closePath();

            g.fill(shape);
            g.draw(shape);

            // handle
            shape = new Path2D.Float();
            shape.moveTo(12.602392, 15.648266);
            shape.curveTo(12.602392, 15.648266, 8.388771, 10.379845, 8.438116, 6.145397);
            shape.curveTo(8.471819, 3.2532463, 10.364073, 1.4242454, 14.003829, 1.4242454);
            shape.curveTo(17.643581, 1.4242454, 19.590433, 3.2958915, 19.574783, 6.2119613);
            shape.curveTo(19.552193, 10.422222, 15.397608, 15.648266, 15.397608, 15.648266);

            g.draw(shape);
        }
    }
}

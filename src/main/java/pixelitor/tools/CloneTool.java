/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static pixelitor.gui.GUIText.CLOSE_DIALOG;
import static pixelitor.gui.utils.SliderSpinner.LabelPosition.NONE;
import static pixelitor.tools.CloneTool.State.CLONING;
import static pixelitor.tools.CloneTool.State.NO_SOURCE;
import static pixelitor.tools.CloneTool.State.SOURCE_DEFINED;

/**
 * The clone stamp tool.
 */
public class CloneTool extends BlendingModeBrushTool {
    private static final String ALIGNED_TEXT = "Aligned";
    private static final String SAMPLE_ALL_LAYERS_TEXT = "Sample All Layers";

    private EnumComboBoxModel<CopyBrushType> brushModel;
    private JCheckBox alignedCB;
    private JCheckBox sampleAllCB;

    // the current state of the clone tool
    public enum State {
        NO_SOURCE,
        SOURCE_DEFINED,
        CLONING
    }
    private State state = NO_SOURCE;

    private boolean sampleAllLayers = false;
    private boolean showUndefinedSourceDialog = false;

    private CloneBrush cloneBrush;

    private JButton showTransformDialogButton;
    private JDialog transformDialog;

    private final RangeParam scaleParam = new RangeParam(
        "Scale (%)", 10, 100, 400, true, NONE);
    private final RangeParam rotationParam = new RangeParam(
        "Rotate (Degrees)", -180, 0, 180, true, NONE);
    private final EnumParam<Mirror> mirrorParam = new EnumParam<>(
        "Mirror", Mirror.class);

    protected CloneTool() {
        super("Clone Stamp", 'S',
            "<b>Alt-click</b> (or <b>right-click</b>) to select the source, " +
                "then <b>drag</b> to paint. <b>Shift-click</b> to clone along a line.",
            Cursors.CROSSHAIR, false);
    }

    @Override
    public void initSettingsPanel(ResourceBundle resources) {
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
            e -> showTransformDialog(),
            "transformButton", "Transform while cloning");
        addLazyMouseDialogButton();
    }

    private void showTransformDialog() {
        if (transformDialog != null) {
            // call this even if it's already visible to set its location
            GUIUtils.showDialog(transformDialog, showTransformDialogButton);
            return;
        }

        transformDialog = new DialogBuilder()
            .title("Clone Transform")
            .content(createTransformPanel())
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
    protected void updateLazyMouseEnabledState() {
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
            // don't call super.mousePressed, as source setting shouldn't draw
        } else {
            if (state == NO_SOURCE) {
                handleNoSourceInMousePressed(e);
                return; // prevent further processing
            }
            boolean lineConnect = e.isShiftDown() && brush.hasPrevious();
            startCloningStroke(e, lineConnect);

            super.mousePressed(e);
        }
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
        super.mouseReleased(e);

        // show the delayed error dialog if cloning was attempted without a source
        if (showUndefinedSourceDialog) {
            showUndefinedSourceDialog = false;
            String msg = "<html>Define a source point first with " +
                "<b>Alt-Click</b> or with <b>right-click</b>.";
            if (JVM.isLinux) {
                msg += "<br><br>(For <b>Alt-Click</b> you might need to disable " +
                    "<br><b>Alt-Click</b> for window dragging in the window manager)";
            }
            Messages.showError("No Source Point", msg, e.getView().getDialogParent());
        }
    }

    // configures the brush for a new cloning stroke
    private void startCloningStroke(PPoint strokeStart, boolean lineConnect) {
        setState(CLONING);

        // apply transform settings to the brush
        double scaleAbs = scaleParam.getPercentage();
        Mirror mirror = mirrorParam.getSelected();
        cloneBrush.setScale(
            mirror.getScaleX(scaleAbs),
            mirror.getScaleY(scaleAbs));
        cloneBrush.setRotationAngle(rotationParam.getValueInRadians());

        // when drawing with line, a mouse press should not change the destination
        if (!lineConnect) {
            cloneBrush.setCloningDestPoint(strokeStart);
        }
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        if (state == CLONING) {
            super.mouseDragged(e);
        }
        // otherwise, ignore drags (e.g., if user is dragging after Alt-clicking source)
    }

    // handles the case where user tries to clone without setting a source
    private void handleNoSourceInMousePressed(PMouseEvent e) {
        if (RandomGUITest.isRunning()) {
            // special case for testing: act as if source was set at the click point
            setCloningSource(e);
        } else {
            // set flag to show dialog on mouse release
            // (otherwise the modal dialog swallows the mouse released event)
            showUndefinedSourceDialog = true;
        }
    }

    // sets the source point for cloning based on the event coordinates
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
        cloneBrush.setSource(sourceImage, e.getImX() + dx, e.getImY() + dy);
        setState(SOURCE_DEFINED);
    }

    @Override
    public boolean hasColorPickerForwarding() {
        return false; // this tool uses Alt-click for source selection
    }

    @Override
    protected Symmetry getSymmetry() {
        throw new UnsupportedOperationException("no symmetry");
    }

    protected void setState(State state) {
        this.state = state;
    }

    @Override
    protected void prepareProgrammaticBrushStroke(Drawable dr, PPoint strokeStart) {
        super.prepareProgrammaticBrushStroke(dr, strokeStart);

        PPoint randomPoint = dr.getComp().genRandomPointInCanvas();
        setCloningSource(randomPoint);

        startCloningStroke(strokeStart, false);
    }

    @Override
    protected void closeAllDialogs() {
        super.closeAllDialogs();
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
        DebugNode node = super.createDebugNode(key);

        node.addAsString("brush", cloneBrush.getType());
        node.addAsString("state", state);
        node.addBoolean("sample all layers", sampleAllLayers);
        node.addBoolean("aligned", cloneBrush.isAligned());

        node.addDouble("scale", scaleParam.getPercentage());
        node.addInt("rotation", rotationParam.getValue());
        node.addAsString("mirror", mirrorParam.getSelected());

        return node;
    }

    @Override
    public Consumer<Graphics2D> createIconPainter() {
        return ToolIcons::paintCloneIcon;
    }
}

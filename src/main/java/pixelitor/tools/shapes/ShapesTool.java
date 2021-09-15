/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.shapes;

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.filters.gui.EffectsParam;
import pixelitor.filters.gui.StrokeParam;
import pixelitor.filters.gui.StrokeSettings;
import pixelitor.filters.painters.AreaEffects;
import pixelitor.gui.GUIText;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.View;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.PAction;
import pixelitor.history.History;
import pixelitor.history.PixelitorEdit;
import pixelitor.layers.Drawable;
import pixelitor.menus.DrawableAction;
import pixelitor.tools.DragTool;
import pixelitor.tools.DragToolState;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.shapes.history.ConvertShapeToSelectionEdit;
import pixelitor.tools.shapes.history.CreateBoxedShapeEdit;
import pixelitor.tools.transform.TransformBox;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.DragDisplayType;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;
import pixelitor.utils.Lazy;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.EnumMap;
import java.util.Map;

import static pixelitor.Composition.UpdateActions.REPAINT;
import static pixelitor.colors.FgBgColors.setBGColor;
import static pixelitor.colors.FgBgColors.setFGColor;
import static pixelitor.tools.DragToolState.*;
import static pixelitor.tools.shapes.TwoPointPaintType.FOREGROUND;
import static pixelitor.tools.shapes.TwoPointPaintType.NONE;

/**
 * The shapes tool.
 */
public class ShapesTool extends DragTool {
    // history edit names for the shape editing
    public static final String CHANGE_SHAPE_FILL = "Change Shape Fill";
    public static final String CHANGE_SHAPE_STROKE = "Change Shape Stroke";
    public static final String CHANGE_SHAPE_STROKE_SETTINGS = "Change Shape Stroke Settings";
    public static final String CHANGE_SHAPE_EFFECTS = "Change Shape Effects";
    public static final String CHANGE_SHAPE_TYPE = "Change Shape Type";
    public static final String CHANGE_SHAPE_COLORS = "Change Shape Colors";

    private final Map<ShapeType, ShapeTypeSettings> defaultShapeTypeSettings;

    private boolean regenerateShape = true;

    private final EnumComboBoxModel<ShapeType> typeModel
        = new EnumComboBoxModel<>(ShapeType.class);
    private final EnumComboBoxModel<TwoPointPaintType> fillPaintModel
        = new EnumComboBoxModel<>(TwoPointPaintType.class);
    private final EnumComboBoxModel<TwoPointPaintType> strokePaintModel
        = new EnumComboBoxModel<>(TwoPointPaintType.class);

    private final StrokeParam strokeParam = new StrokeParam("");

    private JButton showShapeSettingsButton;
    private final Action shapeSettingsAction = new PAction("Settings...") {
        @Override
        public void onClick() {
            showShapeSettingsDialog();
        }
    };

    // During a single mouse drag, only one stroke should be created
    // This is particularly important for "random shape"
    private final Lazy<Stroke> stroke = Lazy.of(strokeParam::createStroke);
    private final EffectsParam effectsParam = new EffectsParam("");

    private final JComboBox<TwoPointPaintType> fillPaintCombo
        = createFillPaintCombo();
    private final JComboBox<TwoPointPaintType> strokePaintCombo
        = createStrokePaintCombo();

    private JDialog shapeSettingsDialog;

    private JButton showStrokeDialogButton;
    private Action strokeSettingsAction;
    private JDialog strokeSettingsDialog;

    private JButton showEffectsDialogButton;
    private JDialog effectsDialog;

    private StyledShape styledShape;
    private TransformBox transformBox;

    private DragToolState state = NO_INTERACTION;

    private final Action convertToSelectionAction = new PAction("Convert to Selection") {
        @Override
        public void onClick() {
            convertToSelection();
        }
    };

    public ShapesTool() {
        super("Shapes", 'U', "shapes_tool.png",
            "<b>drag</b> to draw a shape. " +
                "Hold <b>Alt</b> down to drag from the center. " +
                "Hold <b>SPACE</b> down while drawing to move the shape. ",
            Cursors.DEFAULT, false);
        spaceDragStartPoint = true;
        convertToSelectionAction.setEnabled(false);
        defaultShapeTypeSettings = new EnumMap<>(ShapeType.class);
    }

    @Override
    public void initSettingsPanel() {
        strokeParam.setAdjustmentListener(() -> guiChanged(CHANGE_SHAPE_STROKE_SETTINGS));
        effectsParam.setAdjustmentListener(() -> guiChanged(CHANGE_SHAPE_EFFECTS));

        JComboBox<ShapeType> shapeTypeCB = createShapeTypeCombo();
        settingsPanel.addComboBox("Shape:", shapeTypeCB, "shapeTypeCB");
        showShapeSettingsButton = settingsPanel.addButton(shapeSettingsAction, "shapeSettingsButton",
            "Configure the selected shape");

        settingsPanel.addSeparator();
        settingsPanel.addComboBox("Fill:", fillPaintCombo, "fillPaintCB");
        settingsPanel.addComboBox("Stroke:", strokePaintCombo, "strokePaintCB");

        strokeSettingsAction = new PAction("Stroke Settings...") {
            @Override
            public void onClick() {
                initAndShowStrokeSettingsDialog();
            }
        };
        showStrokeDialogButton = settingsPanel.addButton(strokeSettingsAction,
            "strokeSettingsButton",
            "Configure the stroke");

        showEffectsDialogButton = settingsPanel.addButton("Effects...",
            e -> showEffectsDialog(),
            "effectsButton", "Configure the effects");

        settingsPanel.addButton(convertToSelectionAction, "convertToSelection",
            "Convert the active shape to a selection");

        fillPaintModel.setSelectedItem(FOREGROUND);
        updateStrokeEnabledState();
    }

    private void guiChanged(String editName) {
        if (regenerateShape) {
            regenerateShape(editName);
        }
        updateStrokeEnabledState();
        if (editName.equals(CHANGE_SHAPE_TYPE)) {
            boolean hasSettings = getSelectedType().hasSettings();
            shapeSettingsAction.setEnabled(hasSettings);
            if (!hasSettings) {
                closeShapeSettingsDialog();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private JComboBox<TwoPointPaintType> createFillPaintCombo() {
        return GUIUtils.createComboBox(fillPaintModel,
            e -> guiChanged(CHANGE_SHAPE_FILL));
    }

    @SuppressWarnings("unchecked")
    private JComboBox<TwoPointPaintType> createStrokePaintCombo() {
        return GUIUtils.createComboBox(strokePaintModel,
            e -> guiChanged(CHANGE_SHAPE_STROKE));
    }

    @SuppressWarnings("unchecked")
    private JComboBox<ShapeType> createShapeTypeCombo() {
        return GUIUtils.createComboBox(typeModel,
            e -> guiChanged(CHANGE_SHAPE_TYPE));
    }

    public ShapeType getSelectedType() {
        return typeModel.getSelectedItem();
    }

    public ShapeTypeSettings getDefaultSettingsFor(ShapeType shapeType) {
        return defaultShapeTypeSettings.computeIfAbsent(shapeType,
            ShapeType::createDefaultSettings);
    }

    public TwoPointPaintType getSelectedFillPaint() {
        return fillPaintModel.getSelectedItem();
    }

    public TwoPointPaintType getSelectedStrokePaint() {
        return strokePaintModel.getSelectedItem();
    }

    public AreaEffects getEffects() {
        return effectsParam.getEffects();
    }

    public Stroke getStroke() {
        return stroke.get();
    }

    public StrokeSettings getStrokeSettings() {
        return strokeParam.copyState();
    }

    public void invalidateStroke() {
        stroke.invalidate();
    }

    private JDialog createStrokeSettingsDialog() {
        DialogBuilder builder = new DialogBuilder()
            .owner(PixelitorWindow.get())
            .notModal();
        strokeParam.configureSettingsDialog(builder);
        return builder.build();
    }

    public StrokeParam getStrokeParam() {
        return strokeParam;
    }

    private void setGUIDefaultsFrom(StyledShape styledShape) {
        // as this is used as part of undo/redo, don't regenerate the shape
        regenerateShape = false;
        try {
            // the shape target can't change for a styled shape edit
            typeModel.setSelectedItem(styledShape.getShapeType());
            fillPaintModel.setSelectedItem(styledShape.getFillPaintType());
            strokePaintModel.setSelectedItem(styledShape.getStrokePaintType());
            strokeParam.loadStateFrom(styledShape.getStrokeSettings(), false);
            effectsParam.setEffects(styledShape.getEffects());
        } finally {
            regenerateShape = true;
        }
    }

    @Override
    public void dragStarted(PMouseEvent e) {
        if (state == TRANSFORM) {
            assert transformBox != null;
            assert styledShape != null;
            if (transformBox.processMousePressed(e)) {
                return;
            }
            // if pressed outside the transform box,
            // finish the existing shape
            rasterizeShape(e.getComp());
        }

        // if this method didn't return yet, start a new shape
        styledShape = new StyledShape(this);
        setState(INITIAL_DRAG);
    }

    @Override
    public void ongoingDrag(PMouseEvent e) {
        if (state == TRANSFORM) {
            assert transformBox != null;
            assert styledShape != null;
            if (transformBox.processMouseDragged(e)) {
                return;
            }
            // we can get here if we entered the TRANSFORM state
            // while dragging another shape
            return;
        }

        assert state == INITIAL_DRAG : "state = " + state;

        updateStyledShapeFromDrag(e);

        // This will trigger paintOverActiveLayer,
        // therefore the continuous drawing of the shape.
        // It repaints the whole image because
        // some shapes extend beyond their drag rectangle.
        e.getComp().update(REPAINT);
    }

    @Override
    public void dragFinished(PMouseEvent e) {
        if (state == TRANSFORM) {
            assert transformBox != null;
            assert styledShape != null;
            if (transformBox.processMouseReleased(e)) {
                return;
            }
            // we can get here if we entered the TRANSFORM state
            // while dragging another shape
            return;
        }

        if (drag.isClick()) {
            // cancel the shape started in dragStarted
            styledShape = null;
            setState(NO_INTERACTION);
            return;
        }

        if (styledShape == null) {
            // this can happen when getting a fake mouse released event
            // because the user started another tool via keyboard
            setState(NO_INTERACTION);
            return;
        }

        updateStyledShapeFromDrag(e);

        transformBox = styledShape.createBox(drag, e.getView());
        if (transformBox == null) {
            // The box could not be created.
            // Cancel just as for empty clicks.
            styledShape = null;
            setState(NO_INTERACTION);
            e.getView().repaint();
            return;
        }

        e.getView().repaint();
        setState(TRANSFORM);
        History.add(new CreateBoxedShapeEdit(e.getComp(), styledShape, transformBox));

        invalidateStroke();
    }

    private void updateStyledShapeFromDrag(PMouseEvent e) {
        if (styledShape != null) {
            drag.setStartFromCenter(e.isAltDown());
            drag.setEquallySized(e.isShiftDown());
            styledShape.updateFromDrag(drag);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e, View view) {
        if (state == TRANSFORM) {
            transformBox.mouseMoved(e);
        }
    }

    @Override
    public void altPressed() {
        if (!altDown && state == INITIAL_DRAG && drag.isDragging()) {
            drag.setStartFromCenter(true);

            assert styledShape != null;
            styledShape.updateFromDrag(drag);

            var comp = OpenImages.getActiveComp();
            comp.update(REPAINT);
        }
        altDown = true;
    }

    @Override
    public void altReleased() {
        if (state == INITIAL_DRAG && drag.isDragging()) {
            drag.setStartFromCenter(false);

            assert styledShape != null;
            styledShape.updateFromDrag(drag);

            var comp = OpenImages.getActiveComp();
            comp.update(REPAINT);
        }
        altDown = false;
    }

    @Override
    public void escPressed() {
        // pressing Esc should work similarly to the Gradient Tool,
        // or to clicking outside the transform box:
        // the handles disappear, but the effect remains
        if (state == TRANSFORM) {
            OpenImages.onActiveComp(this::rasterizeShape);
        }
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        if (transformBox != null) {
            View view = OpenImages.getActiveView();
            assert view != null;

            transformBox.arrowKeyPressed(key, view);
            return true;
        }
        return false;
    }

    private void setState(DragToolState newState) {
        state = newState;

        assert state.isOK(this);

        convertToSelectionAction.setEnabled(newState == TRANSFORM);
    }

    /**
     * After this method the shape becomes part of the {@link Drawable}'s
     * pixels (before it was only drawn above it).
     */
    private void rasterizeShape(Composition comp) {
        assert transformBox != null;
        assert styledShape != null;

        styledShape.rasterizeTo(comp, transformBox, this);

        styledShape = null;
        transformBox = null;
        setState(NO_INTERACTION);
    }

    private void regenerateShape(String editName) {
        if (styledShape != null) {
            assert transformBox != null;

            DrawableAction.run(editName,
                dr -> styledShape.regenerate(transformBox, this, editName));
        }
    }

    private void updateStrokeEnabledState() {
        enableStrokeSettings(getSelectedStrokePaint() != NONE);
    }

    @Override
    public void paintOverActiveLayer(Graphics2D g) {
        // updates the shape continuously while drawing
        if (state == INITIAL_DRAG) {
            if (drag.isClick()) {
                return;
            }
            styledShape.paint(g);
        } else if (state == TRANSFORM) {
            assert transformBox != null;
            styledShape.paint(g);
        }
    }

    @Override
    public void paintOverImage(Graphics2D g, Composition comp) {
        if (state == INITIAL_DRAG) {
            // paint the drag display for the initial drag
            super.paintOverImage(g, comp);
        } else if (state == TRANSFORM) {
            assert transformBox != null;
            assert styledShape != null;
            transformBox.paint(g);
        }
    }

    @Override
    public DragDisplayType getDragDisplayType() {
        assert state == INITIAL_DRAG : "state = " + state;
        return getSelectedType().getDragDisplayType();
    }

    public boolean shouldDrawOverLayer() {
        return state == INITIAL_DRAG || state == TRANSFORM;
    }

    private void enableStrokeSettings(boolean b) {
        strokeSettingsAction.setEnabled(b);

        if (!b) {
            closeStrokeDialog();
        }
    }

    private void convertToSelection() {
        assert state == TRANSFORM : "state = " + state;

        Shape shape = styledShape.getShapeForSelection();

        var comp = OpenImages.getActiveComp();

        PixelitorEdit selectionEdit = comp.changeSelection(shape);
        if (selectionEdit == null) {
            Dialogs.showInfoDialog("No Selection",
                "No selection was created because the shape is outside the canvas.");
            return;
        }

        History.add(new ConvertShapeToSelectionEdit(
            comp, transformBox, styledShape, selectionEdit));

        resetInitialState();
        Tools.SELECTION.activate();
    }

    @Override
    public void fgBgColorsChanged() {
        regenerateShape(CHANGE_SHAPE_COLORS);
    }

    @Override
    public void coCoordsChanged(View view) {
        if (transformBox != null) {
            transformBox.coCoordsChanged(view);
        }
    }

    @Override
    public void imCoordsChanged(AffineTransform at, Composition comp) {
        if (transformBox != null) {
            transformBox.imCoordsChanged(at, comp);
        }
    }

    @Override
    public void resetInitialState() {
        // true, for example, when the initial box creation is undone
        boolean hadShape = styledShape != null;

        transformBox = null;
        styledShape = null;
        setState(NO_INTERACTION);

        OpenImages.onActiveComp(comp -> {
            if (hadShape) {
                comp.update();
            } else {
                comp.repaint();
            }
        });
    }

    @Override
    public void compReplaced(Composition newComp, boolean reloaded) {
        if (reloaded) {
            resetInitialState();
        }
    }

    /**
     * Restores a previously removed transform box as part of an undo/redo operation
     */
    public void restoreBox(StyledShape shape, TransformBox box) {
        // at this point we could have an active StyledShape, if
        // an undo happened while a second shape was dragged

        styledShape = shape;
        transformBox = box;
        setState(TRANSFORM);
        OpenImages.getActiveComp().update();
    }

    public StyledShape getStyledShape() {
        return styledShape;
    }

    public TransformBox getTransformBox() {
        return transformBox;
    }

    public Map<ShapeType, ShapeTypeSettings> getDefaultShapeTypeSettings() {
        return defaultShapeTypeSettings;
    }

    // used only for undo/redo
    public void setStyledShape(StyledShape styledShape) {
        assert state == TRANSFORM : "state = " + state;

        this.styledShape = styledShape;
        transformBox.replaceOwner(styledShape);

        setFGColor(styledShape.getFgColor(), false);
        setBGColor(styledShape.getBgColor(), false);

        setGUIDefaultsFrom(styledShape);
    }

    @Override
    protected void toolEnded() {
        super.toolEnded();

        finalizeBox();

        resetInitialState();
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        if (oldView != null) {
            finalizeBox(oldView.getComp());
        }

        super.viewActivated(oldView, newView);
    }

    @Override
    public void forceFinish() {
        finalizeBox();
    }

    private void finalizeBox() {
        if (transformBox != null) {
            Composition comp = OpenImages.getActiveComp();
            finalizeBox(comp);
        }
    }

    private void finalizeBox(Composition comp) {
        if (transformBox != null) {
            assert styledShape != null;
            assert state == TRANSFORM : "state = " + state;
            rasterizeShape(comp);
        }
    }

    private void showShapeSettingsDialog() {
        if (shapeSettingsDialog != null && shapeSettingsDialog.isVisible()) {
            // don't allow two dialogs
            return;
        }

        ShapeType selectedType = getSelectedType();
        ShapeTypeSettings settings;
        if (styledShape != null) {
            // if there is an active shape, then configure it
            settings = styledShape.getShapeTypeSettings();
        } else {
            // else configure and store the tool's default
            settings = getDefaultSettingsFor(selectedType);
        }
        JPanel configPanel = settings.getConfigPanel();
        shapeSettingsDialog = new DialogBuilder()
            .title("Settings for " + selectedType)
            .notModal()
            .withScrollbars()
            .content(configPanel)
            .noCancelButton()
            .okText(GUIText.CLOSE_DIALOG)
            .parentComponent(showShapeSettingsButton)
            .show()
            .getDialog();
    }

    private void showEffectsDialog() {
        if (effectsDialog != null && effectsDialog.isVisible()) {
            // don't allow two dialogs
            return;
        }

        DialogBuilder builder = new DialogBuilder()
            .parentComponent(showEffectsDialogButton)
            .notModal();
        effectsParam.configureDialog(builder);
        effectsDialog = builder.show().getDialog();
    }

    private void initAndShowStrokeSettingsDialog() {
        if (strokeSettingsDialog != null && strokeSettingsDialog.isVisible()) {
            // don't allow two dialogs
            return;
        }
        strokeSettingsDialog = createStrokeSettingsDialog();
        GUIUtils.showDialog(strokeSettingsDialog, showStrokeDialogButton);
    }

    private void closeEffectsDialog() {
        GUIUtils.closeDialog(effectsDialog, true);
    }

    private void closeStrokeDialog() {
        GUIUtils.closeDialog(strokeSettingsDialog, true);
    }

    private void closeShapeSettingsDialog() {
        GUIUtils.closeDialog(shapeSettingsDialog, true);
    }

    @Override
    protected void closeToolDialogs() {
        closeStrokeDialog();
        closeEffectsDialog();
        closeShapeSettingsDialog();
    }

    @VisibleForTesting
    public DragToolState getState() {
        return state;
    }

    @Override
    public boolean isDirectDrawing() {
        return state == NO_INTERACTION;
    }

    @Override
    public boolean allowOnlyDrawables() {
        return true;
    }

    @Override
    public String getStateInfo() {
        return getSelectedType()
            + ", fp=" + getSelectedFillPaint()
            + ", sp=" + getSelectedStrokePaint()
            + ", state=" + state;
    }

    @Override
    public DebugNode createDebugNode() {
        var node = super.createDebugNode();

        if (transformBox == null) {
            node.addString("transform box", "null");
        } else {
            node.add(transformBox.createDebugNode());
        }
        if (styledShape == null) {
            node.addString("styledShape", "null");
        } else {
            node.add(styledShape.createDebugNode());
        }

        node.addString("type", getSelectedType().toString());
        node.addString("fill", getSelectedFillPaint().toString());
        node.addString("stroke", getSelectedStrokePaint().toString());
        strokeParam.addDebugNodeInfo(node);

        return node;
    }

    @Override
    public Icon createIcon() {
        return new ShapesToolIcon();
    }

    private static class ShapesToolIcon extends Tool.ToolIcon {
        @Override
        public void paintIcon(Graphics2D g) {
            // the shape is based on shapes_tool.svg
            Path2D shape = new Path2D.Float();

            shape.moveTo(14.0, 5.134844);
            shape.curveTo(14, 5.134844, 15.964081, 1.4467101, 20.675045, 1.4467101);
            shape.curveTo(24.443815, 1.4467101, 26.260193, 5.2239814, 26.286139, 9.187077);
            shape.curveTo(26.328203, 15.612079, 16.0, 21.0, 14, 26.0);
            shape.curveTo(12.0, 21.0, 1.8311971, 15.612079, 1.8311971, 9.001549);
            shape.curveTo(1.8311971, 5.224129, 3.7155826, 1.44671, 7.484354, 1.44671);
            shape.curveTo(12.195311, 1.44671, 14, 5.134844, 14, 5.134844);
            shape.closePath();

            g.setStroke(new BasicStroke(1.5f));
            g.draw(shape);
        }
    }
}


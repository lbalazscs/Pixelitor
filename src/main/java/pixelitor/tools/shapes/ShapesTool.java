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

package pixelitor.tools.shapes;

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.colors.FgBgColors;
import pixelitor.filters.gui.EffectsParam;
import pixelitor.filters.gui.StrokeParam;
import pixelitor.filters.gui.StrokeSettings;
import pixelitor.filters.gui.UserPreset;
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
import pixelitor.layers.Layer;
import pixelitor.layers.ShapesLayer;
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
    public static final String CHANGE_SHAPE_TYPE_SETTINGS = "Change Shape Settings";
    public static final String CHANGE_SHAPE_COLORS = "Change Shape Colors";

    private final Map<ShapeType, ShapeTypeSettings> typeSettingsMap;

    private boolean regenerateShape = true;

    private final EnumComboBoxModel<ShapeType> typeModel
        = new EnumComboBoxModel<>(ShapeType.class);
    private final EnumComboBoxModel<TwoPointPaintType> fillPaintModel
        = new EnumComboBoxModel<>(TwoPointPaintType.class);
    private final EnumComboBoxModel<TwoPointPaintType> strokePaintModel
        = new EnumComboBoxModel<>(TwoPointPaintType.class);

    private JButton showShapeSettingsButton;
    private final Action shapeSettingsAction = new PAction(
        "Settings...", this::showShapeSettingsDialog);

    private final JComboBox<TwoPointPaintType> fillPaintCombo
        = createFillPaintCombo();
    private final JComboBox<TwoPointPaintType> strokePaintCombo
        = createStrokePaintCombo();

    private JDialog shapeSettingsDialog;

    private final StrokeParam strokeParam = new StrokeParam("");

    // During a single mouse drag, only one stroke should be created
    // This is particularly important for "random shape"
    private Stroke stroke = strokeParam.createStroke();

    private JButton showStrokeDialogButton;
    private Action strokeSettingsAction;
    private JDialog strokeSettingsDialog;

    private JButton showEffectsDialogButton;
    private JDialog effectsDialog;
    private final EffectsParam effectsParam = new EffectsParam("");
    private AreaEffects effects = effectsParam.getEffects();

    private StyledShape styledShape;
    private TransformBox transformBox;

    private DragToolState state = NO_INTERACTION;

    private ShapesLayer shapesLayer;

    private final Action convertToSelectionAction = new PAction(
        "Convert to Selection", this::convertToSelection);

    public ShapesTool() {
        super("Shapes", 'U',
            "<b>drag</b> to draw a shape. " +
            "<b>Alt</b> starts from the center, <b>Shift</b> constrains. " +
            "Hold <b>SPACE</b> down while drawing to move the shape. ",
            Cursors.DEFAULT, false);
        spaceDragStartPoint = true;
        convertToSelectionAction.setEnabled(false);
        typeSettingsMap = new EnumMap<>(ShapeType.class);
    }

    @Override
    public void initSettingsPanel() {
        strokeParam.setAdjustmentListener(this::strokeUIChanged);
        effectsParam.setAdjustmentListener(this::effectsUIChanged);

        JComboBox<ShapeType> shapeTypeCB = createShapeTypeCombo();
        settingsPanel.addComboBox("Shape:", shapeTypeCB, "shapeTypeCB");
        showShapeSettingsButton = settingsPanel.addButton(shapeSettingsAction, "shapeSettingsButton",
            "Configure the selected shape");

        settingsPanel.addSeparator();
        settingsPanel.addComboBox("Fill:", fillPaintCombo, "fillPaintCB");
        settingsPanel.addComboBox("Stroke:", strokePaintCombo, "strokePaintCB");

        strokeSettingsAction = new PAction("Stroke Settings...",
            this::initAndShowStrokeSettingsDialog);
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

    public AreaEffects getEffects() {
        return effects;
    }

    private void updateEffects() {
        effects = effectsParam.getEffects();
    }

    private void effectsUIChanged() {
        updateEffects();
        uiChanged(CHANGE_SHAPE_EFFECTS);
    }

    public Stroke getStroke() {
        return stroke;
    }

    public StrokeSettings getStrokeSettings() {
        return strokeParam.copyState();
    }

    private void updateStroke() {
        stroke = strokeParam.createStroke();
    }

    private void shapeTypeChanged() {
        uiChanged(CHANGE_SHAPE_TYPE);
        if (isEditingShapesLayer()) {
            shapesLayer.updateIconImage();
        }
    }

    private void strokeUIChanged() {
        updateStroke();
        uiChanged(CHANGE_SHAPE_STROKE_SETTINGS);
    }

    private void uiChanged(String editName) {
        if (regenerateShape) {
            regenerateShape(editName);
        }

        updateStrokeEnabledState();
        if (editName.equals(CHANGE_SHAPE_TYPE)) {
            shapeSettingsAction.setEnabled(getSelectedType().hasSettings());
            closeShapeSettingsDialog();
        }
    }

    @SuppressWarnings("unchecked")
    private JComboBox<TwoPointPaintType> createFillPaintCombo() {
        return GUIUtils.createComboBox(fillPaintModel,
            e -> fillChanged());
    }

    @SuppressWarnings("unchecked")
    private JComboBox<TwoPointPaintType> createStrokePaintCombo() {
        return GUIUtils.createComboBox(strokePaintModel,
            e -> uiChanged(CHANGE_SHAPE_STROKE));
    }

    @SuppressWarnings("unchecked")
    private JComboBox<ShapeType> createShapeTypeCombo() {
        return GUIUtils.createComboBox(typeModel, e -> shapeTypeChanged());
    }

    public ShapeType getSelectedType() {
        return typeModel.getSelectedItem();
    }

    public ShapeTypeSettings getSettingsOf(ShapeType shapeType) {
        if (!shapeType.hasSettings()) {
            return null;
        }
        return typeSettingsMap.computeIfAbsent(shapeType, this::createTypeSettings);
    }

    private ShapeTypeSettings createTypeSettings(ShapeType type) {
        ShapeTypeSettings settings = type.createSettings();
        settings.setAdjustmentListener(() -> regenerateShape(CHANGE_SHAPE_TYPE_SETTINGS));
        return settings;
    }

    public TwoPointPaintType getSelectedFillPaint() {
        return fillPaintModel.getSelectedItem();
    }

    public TwoPointPaintType getSelectedStrokePaint() {
        return strokePaintModel.getSelectedItem();
    }

    private JDialog createStrokeSettingsDialog() {
        DialogBuilder builder = new DialogBuilder()
            .owner(PixelitorWindow.get())
            .notModal();
        strokeParam.configureSettingsDialog(builder);
        return builder.build();
    }

    public boolean hasStroke() {
        return getSelectedStrokePaint() != NONE;
    }

    public boolean hasFill() {
        return getSelectedFillPaint() != NONE;
    }

    public StrokeParam getStrokeParam() {
        return strokeParam;
    }

    private void setGUIDefaultsFrom(StyledShape styledShape) {
        // as this is used as part of undo/redo, don't regenerate the shape
        regenerateShape = false;
        try {
            ShapeType shapeType = styledShape.getShapeType();
            typeModel.setSelectedItem(shapeType);
            if (shapeType.hasSettings()) {
                getSettingsOf(shapeType).loadStateFrom(styledShape.getShapeTypeSettings());
            }

            fillPaintModel.setSelectedItem(styledShape.getFillPaint());
            strokePaintModel.setSelectedItem(styledShape.getStrokePaint());
            strokeParam.loadStateFrom(styledShape.getStrokeSettings(), false);
            effectsParam.setEffects(styledShape.getEffects());
        } finally {
            regenerateShape = true;
        }
    }

    @Override
    protected void dragStarted(PMouseEvent e) {
        if (state == TRANSFORM) {
            assert hasBox();
            assert hasStyledShape();
            if (transformBox.processMousePressed(e)) {
                return;
            }
            if (!isEditingShapesLayer()) {
                // if pressed outside the transform box,
                // rasterize the existing shape
                rasterizeShape(e.getComp());
            }
        }

        if (isEditingShapesLayer()) {
            if (styledShape == null) {
                startNewShape();
                shapesLayer.setStyledShape(styledShape);
            } else {
                assert state == TRANSFORM : "state = " + state;
            }
        } else {
            startNewShape();
        }
        assert state != NO_INTERACTION : "state = " + state;
    }

    private void startNewShape() {
        styledShape = new StyledShape(this);
        setState(AFTER_FIRST_MOUSE_PRESS);
    }

    @Override
    protected void ongoingDrag(PMouseEvent e) {
        if (state == TRANSFORM) {
            assert hasBox();
            assert hasStyledShape();
            if (transformBox.processMouseDragged(e)) {
                return;
            }
            // we can get here if we entered the TRANSFORM state
            // while dragging another shape
            return;
        }

        if (styledShape != null && !drag.isClick()) {
            updateStyledShapeFromDrag(e);

            // This will trigger paintOverActiveLayer,
            // therefore the continuous drawing of the shape.
            // It repaints the whole image because
            // some shapes extend beyond their drag rectangle.
            e.getComp().getActiveLayer().update(REPAINT);
        }
    }

    @Override
    protected void dragFinished(PMouseEvent e) {
        if (state == TRANSFORM) {
            assert hasBox();
            assert hasStyledShape();
            if (transformBox.processMouseReleased(e)) {
                return;
            }
            // we can get here if we entered the TRANSFORM state
            // while dragging another shape
            return;
        }

        if (drag.isClick()) {
            // cancel the shape started in dragStarted
            setNoInteractionState();
            return;
        }

        if (styledShape == null) {
            // this can happen when getting a fake mouse released event
            // because the user started another tool via keyboard
            setNoInteractionState();
            return;
        }

        updateStyledShapeFromDrag(e);
//        styledShape.createBackupState(this);

        transformBox = styledShape.createBox(e.getView());
        if (transformBox == null) {
            // The box could not be created.
            // Cancel just as for empty clicks.
            setNoInteractionState();
            e.getView().repaint();
            return;
        }

        e.getView().repaint();
        setState(TRANSFORM);
        History.add(new CreateBoxedShapeEdit(e.getComp(), styledShape, transformBox));

        updateStroke(); // is this still necessary?

        if (isEditingShapesLayer()) {
            shapesLayer.setTransformBox(transformBox);
            shapesLayer.updateIconImage();
        }
    }

    private void updateStyledShapeFromDrag(PMouseEvent e) {
        assert styledShape != null;
        assert !drag.isClick();

        styledShape.updateFromDrag(drag, e.isAltDown(), e.isShiftDown());
        setState(INITIAL_DRAG);
    }

    @Override
    public void mouseMoved(MouseEvent e, View view) {
        if (state == TRANSFORM) {
            transformBox.mouseMoved(e);
        }
    }

    @Override
    public void altPressed() {
        if (!altDown && state == INITIAL_DRAG && drag.isDragging() && !drag.isClick()) {
            assert hasStyledShape();
            styledShape.updateFromDrag(drag, true, false);

            Views.getActiveLayer().update(REPAINT);
        }
        altDown = true;
    }

    @Override
    public void altReleased() {
        if (state == INITIAL_DRAG && drag.isDragging() && !drag.isClick()) {
            drag.setStartFromCenter(false);

            assert hasStyledShape();
            styledShape.updateFromDrag(drag, false, false);

            Views.getActiveLayer().update(REPAINT);
        }
        altDown = false;
    }

    @Override
    public void escPressed() {
        // pressing Esc should work similarly to the Gradient Tool,
        // or to clicking outside the transform box:
        // the handles disappear, but the effect remains
        if (state == TRANSFORM && !isEditingShapesLayer()) {
            Views.onActiveComp(this::rasterizeShape);
        }
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        if (transformBox != null) {
            View view = Views.getActive();
            assert view != null;

            transformBox.arrowKeyPressed(key, view);
            return true;
        }
        return false;
    }

    private void setNoInteractionState() {
        transformBox = null;
        styledShape = null;

        setState(NO_INTERACTION);
    }

    private void setState(DragToolState newState) {
        state = newState;

        assert state.isOK(this) : "state = " + state
                                  + ", has styledShape = " + hasStyledShape()
                                  + ", hasBox = " + hasBox();

        convertToSelectionAction.setEnabled(newState == TRANSFORM);
    }

    /**
     * After this method the shape becomes part of the {@link Drawable}'s
     * pixels (before it was only drawn above it).
     */
    private void rasterizeShape(Composition comp) {
        assert hasBox();
        assert hasStyledShape();
        assert !isEditingShapesLayer();

        styledShape.rasterizeTo(comp, transformBox, this);

        setNoInteractionState();
    }

    private void regenerateShape(String editName) {
        if (styledShape != null) {
            // the box can still be null for example if the color
            // is changed via keyboard during the initial drag
            assert transformBox != null || state == INITIAL_DRAG : "state = " + state;

            styledShape.regenerate(transformBox, this, editName);
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
            assert hasBox();
            styledShape.paint(g);
        }
    }

    @Override
    public void paintOverImage(Graphics2D g, Composition comp) {
        if (!comp.getActiveLayer().isVisible()) {
            return;
        }

        if (state == INITIAL_DRAG) {
            assert !hasBox();
            // paint the drag display for the initial drag
            super.paintOverImage(g, comp);
        } else if (state == TRANSFORM) {
            assert hasBox();
            assert hasStyledShape();
            transformBox.paint(g);
        }
    }

    @Override
    protected DragDisplayType getDragDisplayType() {
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

        var comp = Views.getActiveComp();

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

    private void fillChanged() {
        uiChanged(CHANGE_SHAPE_FILL);
        if (isEditingShapesLayer()) {
            shapesLayer.updateIconImage();
        }
    }

    @Override
    public void fgBgColorsChanged() {
        regenerateShape(CHANGE_SHAPE_COLORS);
        if (isEditingShapesLayer()) {
            shapesLayer.updateIconImage();
        }
    }

    @Override
    public void coCoordsChanged(View view) {
        if (transformBox != null) {
            transformBox.coCoordsChanged(view);
        }
    }

    @Override
    public void imCoordsChanged(AffineTransform at, View view) {
        if (transformBox != null && !isEditingShapesLayer()) {
            // A shapes layer will manage the transformation of the box itself.
            // In the case of a CompAction, the transformBox reference belongs
            // to the old composition anyway.
            transformBox.imCoordsChanged(at, view);
        }
    }

    @Override
    public void resetInitialState() {
        // true, for example, when the initial box creation is undone
        boolean hadShape = styledShape != null;

        setNoInteractionState();

        Views.onActiveComp(comp -> {
            if (hadShape) {
                comp.getActiveLayer().update();
            } else {
                comp.repaint();
            }
        });
        shapesLayer = null;
    }

    @Override
    public void compReplaced(Composition newComp, boolean reloaded) {
        if (reloaded) {
            resetInitialState();
        }
        layerActivated(newComp.getActiveLayer());
    }

    @Override
    public void activeLayerChanged(Layer layer) {
        layerActivated(layer);
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
        Views.getActiveComp().update();
    }

    public StyledShape getStyledShape() {
        return styledShape;
    }

    public boolean hasStyledShape() {
        return styledShape != null;
    }

    public TransformBox getTransformBox() {
        return transformBox;
    }

    public boolean hasBox() {
        return transformBox != null;
    }

    public void loadShapeAndBox(StyledShape styledShape, TransformBox box) {
        this.styledShape = styledShape;
        if (box == null) {
            // called by an edit that didn't store the box, because
            // it assumes that the box remains unchanged
            assert hasBox();
            assert state == TRANSFORM : "state = " + state;
            transformBox.setOwner(styledShape);
        } else {
            transformBox = box; // needed for the assertion in createBox
            setState(TRANSFORM);
        }

        FgBgColors.setFGColor(styledShape.getFgColor(), false);
        FgBgColors.setBGColor(styledShape.getBgColor(), false);

        setGUIDefaultsFrom(styledShape);
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        if (oldView != null) {
            rasterizeBox(oldView.getComp());
        }

        super.viewActivated(oldView, newView);
    }

    @Override
    public void forceFinish() {
        rasterizeBox();
    }

    private void rasterizeBox() {
        rasterizeBox(Views.getActiveComp());
    }

    private void rasterizeBox(Composition comp) {
        if (transformBox != null && !isEditingShapesLayer()) {
            assert hasStyledShape();
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
        ShapeTypeSettings settings = getSettingsOf(selectedType);
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

    /**
     * Calculate the extra thickness around the shape for the undo area
     */
    public double calcThickness() {
        double thickness = 0;
        double extraStrokeThickness = 0;
        if (hasStroke()) {
            thickness = strokeParam.getStrokeWidth();

            StrokeType strokeType = strokeParam.getStrokeType();
            extraStrokeThickness = strokeType.getExtraThickness(thickness);
            thickness += extraStrokeThickness;
        }
        if (effects.isNotEmpty()) {
            double effectThickness = effects.getMaxEffectThickness();
            // the extra stroke thickness must be added
            // because the effect can be on the stroke
            effectThickness += extraStrokeThickness;
            if (effectThickness > thickness) {
                thickness = effectThickness;
            }
        }

        return thickness;
    }

    public boolean isEditingShapesLayer() {
        return shapesLayer != null;
    }

    @Override
    protected void toolStarted() {
        super.toolStarted();

        Layer activeLayer = Views.getActiveLayer();
        if (activeLayer != null) {
            layerActivated(activeLayer);
        }
    }

    @Override
    protected void toolEnded() {
        super.toolEnded();

        rasterizeBox();

        resetInitialState();
    }

    private void layerActivated(Layer layer) {
        boolean wasShapesLayer = isEditingShapesLayer();
        if (layer.isMaskEditing()) {
            setupMaskEditing(true);
            startEditingRasterLayer(layer, wasShapesLayer);
        } else {
            if (layer instanceof ShapesLayer sl) {
                if (sl == shapesLayer) {
                    return; // not a new layer
                }
                shapesLayer = sl;
                styledShape = sl.getStyledShape();
                if (styledShape != null && styledShape.isInitialized()) {
                    View view = layer.getComp().getView();
                    transformBox = sl.getTransformBox();
//                    transformBox = styledShape.createBox(view);

                    if (transformBox != null) {
                        if (transformBox.needsInitialization(view)) {
                            transformBox.reInitialize(view, styledShape);
                        }
                        // the zoom could have changed since the box was active
                        transformBox.coCoordsChanged(view);

                        loadShapeAndBox(styledShape, transformBox);
                    } else {
                        throw new IllegalStateException("state = " + state);
                    }

                    // make the loaded box visible - TODO necessary?
                    layer.getComp().repaint();
                } else {
                    setNoInteractionState();
                }
            } else {
                startEditingRasterLayer(layer, wasShapesLayer);
            }
        }
    }

    private void startEditingRasterLayer(Layer layer, boolean wasShapesLayer) {
        shapesLayer = null;
        if (wasShapesLayer) {
            // hide the shape and box
            setNoInteractionState();
            layer.getComp().repaint();
        }
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        ShapeType type = getSelectedType();
        preset.put(ShapeType.PRESET_KEY, type.toString());

        if (type.hasSettings()) {
            getSettingsOf(type).saveStateTo(preset);
        }

        preset.put("Fill", getSelectedFillPaint().toString());
        preset.put("Stroke", getSelectedStrokePaint().toString());

        strokeParam.saveStateTo(preset);
        effectsParam.saveStateTo(preset);

        FgBgColors.saveStateTo(preset);
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        load(preset, true);
    }

    private void load(UserPreset preset, boolean regenerateStyledShape) {
        regenerateShape = false;

        ShapeType type = preset.getEnum(ShapeType.PRESET_KEY, ShapeType.class);
        typeModel.setSelectedItem(type);

        if (type.hasSettings()) {
            getSettingsOf(type).loadStateFrom(preset);
        }

        fillPaintModel.setSelectedItem(
            preset.getEnum("Fill", TwoPointPaintType.class));
        strokePaintModel.setSelectedItem(
            preset.getEnum("Stroke", TwoPointPaintType.class));

        strokeParam.loadStateFrom(preset);
        updateStroke();

        effectsParam.loadStateFrom(preset);
        updateEffects();

        FgBgColors.loadStateFrom(preset);

        regenerateShape = true;
        if (styledShape != null && regenerateStyledShape) {
            styledShape.regenerateAll(transformBox, this);
        }
    }

    @VisibleForTesting
    public void setSelectedType(ShapeType type) {
        typeModel.setSelectedItem(type);
    }

    @Override
    public String getStateInfo() {
        return getSelectedType()
               + ", fp=" + getSelectedFillPaint()
               + ", sp=" + getSelectedStrokePaint()
               + ", state=" + state;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        var node = super.createDebugNode(key);
        node.addAsString("state", state);

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

        node.addAsString("type", getSelectedType());
        node.addAsString("fill", getSelectedFillPaint());
        node.addAsString("stroke", getSelectedStrokePaint());
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


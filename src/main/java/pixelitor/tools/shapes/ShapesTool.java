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
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.TaskAction;
import pixelitor.history.History;
import pixelitor.layers.Drawable;
import pixelitor.layers.Layer;
import pixelitor.layers.ShapesLayer;
import pixelitor.selection.SelectionChangeResult;
import pixelitor.tools.DragTool;
import pixelitor.tools.DragToolState;
import pixelitor.tools.ToolIcons;
import pixelitor.tools.Tools;
import pixelitor.tools.shapes.history.ConvertShapeToSelectionEdit;
import pixelitor.tools.shapes.history.CreateBoxedShapeEdit;
import pixelitor.tools.transform.TransformBox;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.OverlayType;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.EnumMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static pixelitor.tools.DragToolState.AFTER_FIRST_MOUSE_PRESS;
import static pixelitor.tools.DragToolState.IDLE;
import static pixelitor.tools.DragToolState.INITIAL_DRAG;
import static pixelitor.tools.DragToolState.TRANSFORM;
import static pixelitor.tools.shapes.TwoPointPaintType.FOREGROUND;
import static pixelitor.tools.shapes.TwoPointPaintType.NONE;

/**
 * The shapes tool.
 */
public class ShapesTool extends DragTool {
    // history edit names for the shape editing
    public static final String EDIT_FILL = "Change Shape Fill";
    public static final String EDIT_STROKE = "Change Shape Stroke";
    public static final String EDIT_STROKE_SETTINGS = "Change Shape Stroke Settings";
    public static final String EDIT_EFFECTS = "Change Shape Effects";
    public static final String EDIT_TYPE = "Change Shape Type";
    public static final String EDIT_TYPE_SETTINGS = "Change Shape Settings";
    public static final String EDIT_COLORS = "Change Shape Colors";

    private final Map<ShapeType, ShapeTypeSettings> typeSettingsMap;

    // flag to prevent unnecessary shape updates
    private boolean shouldRegenerateShape = true;

    private final EnumComboBoxModel<ShapeType> typeModel
        = new EnumComboBoxModel<>(ShapeType.class);
    private final EnumComboBoxModel<TwoPointPaintType> fillPaintModel
        = new EnumComboBoxModel<>(TwoPointPaintType.class);
    private final EnumComboBoxModel<TwoPointPaintType> strokePaintModel
        = new EnumComboBoxModel<>(TwoPointPaintType.class);

    private JButton showShapeSettingsButton;
    private final Action shapeSettingsAction = new TaskAction(
        "Settings...", this::showShapeSettingsDialog);

    private final JComboBox<TwoPointPaintType> fillPaintCombo
        = createFillPaintCombo();
    private final JComboBox<TwoPointPaintType> strokePaintCombo
        = createStrokePaintCombo();

    private JDialog shapeSettingsDialog;

    private final StrokeParam strokeParam = new StrokeParam("");

    // During a single mouse drag, only one stroke should be created.
    // This is particularly important for "random shape".
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

    private ShapesLayer shapesLayer;

    private final Action convertToSelectionAction = new TaskAction(
        "Convert to Selection", this::convertShapeToSelection);

    public ShapesTool() {
        super("Shapes", 'U',
            "<b>drag</b> to draw a shape. " +
                "<b>Alt</b>-drag from the center, <b>Shift</b>-drag to constrain. " +
                "<b>Space</b>-drag while drawing to move. ",
            Cursors.DEFAULT, false);
        repositionOnSpace = true;
        convertToSelectionAction.setEnabled(false);
        typeSettingsMap = new EnumMap<>(ShapeType.class);
        pixelSnapping = true;
    }

    @Override
    public void initSettingsPanel(ResourceBundle resources) {
        strokeParam.setAdjustmentListener(this::strokeSettingsChanged);
        effectsParam.setAdjustmentListener(this::effectSettingsChanged);

        JComboBox<ShapeType> shapeTypeCB = createShapeTypeCombo();
        settingsPanel.addComboBox("Shape:", shapeTypeCB, "shapeTypeCB");
        showShapeSettingsButton = settingsPanel.addButton(shapeSettingsAction, "shapeSettingsButton",
            "Configure the selected shape");

        settingsPanel.addSeparator();
        settingsPanel.addComboBox("Fill:", fillPaintCombo, "fillPaintCB");
        settingsPanel.addComboBox("Stroke:", strokePaintCombo, "strokePaintCB");

        strokeSettingsAction = new TaskAction("Stroke Settings...",
            this::showStrokeSettingsDialog);
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

    private void fillChanged() {
        settingsChanged(EDIT_FILL);
        if (isEditingShapesLayer()) {
            shapesLayer.updateIconImage();
        }
    }

    private void strokeSettingsChanged() {
        updateStrokeFromSettings();
        settingsChanged(EDIT_STROKE_SETTINGS);
    }

    private void effectSettingsChanged() {
        updateEffects();
        settingsChanged(EDIT_EFFECTS);
    }

    private void shapeTypeChanged() {
        settingsChanged(EDIT_TYPE);
        if (isEditingShapesLayer()) {
            shapesLayer.updateIconImage();
        }
    }

    private void settingsChanged(String editName) {
        if (shouldRegenerateShape) {
            regenerateShape(editName);
        }

        updateStrokeEnabledState();
        if (editName.equals(EDIT_TYPE)) {
            shapeSettingsAction.setEnabled(getSelectedType().hasSettings());
            closeShapeSettingsDialog();
        }
    }

    @SuppressWarnings("unchecked")
    private JComboBox<ShapeType> createShapeTypeCombo() {
        return GUIUtils.createComboBox(typeModel, e -> shapeTypeChanged());
    }

    @SuppressWarnings("unchecked")
    private JComboBox<TwoPointPaintType> createFillPaintCombo() {
        return GUIUtils.createComboBox(fillPaintModel,
            e -> fillChanged());
    }

    @SuppressWarnings("unchecked")
    private JComboBox<TwoPointPaintType> createStrokePaintCombo() {
        return GUIUtils.createComboBox(strokePaintModel,
            e -> settingsChanged(EDIT_STROKE));
    }

    public AreaEffects getEffects() {
        return effects;
    }

    private void updateEffects() {
        effects = effectsParam.getEffects();
    }

    public Stroke getStroke() {
        return stroke;
    }

    public StrokeSettings getStrokeSettings() {
        return strokeParam.copyState();
    }

    private void updateStrokeFromSettings() {
        stroke = strokeParam.createStroke();
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
        settings.setAdjustmentListener(() -> regenerateShape(EDIT_TYPE_SETTINGS));
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

    /**
     * Sets the GUI values based on the given {@link StyledShape}.
     */
    private void updateUIFromShape(StyledShape styledShape) {
        // as this is used as part of undo/redo, don't regenerate the shape
        shouldRegenerateShape = false;
        try {
            ShapeType shapeType = styledShape.getShapeType();
            typeModel.setSelectedItem(shapeType);
            if (shapeType.hasSettings()) {
                getSettingsOf(shapeType).loadStateFrom(styledShape.getTypeSettings());
            }

            fillPaintModel.setSelectedItem(styledShape.getFillPaint());
            strokePaintModel.setSelectedItem(styledShape.getStrokePaint());
            strokeParam.loadStateFrom(styledShape.getStrokeSettings(), false);
            effectsParam.setEffects(styledShape.getEffects());
        } finally {
            shouldRegenerateShape = true;
        }
    }

    @Override
    protected void dragStarted(PMouseEvent e) {
        if (state == TRANSFORM) {
            assert hasBox();
            assert hasStyledShape();
            if (transformBox.processMousePressed(e)) {
                return; // drag started on a handle, the box will manage it
            }

            // if the mouse was pressed outside the transform box:
            if (isEditingShapesLayer()) {
                return; // do nothing
            } else {
                rasterizeShape(e.getComp());
            }
        }

        // if we are here, we are starting a new shape
        startNewShape();
        if (isEditingShapesLayer()) {
            // if we are on an empty shapes layer, the new shape belongs to it
            shapesLayer.setStyledShape(styledShape);
        }

        assert state != IDLE : "state = " + state;
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
            styledShape.updateUI(e.getView());
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
            // we can get here if we entered the TRANSFORM
            // state while dragging another shape
            return;
        }

        if (drag.isClick()) {
            // cancel the shape started in dragStarted
            setIdleState();
            return;
        }

        if (styledShape == null) {
            // this can happen when getting a fake mouse released event
            // because the user started another tool via keyboard
            setIdleState();
            return;
        }

        updateStyledShapeFromDrag(e);

        transformBox = styledShape.createBox(e.getView());
        if (transformBox == null) { // the box could not be created
            // cancel just as for empty clicks.
            setIdleState();
            e.getView().repaint();
            return;
        }

        e.getView().repaint();
        setState(TRANSFORM);
        History.add(new CreateBoxedShapeEdit(e.getComp(), styledShape, transformBox));

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

            Views.getActiveLayer().update(false);
        }
        altDown = true;
    }

    @Override
    public void altReleased() {
        if (state == INITIAL_DRAG && drag.isDragging() && !drag.isClick()) {
            drag.setExpandFromCenter(false);

            assert hasStyledShape();
            styledShape.updateFromDrag(drag, false, false);

            Views.getActiveLayer().update(false);
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

    private void setIdleState() {
        transformBox = null;
        styledShape = null;

        setState(IDLE);
    }

    private void setState(DragToolState newState) {
        state = newState;

        assert state.checkInvariants(this) : "state = " + state
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

        styledShape.rasterize(comp, transformBox, this);

        setIdleState();
    }

    /**
     * Regenerates the shape based on the current UI settings.
     */
    private void regenerateShape(String editName) {
        if (styledShape != null) {
            // the box can still be null for example if the color
            // is changed via keyboard during the initial drag
            assert transformBox != null || state == INITIAL_DRAG : "state = " + state;

            styledShape.regenerate(transformBox, this, editName);
        }
    }

    private void updateStrokeEnabledState() {
        enableStrokeSettings(hasStroke());
    }

    /**
     * Paint over the active layer while rendering the composite image.
     * The transform of the given Graphics2D is in image space.
     */
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
    public void paintOverCanvas(Graphics2D g, Composition comp) {
        if (!comp.getActiveLayer().isVisible()) {
            return;
        }

        if (state == INITIAL_DRAG) {
            assert !hasBox();
            // paint the measurement overlay for the initial drag
            super.paintOverCanvas(g, comp);
        } else if (state == TRANSFORM) {
            assert hasBox();
            assert hasStyledShape();
            transformBox.paint(g);
        }
    }

    @Override
    protected OverlayType getOverlayType() {
        assert state == INITIAL_DRAG : "state = " + state;
        return getSelectedType().getOverlayType();
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

    private void convertShapeToSelection() {
        assert state == TRANSFORM : "state = " + state;

        Shape shape = styledShape.getShape();

        Composition comp = Views.getActiveComp();

        SelectionChangeResult result = comp.changeSelection(shape);
        if (!result.isSuccess()) {
            result.showInfoDialog("shape");
            return;
        }

        History.add(new ConvertShapeToSelectionEdit(
            comp, transformBox, styledShape, result.getEdit()));

        reset();
        Tools.LASSO_SELECTION.activate();
    }

    @Override
    public void fgBgColorsChanged() {
        regenerateShape(EDIT_COLORS);
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
    public void reset() {
        // true, for example, when the initial box creation is undone
        boolean hadShape = styledShape != null;

        setIdleState();

        Composition comp = Views.getActiveComp();
        if (comp != null) {
            if (hadShape) {
                comp.getActiveLayer().update();
            } else {
                comp.repaint();
            }
        }
        shapesLayer = null;
    }

    @Override
    public void compReplaced(Composition newComp, boolean reloaded) {
        if (reloaded) {
            reset();
        }
        layerActivated(newComp.getActiveLayer());
    }

    @Override
    public void editingTargetChanged(Layer activeLayer) {
        layerActivated(activeLayer);
    }

    /**
     * Restores a previously removed transform box as part of an undo/redo operation
     */
    public void restoreBox(StyledShape shape, TransformBox box) {
        assert box.getTarget() == shape;
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

    private boolean hasStroke() {
        return getSelectedStrokePaint() != NONE;
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
            transformBox.setTarget(styledShape);
        } else {
            transformBox = box; // needed for the assertion in createBox
            setState(TRANSFORM);
        }

        FgBgColors.setFGColor(styledShape.getFgColor(), false);
        FgBgColors.setBGColor(styledShape.getBgColor(), false);

        updateUIFromShape(styledShape);
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

    private void showStrokeSettingsDialog() {
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
    protected void closeAllDialogs() {
        closeStrokeDialog();
        closeEffectsDialog();
        closeShapeSettingsDialog();
    }

    @Override
    public boolean isDirectDrawing() {
        return state == IDLE;
    }

    @Override
    public boolean allowOnlyDrawables() {
        return true;
    }

    /**
     * Calculates the extra padding around the shape that is needed
     * to define a safe zone for creating undo history snapshots.
     */
    public double calcExtraPadding() {
        double totalPadding = 0;

        if (hasStroke()) {
            double strokeWidth = strokeParam.getStrokeWidth();
            StrokeType strokeType = strokeParam.getStrokeType();
            totalPadding = strokeWidth + strokeType.getExtraThickness(strokeWidth);
        }
        if (effects.hasEnabledEffects()) {
            // must be added because the effect can be on the stroke
            totalPadding += effects.calcMaxEffectPadding();
        }

        return totalPadding;
    }

    private boolean isEditingShapesLayer() {
        return shapesLayer != null;
    }

    @Override
    protected void toolActivated(View view) {
        super.toolActivated(view);

        if (view != null) {
            layerActivated(view.getComp().getActiveLayer());
        }
    }

    @Override
    protected void toolDeactivated(View view) {
        super.toolDeactivated(view);

        rasterizeBox();

        reset();
    }

    private void layerActivated(Layer layer) {
        if (layer.isMaskEditing()) {
            switchToRasterEditing(layer);
        } else {
            if (layer instanceof ShapesLayer newShapesLayer) {
                if (newShapesLayer == shapesLayer) {
                    return; // already editing this layer
                }
                shapesLayer = newShapesLayer;
                switchToShapesLayerEditing();
            } else {
                switchToRasterEditing(layer);
            }
        }
    }

    private void switchToShapesLayerEditing() {
        styledShape = shapesLayer.getStyledShape();
        if (styledShape == null || !styledShape.hasShape()) {
            setIdleState();
            return;
        }

        View view = shapesLayer.getComp().getView();
        transformBox = shapesLayer.getTransformBox();

        if (transformBox != null) {
            transformBox.reInitialize(view, styledShape);
            // the zoom could have changed since the box was active
            transformBox.coCoordsChanged(view);

            loadShapeAndBox(styledShape, transformBox);
        } else {
            throw new IllegalStateException("state = " + state);
        }

        // make the loaded box visible
        shapesLayer.getComp().repaint();
    }

    private void switchToRasterEditing(Layer layer) {
        boolean wasShapesLayer = isEditingShapesLayer();
        shapesLayer = null;
        if (wasShapesLayer) {
            // hide the shape and box
            setIdleState();
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
        shouldRegenerateShape = false;

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
        updateStrokeFromSettings();

        effectsParam.loadStateFrom(preset);
        updateEffects();

        FgBgColors.loadStateFrom(preset);

        shouldRegenerateShape = true;
        if (styledShape != null && regenerateStyledShape) {
            styledShape.regenerateAll(transformBox, this);
        }
    }

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
        DebugNode node = super.createDebugNode(key);

        node.addNullableDebuggable("transform box", transformBox);
        node.addNullableDebuggable("styled shape", styledShape);
        node.addAsString("type", getSelectedType());
        node.addAsString("fill", getSelectedFillPaint());
        node.addAsString("stroke", getSelectedStrokePaint());
        node.add(strokeParam.createDebugNode("strokeParam"));

        return node;
    }

    @Override
    public Consumer<Graphics2D> createIconPainter() {
        return ToolIcons::paintShapesIcon;
    }
}


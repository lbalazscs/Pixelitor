/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.gui.OpenComps;
import pixelitor.gui.View;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.history.History;
import pixelitor.history.PixelitorEdit;
import pixelitor.layers.Drawable;
import pixelitor.tools.ClipStrategy;
import pixelitor.tools.DragTool;
import pixelitor.tools.Tools;
import pixelitor.tools.shapes.history.ConvertShapeToSelectionEdit;
import pixelitor.tools.shapes.history.CreateBoxedShapeEdit;
import pixelitor.tools.transform.TransformBox;
import pixelitor.tools.util.DragDisplayType;
import pixelitor.tools.util.ImDrag;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

import static pixelitor.Composition.ImageChangeActions.REPAINT;
import static pixelitor.tools.shapes.ShapesToolState.INITIAL_DRAG;
import static pixelitor.tools.shapes.ShapesToolState.NO_INTERACTION;
import static pixelitor.tools.shapes.ShapesToolState.TRANSFORM;

/**
 * The Shapes Tool
 */
public class ShapesTool extends DragTool {
    private final ShapeSettings settings = new ShapeSettings(this);

    private final JComboBox<TwoPointPaintType> fillPaintCombo
        = settings.createFillPaintCombo();
    private final JComboBox<TwoPointPaintType> strokePaintCombo
        = settings.createStrokePaintCombo();

    private JButton strokeSettingsButton;
    private JDialog strokeSettingsDialog;

    private JDialog effectsDialog;

    public static final BasicStroke STROKE_FOR_OPEN_SHAPES = new BasicStroke(1);

    private StyledShape styledShape;
    private TransformBox transformBox;

    private ShapesToolState state = NO_INTERACTION;

    private final Action convertAction = new AbstractAction("Convert to Selection...") {
        @Override
        public void actionPerformed(ActionEvent e) {
            convertToSelection();
        }
    };

    public ShapesTool() {
        super("Shapes", 'u', "shapes_tool_icon.png",
            "<b>drag</b> to draw a shape. " +
                "Hold <b>Alt</b> down to drag from the center. " +
                "Hold <b>SPACE</b> down while drawing to move the shape. ",
            Cursors.DEFAULT, true, true,
            false, ClipStrategy.FULL);
        spaceDragStartPoint = true;
        convertAction.setEnabled(false);
    }

    @Override
    public void initSettingsPanel() {
        JComboBox<ShapeType> shapeTypeCB = settings.createShapeTypeCombo();
        settingsPanel.addWithLabel("Shape:", shapeTypeCB, "shapeTypeCB");

        settingsPanel.addWithLabel("Fill:", fillPaintCombo, "fillPaintCB");
        settingsPanel.addWithLabel("Stroke:", strokePaintCombo, "strokePaintCB");

        strokeSettingsButton = settingsPanel.addButton("Stroke Settings...",
            e -> initAndShowStrokeSettingsDialog());

        settingsPanel.addButton("Effects...",
            e -> showEffectsDialog());

        settingsPanel.addButton(convertAction, "convertToSelection",
            "Convert the active shape to a selection");

        updateStrokeEnabledState();
    }

    private void showEffectsDialog() {
        effectsDialog = settings.buildEffectsDialog(null);
        GUIUtils.showDialog(effectsDialog);
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
            finalizeShape(e.getComp());
        }

        // if this method didn't return yet, start a new shape
        styledShape = new StyledShape(settings);
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

        assert state == INITIAL_DRAG;

        userDrag.setStartFromCenter(e.isAltDown());

        Composition comp = e.getComp();

        assert styledShape != null;
        styledShape.setImDrag(userDrag.toImDrag());
        // this will trigger paintOverLayer, therefore the continuous drawing of the shape
        // TODO it could be optimized not to repaint the whole image, however
        // it is not easy as some shapes extend beyond their drag rectangle
        comp.imageChanged(REPAINT);
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

        assert state == INITIAL_DRAG;

        if (userDrag.isClick()) {
            // cancel the shape started in dragStarted
            styledShape = null;
            setState(NO_INTERACTION);
            return;
        }

        userDrag.setStartFromCenter(e.isAltDown());
        Composition comp = e.getComp();

        transformBox = styledShape.createBox(userDrag, e.getView());

        e.getView().repaint();
        setState(TRANSFORM);
        History.addEdit(new CreateBoxedShapeEdit(comp, styledShape, transformBox));

        settings.invalidateStroke();
    }

    @Override
    public void mouseMoved(MouseEvent e, View view) {
        if (state == TRANSFORM) {
            transformBox.mouseMoved(e);
        }
    }

    private void setState(ShapesToolState newState) {
        state = newState;

        assert state.isOK(this);

        convertAction.setEnabled(newState == TRANSFORM);
    }

    /**
     * After this method the shape becomes part of the {@link Drawable}'s
     * pixels (before it was only drawn above it).
     */
    private void finalizeShape(Composition comp) {
        assert transformBox != null;
        assert styledShape != null;

        styledShape.finalizeTo(comp, transformBox);

        styledShape = null;
        transformBox = null;
        setState(NO_INTERACTION);
    }

    public void regenerateShape() {
        // TODO do other check like in GradientTool
        if (styledShape != null) {
            assert transformBox != null;
            styledShape.regenerate(transformBox);
        }
    }

    // TODO this should be in ShapeSettings?
    public void updateStrokeEnabledState() {
        enableStrokeSettings(settings.hasStrokePaint());
    }

    private void initAndShowStrokeSettingsDialog() {
        strokeSettingsDialog = settings.createStrokeSettingsDialog();

        GUIUtils.showDialog(strokeSettingsDialog);
    }

    private void closeEffectsDialog() {
        GUIUtils.closeDialog(effectsDialog, true);
    }

    private void closeStrokeDialog() {
        GUIUtils.closeDialog(strokeSettingsDialog, true);
    }

    @Override
    protected void closeToolDialogs() {
        closeStrokeDialog();
        closeEffectsDialog();
    }

    @Override
    public void paintOverLayer(Graphics2D g, Composition comp) {
        // updates the shape continuously while drawing
        if (state == INITIAL_DRAG) {
            if (userDrag.isClick()) {
                return;
            }
            styledShape.paint(g);
        } else if (state == TRANSFORM) {
            assert transformBox != null;
            styledShape.paint(g);
        }
    }

    @Override
    public void paintOverImage(Graphics2D g, Canvas canvas, View view,
                               AffineTransform componentTransform,
                               AffineTransform imageTransform) {
        if (state == INITIAL_DRAG) {
            // paint the drag display for the initial drag
            super.paintOverImage(g, canvas, view, componentTransform, imageTransform);
        } else if (state == TRANSFORM) {
            assert transformBox != null;
            assert styledShape != null;
            transformBox.paint(g);
        }
    }

    @Override
    public DragDisplayType getDragDisplayType() {
        assert state == INITIAL_DRAG;
        return settings.getSelectedType().getDragDisplayType();
    }

    /**
     * Programmatically draw the current shape type with the given drag
     */
    public void paintDrag(Drawable dr, ImDrag imDrag) {
//        Shape shape = settings.getSelectedType().getShape(imDrag);
//        paintShape(dr, shape);
        // TODO create a styled shape and paint it
    }

    public boolean shouldDrawOverLayer() {
        return state == INITIAL_DRAG || state == TRANSFORM;
    }

    private void enableStrokeSettings(boolean b) {
        strokeSettingsButton.setEnabled(b);

        if (!b) {
            closeStrokeDialog();
        }
    }

    private void convertToSelection() {
        assert state == TRANSFORM : "state = " + state;

        Shape shape = styledShape.getEffectiveShape();

        Composition comp = OpenComps.getActiveCompOrNull();

        PixelitorEdit selectionEdit = comp.changeSelectionFromShape(shape);
        if (selectionEdit == null) {
            Dialogs.showInfoDialog("No Selection",
                "No selection was created because the shape is outside the canvas.");
            return;
        }

        History.addEdit(new ConvertShapeToSelectionEdit(
            comp, transformBox, styledShape, selectionEdit));

        resetInitialState();
        Tools.SELECTION.activate();
    }

    @Override
    public void fgBgColorsChanged() {
        // calling with ACTION will make it use the new colors
        regenerateShape();
    }

    @Override
    public void coCoordsChanged(View view) {
        if (transformBox != null) {
            transformBox.coCoordsChanged(view);
        }
    }

    @Override
    public void imCoordsChanged(Composition comp, AffineTransform at) {
        if (transformBox != null) {
            transformBox.imCoordsChanged(at);
        }
    }

    @Override
    public void resetInitialState() {
        // true, for example, when the initial box creation is undone
        boolean hadShape = styledShape != null;

        transformBox = null;
        styledShape = null;
        setState(NO_INTERACTION);

        Composition comp = OpenComps.getActiveCompOrNull();
        if (comp != null) { // this gets also called after a "close all"
            if (hadShape) {
                comp.imageChanged();
            } else {
                comp.repaint();
            }
        }
    }

    @Override
    public void compReplaced(Composition oldComp, Composition newComp) {
        resetInitialState();
    }

    /**
     * Restores a previously deleted box as part of an undo/redo operation
     */
    public void restoreBox(StyledShape shape, TransformBox box) {
        // at this point we could have an active StyledShape, if
        // an undo happened while a second shape was dragged

        styledShape = shape;
        transformBox = box;
        setState(TRANSFORM);
        OpenComps.getActiveCompOrNull().imageChanged();
    }

    public StyledShape getStyledShape() {
        return styledShape;
    }

    public TransformBox getTransformBox() {
        return transformBox;
    }

    // used only for undo/redo
    public void setStyledShape(StyledShape styledShape) {
        assert state == TRANSFORM : "state = " + state;

        this.styledShape = styledShape;
        transformBox.setTransformListener(styledShape::transform);
        settings.restoreFrom(styledShape);
    }

    @Override
    protected void toolEnded() {
        super.toolEnded();

        // finalize existing box
        if (transformBox != null) {
            assert styledShape != null;
            assert state == TRANSFORM : "state = " + state;
            finalizeShape(OpenComps.getActiveCompOrNull());
        }

        resetInitialState();
    }

    @Override
    public void compActivated(View oldCV, View newCV) {
        // finalize existing box
        if (transformBox != null) {
            assert styledShape != null;
            assert state == TRANSFORM : "state = " + state;
            finalizeShape(oldCV.getComp());
        } else {
            assert styledShape == null;
        }

        super.compActivated(oldCV, newCV);
    }

    @VisibleForTesting
    public ShapesToolState getState() {
        return state;
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        if (transformBox == null) {
            node.addString("transformBox", "null");
        } else {
            node.add(transformBox.getDebugNode());
        }
        if (styledShape == null) {
            node.addString("styledShape", "null");
        } else {
            node.add(styledShape.getDebugNode());
        }

        settings.addToDebugNode(node);

        return node;
    }
}


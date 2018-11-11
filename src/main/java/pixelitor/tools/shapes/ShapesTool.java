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

package pixelitor.tools.shapes;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.history.History;
import pixelitor.history.NewSelectionEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.history.SelectionChangeEdit;
import pixelitor.layers.Drawable;
import pixelitor.selection.Selection;
import pixelitor.tools.ClipStrategy;
import pixelitor.tools.DragTool;
import pixelitor.tools.shapes.history.CreateBoxedShapeEdit;
import pixelitor.tools.transform.TransformBox;
import pixelitor.tools.util.DragDisplayType;
import pixelitor.tools.util.ImDrag;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

import static pixelitor.Composition.ImageChangeActions.REPAINT;
import static pixelitor.tools.shapes.ShapesTarget.PIXELS;
import static pixelitor.tools.shapes.ShapesTarget.SELECTION;
import static pixelitor.tools.shapes.ShapesToolState.INITIAL_DRAG;
import static pixelitor.tools.shapes.ShapesToolState.NO_INTERACTION;
import static pixelitor.tools.shapes.ShapesToolState.TRANSFORM;

/**
 * The Shapes Tool
 */
public class ShapesTool extends DragTool {
    private final ShapeSettings settings = new ShapeSettings(this);

    private final JComboBox<TwoPointBasedPaint> fillPaintCombo
            = settings.createFillPaintCombo();
    private final JComboBox<TwoPointBasedPaint> strokePaintCombo
            = settings.createStrokePaintCombo();

    private StyledShape styledShape;

    private JButton strokeSettingsButton;
    private JDialog strokeSettingsDialog;

    private JButton effectsButton;
    private JDialog effectsDialog;

    private Shape backupSelectionShape = null;

    public static final BasicStroke STROKE_FOR_OPEN_SHAPES = new BasicStroke(1);

    private TransformBox transformBox;

    private ShapesToolState state = NO_INTERACTION;

    public ShapesTool() {
        super("Shapes", 'u', "shapes_tool_icon.png",
                "<b>drag</b> to draw a shape. " +
                        "Hold <b>Alt</b> down to drag from the center. " +
                        "Hold <b>SPACE</b> down while drawing to move the shape. ",
                Cursors.DEFAULT, true, true,
                false, ClipStrategy.FULL);
        spaceDragStartPoint = true;
    }

    @Override
    public void initSettingsPanel() {
        JComboBox<ShapeType> shapeTypeCB = settings.createShapeTypeCombo();
        settingsPanel.addWithLabel("Shape:", shapeTypeCB, "shapeTypeCB");

        JComboBox<ShapesTarget> targetCB = settings.createTargetCombo();
        settingsPanel.addWithLabel("Target:", targetCB, "targetCB");

        settingsPanel.addWithLabel("Fill:", fillPaintCombo);
        settingsPanel.addWithLabel("Stroke:", strokePaintCombo);

        strokeSettingsButton = settingsPanel.addButton("Stroke Settings...",
                e -> initAndShowStrokeSettingsDialog());

        effectsButton = settingsPanel.addButton("Effects...",
                e -> showEffectsDialog());

        updateEnabledState();
    }

    private void showEffectsDialog() {

//        if (effectsPanel == null) {
//            effectsPanel = new EffectsPanel(
//                    () -> effectsPanel.updateEffectsFromGUI(), null);
//        }

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
        state = INITIAL_DRAG;
        if (settings.getSelectedTarget() == SELECTION) {
            backupSelectionShape = e.getComp().getSelectionShape();
        } else {
            styledShape = new StyledShape(settings);
        }
    }

    @Override
    public void ongoingDrag(PMouseEvent e) {
        if (state == TRANSFORM) {
            assert transformBox != null;
            assert styledShape != null;
            if (transformBox.processMouseDragged(e)) {
                return;
            }
            throw new IllegalStateException("should not get here");
        }


        assert state == INITIAL_DRAG;

        userDrag.setStartFromCenter(e.isAltDown());

        Composition comp = e.getComp();
        if (styledShape != null) {
            assert settings.getSelectedTarget() == PIXELS;

            styledShape.setImDrag(userDrag.toImDrag());
            // this will trigger paintOverLayer, therefore the continuous drawing of the shape
            // TODO it could be optimized not to repaint the whole image, however
            // it is not easy as some shapes extend beyond their drag rectangle
            comp.imageChanged(REPAINT);
        } else {
            assert settings.getSelectedTarget() == SELECTION;

            Shape currentShape = settings.getSelectedType().getShape(userDrag.toImDrag());
            setSelection(currentShape, comp);
        }
    }

    @Override
    public void dragFinished(PMouseEvent e) {
        if (state == TRANSFORM) {
            assert transformBox != null;
            assert styledShape != null;
            if (transformBox.processMouseReleased(e)) {
                return;
            }
            throw new IllegalStateException("should not get here");
        }

        assert state == INITIAL_DRAG;

        if (userDrag.isClick()) {
            // cancel the shape started in dragStarted
            styledShape = null;
            return;
        }

        userDrag.setStartFromCenter(e.isAltDown());
        Composition comp = e.getComp();


        ShapesTarget target = settings.getSelectedTarget();
        if (target == PIXELS) {
//            finalizeShape(comp, action);

            transformBox = styledShape.createBox(userDrag, e.getView());

            e.getView().repaint();
            state = TRANSFORM;
            History.addEdit(new CreateBoxedShapeEdit(comp, styledShape, transformBox));
        } else { // selection mode
            comp.onSelection(selection -> addSelectionEdit(comp, selection));
            state = NO_INTERACTION;
        }

        settings.invalidateStroke();
    }

    @Override
    public void mouseMoved(MouseEvent e, ImageComponent ic) {
        if (state == TRANSFORM) {
            transformBox.mouseMoved(e);
        }
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
    }

    private void addSelectionEdit(Composition comp, Selection selection) {
        selection.clipToCanvasSize(comp); // the selection can be too big
        PixelitorEdit edit;
        if (backupSelectionShape != null) {
            edit = new SelectionChangeEdit("Selection Change",
                    comp, backupSelectionShape);
        } else {
            edit = new NewSelectionEdit(comp, selection.getShape());
        }
        History.addEdit(edit);
    }

    public void regenerateShape() {
        // TODO do other check like in GradientTool
        if (styledShape != null) {
            assert transformBox != null;
            styledShape.regenerate(transformBox);
        }
    }

    // TODO this should be in ShapeSettings?
    public void updateEnabledState() {
        ShapesTarget target = settings.getSelectedTarget();

        enableEffectSettings(target == PIXELS);
        enableStrokeSettings(target == PIXELS && settings.hasStrokePaint());
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

            ShapesTarget target = settings.getSelectedTarget();

            if (target == PIXELS) {
                styledShape.paint(g);
            }
        } else if (state == TRANSFORM) {
            assert transformBox != null;
            styledShape.paint(g);
        }
    }

    @Override
    public void paintOverImage(Graphics2D g, Canvas canvas, ImageComponent ic,
                               AffineTransform componentTransform,
                               AffineTransform imageTransform) {
        if (state == INITIAL_DRAG) {
            // paint the drag display for the initial drag
            super.paintOverImage(g, canvas, ic, componentTransform, imageTransform);
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

    private void setSelection(Shape shape, Composition comp) {
        ShapeType shapeType = settings.getSelectedType();
        Shape selectionShape = createSelectionShape(shape, shapeType);

        Selection selection = comp.getSelection();

        if (selection != null) {
            // this code is called for each drag event:
            // update the selection shape
            selection.setShape(selectionShape);
        } else {
            comp.setSelectionFromShape(selectionShape);
        }
    }

    private static Shape createSelectionShape(Shape shape, ShapeType shapeType) {
        Shape selectionShape;
        if (!shapeType.isClosed()) {
            selectionShape = STROKE_FOR_OPEN_SHAPES.createStrokedShape(shape);
        } else {
            selectionShape = shape;
        }
        return selectionShape;
    }

    public boolean shouldDrawOverLayer() {
        // TODO if a selection is made, then it could always return false?
        return state == INITIAL_DRAG || state == TRANSFORM;
    }

    private void enableStrokeSettings(boolean b) {
        strokeSettingsButton.setEnabled(b);

        if (!b) {
            closeStrokeDialog();
        }
    }

    private void enableEffectSettings(boolean b) {
        effectsButton.setEnabled(b);

        if (!b) {
            closeEffectsDialog();
        }
    }

    @Override
    public void fgBgColorsChanged() {
        // calling with ACTION will make it use the new colors
        regenerateShape();
    }

    @Override
    public void coCoordsChanged(ImageComponent ic) {
        if (transformBox != null) {
            transformBox.coCoordsChanged(ic);
        }
    }

    @Override
    public void resetStateToInitial() {
        // true, for example, when the initial box creation is undone
        boolean hadShape = styledShape != null;

        state = NO_INTERACTION;
        transformBox = null;
        styledShape = null;

        Composition comp = ImageComponents.getActiveCompOrNull();
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
        resetStateToInitial();
    }

    /**
     * Restores a previously deleted box as part of an undo/redo operation
     */
    public void restoreBox(StyledShape shape, TransformBox box) {
        assert styledShape == null;
        assert transformBox == null;

        styledShape = shape;
        transformBox = box;
        state = TRANSFORM;
        ImageComponents.getActiveCompOrNull().imageChanged();
    }

    public StyledShape getStyledShape() {
        return styledShape;
    }

    // used only for undo/redo
    public void setStyledShape(StyledShape styledShape) {
        this.styledShape = styledShape;
        transformBox.setTransformListener(styledShape::transform);
        settings.restoreFrom(styledShape);
    }

    @Override
    protected void toolEnded() {
        super.toolEnded();

        // finalize existing box
        if (transformBox != null) {
            finalizeShape(ImageComponents.getActiveCompOrNull());
        }

        resetStateToInitial();
    }

    @Override
    public void activeImageHasChanged(ImageComponent oldIC, ImageComponent newIC) {
        // finalize existing box
        if (transformBox != null) {
            assert styledShape != null;
            finalizeShape(oldIC.getComp());
        } else {
            assert styledShape == null;
        }

        super.activeImageHasChanged(oldIC, newIC);
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


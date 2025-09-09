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

package pixelitor.tools.move;

import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.View;
import pixelitor.history.History;
import pixelitor.history.PixelitorEdit;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.selection.Selection;
import pixelitor.tools.DragTool;
import pixelitor.tools.ToolIcons;
import pixelitor.tools.selection.SelectionChangeListener;
import pixelitor.tools.transform.CompositeTransformable;
import pixelitor.tools.transform.TransformBox;
import pixelitor.tools.transform.Transformable;
import pixelitor.tools.transform.history.ApplyTransformEdit;
import pixelitor.tools.transform.history.CancelTransformEdit;
import pixelitor.tools.transform.history.TransformStepEdit;
import pixelitor.tools.transform.history.TransformUISnapshot;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.OverlayType;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * A tool for moving layers and selections, with an optional free-transform mode.
 */
public class MoveTool extends DragTool implements SelectionChangeListener {
    private final JComboBox<MoveMode> modeSelector = new JComboBox<>(MoveMode.values());
    private MoveMode activeMode = (MoveMode) modeSelector.getSelectedItem();

    private final JCheckBox autoSelectCheckBox = new JCheckBox();
    private final JCheckBox freeTransformCheckBox = new JCheckBox();

    private TransformBox transformBox;
    private Transformable transformTarget;
    private TransformBox.Memento initialTransformMemento;

    public MoveTool() {
        super("Move", 'V',
            "<b>drag</b> to move the active layer. " +
                "<b>Alt-drag</b> or <b>right-mouse-drag</b> to duplicate and move the active layer. " +
                "<b>Shift-drag</b> to constrain movement.",
            Cursors.DEFAULT, true);
    }

    @Override
    public void initSettingsPanel(ResourceBundle resources) {
        String moveText = resources.getString("mt_move");
        settingsPanel.addComboBox(moveText, modeSelector, "modeSelector");
        modeSelector.addActionListener(e -> modeChanged());

        settingsPanel.addSeparator();
        settingsPanel.addWithLabel("Auto Select Layer:",
            autoSelectCheckBox, "autoSelectCheckBox");

        settingsPanel.addWithLabel("Free Transform:",
            freeTransformCheckBox, "freeTransformCheckBox");
        freeTransformCheckBox.addActionListener(e ->
            setFreeTransformMode(freeTransformCheckBox.isSelected()));
    }

    private void modeChanged() {
        cancelTransform(true);
        activeMode = (MoveMode) modeSelector.getSelectedItem();
    }

    @Override
    protected void dragStarted(PMouseEvent e) {
        if (isFreeTransforming()) {
            // if a transform is active, the drag event goes to the transform box
            if (!transformBox.processMousePressed(e)) {
                // if the press is outside the box, the drag is canceled
                drag.cancel();
            }
        } else { // regular move mode
            if (activeMode.movesLayer() && isAutoSelecting()) {
                Point2D cursorPos = e.toImPoint2D();
                Layer targetLayer = e.getComp().findLayerAtPoint(cursorPos);

                if (targetLayer == null) {
                    drag.cancel();
                    return;
                }
                e.getComp().setActiveLayer(targetLayer);
            }

            e.getComp().prepareMovement(
                activeMode, e.isAltDown() || e.isRight());
        }
    }

    @Override
    protected void ongoingDrag(PMouseEvent e) {
        if (isFreeTransforming()) {
            transformBox.processMouseDragged(e);
        } else {
            e.getComp().moveActiveContent(activeMode, drag.getDX(), drag.getDY());
        }
    }

    @Override
    public void dragFinished(PMouseEvent e) {
        if (isFreeTransforming()) {
            transformBox.processMouseReleased(e);
            // a drag is a discrete, undoable action
            History.add(new TransformStepEdit("Free Transform Step", e.getComp(), transformBox.getBeforeMovementMemento()));
        } else {
            e.getComp().finalizeMovement(activeMode);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e, View view) {
        super.mouseMoved(e, view);

        if (isFreeTransforming()) {
            transformBox.mouseMoved(e);
        } else if (activeMode.movesLayer()) {
            updateMoveCursor(view, e);
        }
    }

    private void updateMoveCursor(View view, MouseEvent e) {
        if (isAutoSelecting()) {
            Point2D p = view.componentToImageSpace(e.getPoint());
            Layer movedLayer = view.getComp().findLayerAtPoint(p);
            if (movedLayer == null) {
                view.setCursor(Cursors.DEFAULT);
                return;
            }
        }
        view.setCursor(Cursors.MOVE);
    }

    public boolean isFreeTransforming() {
        return transformBox != null;
    }

    /**
     * Programmatically moves the active layer and/or the selection.
     */
    public static void move(Composition comp, MoveMode mode, int imDx, int imDy) {
        comp.prepareMovement(mode, false);
        comp.moveActiveContent(mode, imDx, imDy);
        comp.finalizeMovement(mode);
    }

    @Override
    public void paintOverCanvas(Graphics2D g2, Composition comp) {
        if (isFreeTransforming()) {
            transformBox.paint(g2);
            return;
        }

        if (drag == null || !drag.isDragging()) {
            return;
        }

        comp.drawMovementContours(g2, activeMode);
        OverlayType.REL_MOUSE_POS.draw(g2, drag);
    }

    @Override
    protected OverlayType getOverlayType() {
        return OverlayType.REL_MOUSE_POS;
    }

    @Override
    public void coCoordsChanged(View view) {
        if (isFreeTransforming()) {
            transformBox.coCoordsChanged(view);
        }
    }

    @Override
    public void imCoordsChanged(AffineTransform at, View view) {
        if (isFreeTransforming()) {
            transformBox.imCoordsChanged(at, view);
        }
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        View view = Views.getActive();
        if (view == null) {
            return false;
        }

        if (isFreeTransforming()) {
            transformBox.arrowKeyPressed(key, view);
            // a nudge is a discrete, undoable action
            History.add(new TransformStepEdit("Nudge", view.getComp(), transformBox.getBeforeMovementMemento()));
        } else {
            move(view.getComp(), activeMode, key.getDeltaX(), key.getDeltaY());
        }

        return true;
    }

    private void setFreeTransformMode(boolean enabled) {
        if (enabled) {
            createTransformBox();
            if (isFreeTransforming()) {
                Messages.showStatusMessage("Free Transform: drag handles; <b>Enter</b> or <b>Double-click</b> to apply; <b>Esc</b> to cancel.");
            } else {
                handleTransformStartFailure(true);
            }
        } else {
            // if the user unchecks the box, apply the
            // transform and return to regular move mode
            applyTransform(true);
        }
    }

    @Override
    protected void toolActivated(View view) {
        super.toolActivated(view);
        if (freeTransformCheckBox.isSelected()) {
            createTransformBox();
            if (!isFreeTransforming()) {
                handleTransformStartFailure(false);
            }
        }
    }

    private void handleTransformStartFailure(boolean showMessage) {
        freeTransformCheckBox.setSelected(false);

        if (!showMessage) {
            return;
        }

        Composition comp = Views.getActiveComp();

        String message;
        if (activeMode == MoveMode.MOVE_SELECTION_ONLY && comp.getSelection() == null) {
            message = "Free Transform in 'Selection Only' mode requires a selection.";
        } else if (activeMode.movesLayer() && !(comp.getActiveLayer() instanceof ImageLayer)) {
            message = "The active layer must be an image layer to use Free Transform.";
        } else {
            message = "A transformable layer or an active selection is required to start Free Transform.";
        }
        Messages.showInfo("Cannot Start Free Transform", message);
    }

    @Override
    protected void toolDeactivated(View view) {
        // switching to another tool implicitly applies the transform
        applyTransform(true);
        super.toolDeactivated(view);
    }

    @Override
    public void escPressed() {
        cancelTransform(true);
    }

    @Override
    public void otherKeyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER && isFreeTransforming()) {
            applyTransform(true);
            e.consume();
        }
    }

    @Override
    public void editingTargetChanged(Layer activeLayer) {
        if (activeMode.movesLayer()) {
            // switching to another layer implicitly cancels the transform
            cancelTransform(true);
        }
    }

    @Override
    public void compReplaced(Composition newComp, boolean reloaded) {
        cancelTransform(true);
    }

    @Override
    public void selectionChanged() {
        if (activeMode.movesSelection()) {
            cancelTransform(true);
        }
    }

    @Override
    public void selectionDeleted() {
        if (activeMode.movesSelection()) {
            cancelTransform(true);
        }
    }

    @Override
    public void mouseClicked(PMouseEvent e) {
        if (e.getClickCount() == 2 && !e.isConsumed() && isFreeTransforming()) {
            // a double-click applies the transform
            applyTransform(true);
            e.consume();
        }
    }

    private void createTransformBox() {
        View view = Views.getActive();
        if (view == null || isFreeTransforming()) {
            return;
        }

        Composition comp = view.getComp();
        Transformable target = createTransformTarget(activeMode, comp);

        if (target == null) {
            return;
        }

        Rectangle2D boxBounds;

        if (activeMode == MoveMode.MOVE_SELECTION_ONLY) {
            Selection sel = comp.getSelection();
            Rectangle rect = view.imageToComponentSpace(sel.getShapeBounds2D());
            rect.grow(10, 10); // otherwise the box hides a rectangular selection
            // after growing in component-space, transform back to image space
            boxBounds = view.componentToImageSpace(rect);
        } else {
            boxBounds = comp.getCanvas().getBounds();
        }

        transformTarget = target;
        transformTarget.prepareForTransform();
        transformBox = new TransformBox(boxBounds, view, transformTarget);
        transformBox.setUseLegacyHistory(false);
        initialTransformMemento = transformBox.createMemento();

        view.repaint();
    }

    /**
     * Determines what to transform based on the current move mode.
     */
    private static Transformable createTransformTarget(MoveMode mode, Composition comp) {
        Selection sel = comp.getSelection();
        Layer activeLayer = comp.getActiveLayer();

        boolean wantsLayer = mode.movesLayer() && activeLayer instanceof ImageLayer;
        boolean wantsSelection = mode.movesSelection() && sel != null;

        if (wantsLayer && wantsSelection) {
            CompositeTransformable composite = new CompositeTransformable(comp);
            composite.add((ImageLayer) activeLayer);
            composite.add(sel);
            return composite;
        }
        if (wantsLayer) {
            return (ImageLayer) activeLayer;
        }
        if (wantsSelection) {
            return sel;
        }

        return null;
    }

    private void applyTransform(boolean addHistory) {
        if (!isFreeTransforming()) {
            return;
        }

        // create a snapshot of the UI state before finalizing, for undo support
        TransformUISnapshot snapshot = new TransformUISnapshot(
            transformBox.createMemento(),
            transformBox.getOrigImRect(),
            activeMode
        );

        // create the data edit by finalizing the transform on the target
        PixelitorEdit contentEdit = transformTarget.finalizeTransform();

        // if there was a significant change, create a history entry that bundles the
        // data edit with the UI snapshot, allowing the session to be restored on undo
        if (addHistory && contentEdit != null) {
            History.add(new ApplyTransformEdit(
                "Apply Free Transform",
                Views.getActiveComp(),
                contentEdit,
                snapshot
            ));
        }

        // clean up the tool's state
        endTransformSession();

        Messages.showStatusMessage("Free transform applied.");
    }

    public void cancelTransform(boolean addHistory) {
        if (!isFreeTransforming()) {
            return;
        }

        // create a snapshot of the UI state before canceling, for undo support
        TransformUISnapshot snapshot = new TransformUISnapshot(
            transformBox.createMemento(),
            transformBox.getOrigImRect(),
            activeMode
        );

        // revert the target object to its original state
        transformTarget.cancelTransform();

        // create a history entry for the cancellation action, but only if the box state has changed
        if (addHistory && !snapshot.memento().equals(initialTransformMemento)) {
            History.add(new CancelTransformEdit(
                "Cancel Free Transform",
                Views.getActiveComp(),
                snapshot
            ));
        }

        // clean up the tool's state
        endTransformSession();

        Messages.showStatusMessage("Free transform canceled.");
    }

    public void endTransformSession() {
        transformBox = null;
        transformTarget = null;
        initialTransformMemento = null;
        freeTransformCheckBox.setSelected(false);

        Views.getActive().repaint();
    }

    /**
     * Restores an interactive transform session from a history snapshot.
     */
    public void restoreTransformSession(TransformUISnapshot snapshot) {
        // if a transform is already active, cancel it first
        cancelTransform(false);

        View view = Views.getActive();
        if (view == null) {
            return;
        }

        // set up tool state from the snapshot
        modeSelector.setSelectedItem(snapshot.moveMode());
        activeMode = snapshot.moveMode();

        // re-create the transform target based on the mode from the snapshot
        Composition comp = view.getComp();
        Transformable target = createTransformTarget(activeMode, comp);

        if (target == null) {
            // the original target might no longer exist
            freeTransformCheckBox.setSelected(false);
            return;
        }

        // prepare the target and create the TransformBox with its restored state
        transformTarget = target;
        transformTarget.prepareForTransform();

        // create the box using the original image-space rectangle from the snapshot
        transformBox = new TransformBox(snapshot.origImRect(), view, transformTarget);
        transformBox.setUseLegacyHistory(false);
        initialTransformMemento = transformBox.createMemento();

        // restore the handle positions and angle from the memento
        transformBox.restoreFrom(snapshot.memento());

        // update the UI to show the restored transform box
        freeTransformCheckBox.setSelected(true);
        view.repaint();
    }

    public TransformBox getTransformBox() {
        return transformBox;
    }

    public Transformable getTransformTarget() {
        return transformTarget;
    }

    private boolean isAutoSelecting() {
        return autoSelectCheckBox.isSelected();
    }

    @Override
    public boolean isDirectDrawing() {
        return false;
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        preset.put("Mode", modeSelector.getSelectedItem().toString());
        preset.putBoolean("AutoSelect", isAutoSelecting());
        preset.putBoolean("FreeTransform", isFreeTransforming());
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        MoveMode mode = preset.getEnum("Mode", MoveMode.class);
        modeSelector.setSelectedItem(mode);

        autoSelectCheckBox.setSelected(preset.getBoolean("AutoSelect"));
        freeTransformCheckBox.setSelected(preset.getBoolean("FreeTransform"));
    }

    @Override
    public Consumer<Graphics2D> createIconPainter() {
        return ToolIcons::paintMoveIcon;
    }
}

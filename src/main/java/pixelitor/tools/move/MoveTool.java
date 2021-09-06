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

package pixelitor.tools.move;

import pixelitor.AppContext;
import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.gui.View;
import pixelitor.layers.Layer;
import pixelitor.selection.Selection;
import pixelitor.tools.DragTool;
import pixelitor.tools.Tool;
import pixelitor.tools.transform.TransformBox;
import pixelitor.tools.transform.Transformable;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.DragDisplayType;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/**
 * The move tool.
 */
public class MoveTool extends DragTool {
    private final JComboBox<MoveMode> modeSelector = new JComboBox<>(MoveMode.values());
    private MoveMode currentMode = (MoveMode) modeSelector.getSelectedItem();

    private final JCheckBox autoSelectCheckBox = new JCheckBox();
    private final JCheckBox freeTransformCheckBox = new JCheckBox();

    private TransformBox transformBox;

    public MoveTool() {
        super("Move", 'V', "move_tool.png",
            "<b>drag</b> to move the active layer, " +
            "<b>Alt-drag</b> (or <b>right-mouse-drag</b>) to move a duplicate of the active layer. " +
            "<b>Shift-drag</b> to constrain the movement.",
            Cursors.DEFAULT, true);
    }

    @Override
    public void initSettingsPanel() {
        settingsPanel.addComboBox("Move:", modeSelector, "modeSelector");
        modeSelector.addActionListener(e -> currentMode = (MoveMode) modeSelector.getSelectedItem());

        settingsPanel.addSeparator();
        settingsPanel.addWithLabel("Auto Select Layer:",
            autoSelectCheckBox, "autoSelectCheckBox");
        if (AppContext.enableExperimentalFeatures) {
            settingsPanel.addWithLabel("Free Transform:",
                freeTransformCheckBox, "freeTransformCheckBox");
        }
    }

    @Override
    public void mouseMoved(MouseEvent e, View view) {
        super.mouseMoved(e, view);

//        if (currentMode.movesLayer()) {
//            setMoveCursor(view, e);
//        }

        if (transformBox != null) {
            transformBox.mouseMoved(e);
        }
    }

    private void setMoveCursor(View view, MouseEvent e) {
        if (useAutoSelect()) {
            Point2D p = view.componentToImageSpace(e.getPoint());
            ObjectsSelection objectsSelection = ObjectsFinder.findLayerAtPoint(p, view.getComp());

            if (objectsSelection.isEmpty()) {
                view.setCursor(Cursors.DEFAULT);
                return;
            }
        }
        view.setCursor(Cursors.MOVE);
    }

    @Override
    public void dragStarted(PMouseEvent e) {

        if (currentMode.movesLayer() && useAutoSelect()) {
            Point2D p = e.asImPoint2D();
            ObjectsSelection objectsSelection = ObjectsFinder.findLayerAtPoint(p, e.getComp());

            if (objectsSelection.isEmpty()) {
                drag.cancel();
                return;
            }
            e.getComp().setActiveLayer((Layer) objectsSelection.getObject());

            transformBox = null;
        }

        if (freeTransformCheckBox.isSelected()) {

            Selection selection = e.getComp().getSelection();

            if (selection != null) {

                selection.startMovement();
                Rectangle bounds = e.getView().imageToComponentSpace(selection.getShapeBounds2D());

                transformBox = new TransformBox(bounds, e.getView(), new Transformable() {
                    @Override
                    public void transform(AffineTransform transform) {
                        e.getComp().getSelection().transformWhileDragging(transform);
                    }

                    @Override
                    public void updateUI(View view) {
                        view.repaint();
                    }

                    @Override
                    public DebugNode createDebugNode() {
                        return new DebugNode("Anonymous Transformable in MoveTool", this);
                    }
                });
            }

        } else {
            e.getComp().startMovement(
                currentMode, e.isAltDown() || e.isRight());
        }

        if (transformBox != null) {
            if (transformBox.processMousePressed(e)) {
                Selection selection = e.getComp().getSelection();
                if (selection != null) {
                    selection.startMovement();
                }

            } else {
                transformBox = null;
            }
        }

    }

    @Override
    public void ongoingDrag(PMouseEvent e) {

        if (transformBox != null) {
            if (transformBox.processMouseDragged(e)) {
                return;
            }
        }

        if (!freeTransformCheckBox.isSelected()) {

            double relX = drag.getDX();
            double relY = drag.getDY();

            e.getComp().moveActiveContent(currentMode, relX, relY);
        }
    }

    @Override
    public void paintOverImage(Graphics2D g2, Composition comp) {

        if (transformBox != null) {
            transformBox.paint(g2);
            return;
        }

        if (freeTransformCheckBox.isSelected()) {
            return;
        }

        if (drag == null || !drag.isDragging()) {
            return;
        }

        comp.drawMovementContours(g2, currentMode);
        DragDisplayType.REL_MOUSE_POS.draw(g2, drag);
    }

    @Override
    public DragDisplayType getDragDisplayType() {
        return DragDisplayType.REL_MOUSE_POS;
    }

    @Override
    public void dragFinished(PMouseEvent e) {
        if (transformBox != null) {

            if (transformBox.processMouseReleased(e)) {

                Selection selection = e.getComp().getSelection();
                if (selection != null) {
                    selection.endMovement();
                }
            }

            return;
        }

        if (!freeTransformCheckBox.isSelected()) {
            e.getComp().endMovement(currentMode);
        }

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

    /**
     * Moves the active layer programmatically.
     */
    public static void move(Composition comp, MoveMode mode, int relX, int relY) {
        comp.startMovement(mode, false);
        comp.moveActiveContent(mode, relX, relY);
        comp.endMovement(mode);
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        if (transformBox != null) {
            View view = OpenImages.getActiveView();
            assert view != null;
            transformBox.arrowKeyPressed(key, view);
            return true;
        }

        var comp = OpenImages.getActiveComp();
        if (comp != null) {
            move(comp, currentMode, key.getMoveX(), key.getMoveY());
            return true;
        }

        return false;
    }

    private boolean useAutoSelect() {
        return autoSelectCheckBox.isSelected();
    }

    @Override
    public boolean isDirectDrawing() {
        return false;
    }

    @Override
    public Icon createIcon() {
        return new MoveToolIcon();
    }

    private static class MoveToolIcon extends Tool.ToolIcon {
        @Override
        public void paintIcon(Graphics2D g) {
            // the shape is based on move_tool.svg
            Path2D shape = new Path2D.Float();
            // start at the top
            shape.moveTo(14, 0);
            shape.lineTo(18, 5);
            shape.lineTo(15, 5);
            shape.lineTo(15, 13);

            // east arrow
            shape.lineTo(23, 13);
            shape.lineTo(23, 10);
            shape.lineTo(28, 14);
            shape.lineTo(23, 18);
            shape.lineTo(23, 15);
            shape.lineTo(15, 15);

            // south arrow
            shape.lineTo(15, 23);
            shape.lineTo(18, 23);
            shape.lineTo(14, 28);
            shape.lineTo(10, 23);
            shape.lineTo(13, 23);
            shape.lineTo(13, 15);

            // west arrow
            shape.lineTo(5, 15);
            shape.lineTo(5, 18);
            shape.lineTo(0, 14);
            shape.lineTo(5, 10);
            shape.lineTo(5, 13);
            shape.lineTo(13, 13);

            // finish north arrow
            shape.lineTo(13, 5);
            shape.lineTo(10, 5);
            shape.closePath();

            g.fill(shape);
        }
    }
}
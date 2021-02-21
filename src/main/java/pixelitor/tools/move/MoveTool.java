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

import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.gui.View;
import pixelitor.layers.Layer;
import pixelitor.tools.DragTool;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.DragDisplayType;
import pixelitor.tools.util.ImDrag;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

/**
 * The move tool.
 */
public class MoveTool extends DragTool {
    private final JComboBox<MoveMode> modeSelector = new JComboBox<>(MoveMode.values());
    private MoveMode currentMode = (MoveMode) modeSelector.getSelectedItem();

    private final JCheckBox autoSelectCheckBox = new JCheckBox();

    public MoveTool() {
        super("Move", 'V', "move_tool_icon.png",
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
    }

    @Override
    public void mouseMoved(MouseEvent e, View view) {
        super.mouseMoved(e, view);

        if (currentMode.movesLayer()) {
            setMoveCursor(view, e);
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
                userDrag.cancel();
                return;
            }
            e.getComp().setActiveLayer((Layer) objectsSelection.getObject());
        }
        e.getComp().startMovement(
            currentMode, e.isAltDown() || e.isRight());
    }

    @Override
    public void ongoingDrag(PMouseEvent e) {
        ImDrag imDrag = userDrag.toImDrag();
        double relX = imDrag.getDX();
        double relY = imDrag.getDY();

        e.getComp().moveActiveContent(currentMode, relX, relY);
    }

    @Override
    public DragDisplayType getDragDisplayType() {
        return DragDisplayType.REL_MOUSE_POS;
    }

    @Override
    public void dragFinished(PMouseEvent e) {
        e.getComp().endMovement(currentMode);
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
}
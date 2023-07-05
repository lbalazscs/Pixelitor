/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.pen;

import pixelitor.gui.View;
import pixelitor.tools.Tools;
import pixelitor.tools.transform.TransformBox;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.debug.DebugNode;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.List;

import static pixelitor.tools.pen.PenTool.hasPath;
import static pixelitor.tools.pen.PenTool.path;
import static pixelitor.tools.util.DraggablePoint.activePoint;
import static pixelitor.utils.Cursors.DEFAULT;
import static pixelitor.utils.Cursors.MOVE;

/**
 * A pen tool interaction mode where a path can be transformed
 */
public class PathTransformer implements PenToolMode {
    public static final PathTransformer INSTANCE = new PathTransformer();
    private static final String HELP_MESSAGE = "Pen Tool Transform Mode.";

    private List<TransformBox> boxes;

    // Null if no box is being dragged.
    private TransformBox draggedBox;

    // The receiver of keyboard nudges. Never null.
    private TransformBox lastActiveBox;

    private PathTransformer() {
    }

    @Override
    public void paint(Graphics2D g) {
        if (hasPath()) {
            path.paintForTransforming(g);
            if (draggedBox != null) {
                // if a box is dragged, paint only that box
                draggedBox.paint(g);
            } else {
                // otherwise paint all boxes
                for (TransformBox box : boxes) {
                    box.paint(g);
                }
            }
        }
    }

    @Override
    public void coCoordsChanged(View view) {
        for (TransformBox box : boxes) {
            box.coCoordsChanged(view);
        }
    }

    @Override
    public void imCoordsChanged(AffineTransform at, View view) {
        // the path will be transformed by the box, which is unnecessary,
        // since it was already transformed in Composition, but not a
        // problem, because the box uses reference-point based transformations
        for (TransformBox box : boxes) {
            box.imCoordsChanged(at, view);
        }
    }

    @Override
    public void compReplaced() {
        initBoxes();
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        double x = e.getOrigCoX();
        double y = e.getOrigCoY();

        // first look for a handle hit in all the boxes so that
        // handles can be manipulated even when the boxes are overlapping
        boolean handleWasHit = false;
        for (TransformBox box : boxes) {
            DraggablePoint hit = box.findHandleAt(x, y);
            if (hit != null) {
                handleWasHit = true;
                draggedBox = box;
                lastActiveBox = box;
                box.mousePressedOn(hit, x, y);
                break;
            }
        }
        if (handleWasHit) {
            return;
        }
        activePoint = null;

        // if no handle was hit, then look for whole-box movements
        for (TransformBox box : boxes) {
            if (box.contains(x, y)) {
                box.startWholeBoxDrag(x, y);
                draggedBox = box;
                lastActiveBox = box;
                e.repaint();
                break;
            }
        }
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        if (draggedBox != null) { // normally should be always true
            draggedBox.processMouseDragged(e);
        }
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
        if (draggedBox != null) { // normally should be always true
            draggedBox.processMouseReleased(e);
        }
        draggedBox = null;
    }

    @Override
    public boolean mouseMoved(MouseEvent e, View view) {
        boolean hit = false;
        for (TransformBox box : boxes) {
            if (box.processMouseMoved(e)) {
                hit = true;
                break;
            }
        }
        if (!hit) {
            if (activePoint != null) {
                activePoint = null;
                view.repaint();
            }
            int x = e.getX();
            int y = e.getY();
            boolean contained = false;
            for (TransformBox box : boxes) {
                if (box.contains(x, y)) {
                    contained = true;
                    break;
                }
            }
            view.setCursor(contained ? MOVE : DEFAULT);
        }
        return false;
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key, View view) {
        if (lastActiveBox != null) {
            lastActiveBox.arrowKeyPressed(key, view);
            return true;
        }
        return false;
    }

    @Override
    public String getToolMessage() {
        return HELP_MESSAGE;
    }

    @Override
    public void start() {
        Tools.PEN.startMode(TRANSFORM, false);
    }

    @Override
    public void modeStarted(PenToolMode prevMode) {
        PenToolMode.super.modeStarted(prevMode);
        initBoxes();
    }

    private void initBoxes() {
        boxes = path.createTransformBoxes();
        if (!boxes.isEmpty()) {
            lastActiveBox = boxes.get(0);
        } else {
            lastActiveBox = null;
        }
    }

    @Override
    public boolean requiresExistingPath() {
        return true;
    }

    @VisibleForTesting
    public TransformBox getBox(int index) {
        return boxes.get(index);
    }

    @Override
    public DebugNode createDebugNode() {
        var node = PenToolMode.super.createDebugNode();
        for (TransformBox box : boxes) {
            node.add(box.createDebugNode());
        }
        return node;
    }

    @Override
    public String toString() {
        return "Transform";
    }
}

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

package pixelitor.tools.pen;

import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.gui.View;
import pixelitor.gui.utils.VectorIcon;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.transform.TransformBox;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PMouseEvent;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_MITER;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.tools.pen.PathActions.setActionsEnabled;
import static pixelitor.tools.util.DraggablePoint.activePoint;
import static pixelitor.utils.Cursors.DEFAULT;
import static pixelitor.utils.Cursors.MOVE;

public class TransformPathTool extends PathTool {
    private List<TransformBox> boxes;
    private TransformBox draggedBox;
    private TransformBox lastActiveBox;

    public TransformPathTool() {
        super("Transform Path", "drag the handles to transform the path.", DEFAULT);
    }

    @Override
    protected void toolActivated(View view) {
        super.toolActivated(view);

        if (view == null) {
            // allow the tool activation for now, the path
            // will be checked when there is an active view
            setActionsEnabled(false);
            return;
        }
        Composition comp = view.getComp();
        Path compPath = comp.getActivePath();
        if (compPath == null) {
            Tools.PEN.activate();
            return;
        }

        initBoxes(comp);
        setActionsEnabled(comp.hasActivePath());
        coCoordsChanged(view);
    }

    @Override
    protected void toolDeactivated(View view) {
        super.toolDeactivated(view);

        view.repaint(); // visually hide the path
    }

    @Override
    public void paintOverCanvas(Graphics2D g2, Composition comp) {
        Path path = comp.getActivePath();
        if (path == null) {
            return;
        }
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        path.paintForTransforming(g2);

        // paints all boxes unless a box is being dragged
        if (draggedBox != null) {
            draggedBox.paint(g2);
        } else {
            for (TransformBox box : boxes) {
                box.paint(g2);
            }
        }
    }

    @Override
    public void coCoordsChanged(View view) {
        if (view.getComp().hasActivePath()) {
            for (TransformBox box : boxes) {
                box.coCoordsChanged(view);
            }
        }
    }

    @Override
    public void imCoordsChanged(AffineTransform at, View view) {
        if (view.getComp().hasActivePath()) {
            for (TransformBox box : boxes) {
                box.imCoordsChanged(at, view);
            }
        }
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        if (!e.getComp().hasActivePath()) {
            return;
        }

        double x = e.getOrigCoX();
        double y = e.getOrigCoY();

        boolean handleFound = false;
        for (TransformBox box : boxes) {
            DraggablePoint handle = box.findHandleAt(x, y);
            if (handle != null) {
                handleFound = true;
                draggedBox = box;
                lastActiveBox = box;
                box.mousePressedOn(handle, x, y);
                break;
            }
        }
        if (handleFound) {
            return;
        }
        activePoint = null;

        for (TransformBox box : boxes) {
            if (box.contains(x, y)) {
                box.prepareWholeBoxDrag(x, y);
                draggedBox = box;
                lastActiveBox = box;
                e.repaint();
                break;
            }
        }
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        if (draggedBox != null) {
            draggedBox.processMouseDragged(e);
        }
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
        if (draggedBox != null) {
            draggedBox.processMouseReleased(e);
        }
        draggedBox = null;
    }

    @Override
    public void mouseMoved(MouseEvent e, View view) {
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
            updateMoveCursor(e, view);
        }
    }

    private void updateMoveCursor(MouseEvent e, View view) {
        int x = e.getX();
        int y = e.getY();
        boolean insideBox = false;
        for (TransformBox box : boxes) {
            if (box.contains(x, y)) {
                insideBox = true;
                break;
            }
        }
        view.setCursor(insideBox ? MOVE : DEFAULT);
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        if (lastActiveBox != null) {
            lastActiveBox.arrowKeyPressed(key, Views.getActive());
            return true;
        }
        return false;
    }

    public void initBoxes(Composition comp) {
        // We assume that as long as this tool is active, there is no
        // way for the paths to change (other than via the boxes), so
        // we don't have to listen to external path changes.
        boxes = comp.getActivePath().createTransformBoxes();
        lastActiveBox = boxes.isEmpty() ? null : boxes.getFirst();
    }

    public TransformBox getBox(int index) {
        return boxes.get(index);
    }

    @Override
    public VectorIcon createIcon() {
        return new TransformPathToolIcon();
    }

    private static class TransformPathToolIcon extends Tool.ToolIcon {
        @Override
        public void paintIcon(Graphics2D g) {
            // based on transfrom_tool.svg
            Path2D shape = new Path2D.Double();

            // top left rectangle
            shape.append(new Rectangle2D.Double(2.75, 2.75, 5.5, 5.5), false);
            // bottom left rectangle
            shape.append(new Rectangle2D.Double(2.75, 19.75, 5.5, 5.5), false);
            // top right rectangle
            shape.append(new Rectangle2D.Double(19.75, 2.75, 5.5, 5.5), false);
            // bottom right rectangle
            shape.append(new Rectangle2D.Double(19.75, 19.75, 5.5, 5.5), false);

            // left line segment
            shape.append(new Line2D.Double(5.5, 8.5, 5.5, 19.5), false);
            // top line segment
            shape.append(new Line2D.Double(8.5, 5.5, 19.5, 5.5), false);
            // right line segment
            shape.append(new Line2D.Double(22.5, 8.5, 22.5, 19.5), false);
            // bottom line segment
            shape.append(new Line2D.Double(8.5, 22.5, 19.5, 22.5), false);

            g.setStroke(new BasicStroke(1.5f, CAP_BUTT, JOIN_MITER, 4));
            g.draw(shape);
        }
    }
}
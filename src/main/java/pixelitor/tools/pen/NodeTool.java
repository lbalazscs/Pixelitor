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

import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.tools.Tools;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.util.function.Consumer;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_MITER;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.tools.pen.AnchorPointType.SYMMETRIC;
import static pixelitor.tools.pen.PathActions.setActionsEnabled;
import static pixelitor.tools.util.DraggablePoint.activePoint;
import static pixelitor.tools.util.DraggablePoint.lastActive;

public class NodeTool extends PathTool {
    public NodeTool() {
        super("Node", "<b>drag</b> anchor and control points. " +
            "<b>Right-click</b> anchor points for options. " +
            "<b>Alt-drag</b> pulls out or breaks handles, " +
            "<b>Shift</b> constrains angles.", Cursors.DEFAULT);
    }

    @Override
    protected void toolActivated(View view) {
        super.toolActivated(view);

        if (view == null) {
            // allow tool activation for now, the path
            // will be checked when a view becomes active
            setActionsEnabled(false);
            return;
        }

        if (!view.getComp().hasActivePath()) {
            // if there is no path, switch to the Pen tool to create one
            Tools.PEN.activate();
            return;
        }

        coCoordsChanged(view);
        setActionsEnabled(true);

        view.repaint(); // show the path
    }

    @Override
    protected void toolDeactivated(View view) {
        super.toolDeactivated(view);

        lastActive = null;
        if (view == null) {
            return;
        }
        view.repaint(); // hide the path
    }

    @Override
    public void paintOverCanvas(Graphics2D g2, Composition comp) {
        Path path = comp.getActivePath();
        if (path != null) {
            g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            path.paintForEditing(g2);
        }
    }

    @Override
    public void coCoordsChanged(View view) {
        Path path = view.getComp().getActivePath();
        if (path != null) {
            path.coCoordsChanged(view);
        }
    }

    @Override
    public void imCoordsChanged(AffineTransform at, View view) {
        // do nothing
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        Path path = e.getComp().getActivePath();
        if (path == null) {
            return;
        }

        double x = e.getCoX();
        double y = e.getCoY();

        DraggablePoint handle = path.findHandleAt(x, y, e.isAltDown());
        if (handle != null) {
            if (e.isPopupTrigger() && handle instanceof AnchorPoint ap) {
                ap.showPopup((int) x, (int) y);
            } else if (e.isAltDown()) {
                altMousePressedOn(handle, x, y);
            } else {
                handle.setActive(true);
                handle.mousePressed(x, y);
            }
        }
    }

    private static void altMousePressedOn(DraggablePoint handle, double x, double y) {
        if (handle instanceof ControlPoint cp) {
            altMousePressedOnControl(cp, x, y);
        } else if (handle instanceof AnchorPoint ap) {
            altMousePressedOnAnchor(ap, x, y);
        }
    }

    private static void altMousePressedOnControl(ControlPoint cp, double x, double y) {
        cp.breakOrDragOut();
        cp.setActive(true);
        cp.mousePressed(x, y);
    }

    private static void altMousePressedOnAnchor(AnchorPoint ap, double x, double y) {
        ap.retractHandles();
        ap.setType(SYMMETRIC);
        ap.ctrlOut.setActive(true);
        ap.ctrlOut.mousePressed(x, y);
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        if (activePoint != null) {
            activePoint.mouseDragged(e.getCoX(), e.getCoY(), e.isShiftDown());
            e.repaint();
        }
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
        if (activePoint == null) {
            return;
        }

        double x = e.getCoX();
        double y = e.getCoY();

        if (e.isPopupTrigger() && activePoint instanceof AnchorPoint ap) {
            ap.showPopup((int) x, (int) y);
        } else {
            activePoint.mouseReleased(x, y, e.isShiftDown());
            Composition comp = e.getComp();
            Path path = comp.getActivePath();
            activePoint.createMovedEdit(comp).ifPresent(path::handleMoved);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e, View view) {
        Path path = view.getComp().getActivePath();
        if (path == null) {
            if (AppMode.isDevelopment()) {
                throw new IllegalStateException("null path in path edit mode");
            }
            return;
        }

        DraggablePoint handle = path.findHandleAt(e.getX(), e.getY(), e.isAltDown());
        if (handle != activePoint) {
            if (activePoint != null) {
                activePoint.setActive(false);
            }
            if (handle != null) {
                handle.setActive(true);
            }
            // repaint only if the hovered handle has changed
            view.repaint();
        }
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        if (activePoint != null) {
            return activePoint.nudge(key);
        }
        if (lastActive != null) {
            return lastActive.nudge(key);
        }

        return false;
    }

    @Override
    public Consumer<Graphics2D> createIconPainter() {
        return g -> {
            // based on path_edit_tool.svg

            g.setStroke(new BasicStroke(1.5f, CAP_BUTT, JOIN_MITER, 4));

            g.draw(new Rectangle2D.Double(2.75, 21.75, 3.5, 3.5)); // left
            g.draw(new Rectangle2D.Double(21.75, 21.75, 3.5, 3.5)); // right
            g.draw(new Rectangle2D.Double(12.25, 2.75, 3.5, 3.5)); // top

            // Circles
            g.draw(new Ellipse2D.Double(4.5 - 1.75, 4.5 - 1.75, 1.75 * 2, 1.75 * 2)); // left
            g.draw(new Ellipse2D.Double(23.5 - 1.75, 4.5 - 1.75, 1.75 * 2, 1.75 * 2)); // right

            g.draw(new Line2D.Double(6.5, 4.5, 12, 4.5)); // left
            g.draw(new Line2D.Double(16, 4.5, 21.5, 4.5)); // right

            Path2D.Double leftPath = new Path2D.Double();
            leftPath.moveTo(4.5, 21.5);
            leftPath.curveTo(4.5, 21.5, 4.5, 4.5, 12.0, 4.5);
            g.draw(leftPath);

            Path2D.Double rightPath = new Path2D.Double();
            rightPath.moveTo(23.5, 21.5);
            rightPath.curveTo(23.5, 21.5, 23.5, 4.5, 16.25, 4.500000);
            g.draw(rightPath);
        };
    }
}

/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.View;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PMouseEvent;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

import static pixelitor.tools.pen.AnchorPointType.CUSP;
import static pixelitor.tools.pen.AnchorPointType.SYMMETRIC;
import static pixelitor.tools.pen.PenTool.hasPath;
import static pixelitor.tools.pen.PenTool.path;
import static pixelitor.tools.util.DraggablePoint.activePoint;
import static pixelitor.tools.util.DraggablePoint.lastActive;

/**
 * A pen tool interaction mode where a path can be edited
 */
public final class PathEditor implements PenToolMode {
    public static final PathEditor INSTANCE = new PathEditor();
    private static final String EDIT_HELP_MESSAGE =
        "Pen Tool Edit Mode: " +
            "<b>drag</b> anchor and control points. " +
            "<b>Right-click</b> anchor points for options. " +
            "<b>Alt-drag</b> pulls out or breaks handles, " +
            "<b>Shift</b> constrains angles.";

    private PathEditor() {
    }

    @Override
    public void paint(Graphics2D g) {
        if (hasPath()) {
            path.paintForEditing(g);
        }
    }

    @Override
    public void coCoordsChanged(View view) {
        // do nothing
    }

    @Override
    public void imCoordsChanged(AffineTransform at, View view) {
        // do nothing
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        double x = e.getCoX();
        double y = e.getCoY();
        boolean altDown = e.isAltDown();

        DraggablePoint handle = path.findHandleAt(x, y, altDown);
        if (handle != null) {
            if (e.isPopupTrigger() && handle instanceof AnchorPoint ap) {
                ap.showPopup((int) x, (int) y);
            } else if (altDown) {
                altMousePressedOn(handle, x, y);
            } else {
                // Alt is not down, normal editing
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
        cp.getAnchor().setType(cp.isRetracted() ? SYMMETRIC : CUSP);
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
            activePoint.createMovedEdit(e.getComp()).ifPresent(path::handleMoved);
        }
    }

    @Override
    public boolean mouseMoved(MouseEvent e, View view) {
        if (path == null) {
            // shouldn't happen, but it is very annoying for the user
            // if an exception dialog is shown whenever the mouse moves
            if (AppMode.isDevelopment()) {
                throw new IllegalStateException("null path in path edit mode");
            }
            return false;
        }

        // highlights the point under the mouse if present
        DraggablePoint handle = path.findHandleAt(e.getX(), e.getY(), e.isAltDown());
        if (handle != null) {
            handle.setActive(true);
            return true;
        } else if (activePoint != null) {
            activePoint.setActive(false);
            return true;
        }

        return false;
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key, View view) {
        if (activePoint != null) {
            activePoint.arrowKeyPressed(key);
            return true;
        }
        if (lastActive != null) {
            lastActive.arrowKeyPressed(key);
            return true;
        }

        return false;
    }

    @Override
    public String getToolMessage() {
        return EDIT_HELP_MESSAGE;
    }

    @Override
    public boolean requiresExistingPath() {
        return true;
    }

    @Override
    public String toString() {
        return "Edit";
    }
}

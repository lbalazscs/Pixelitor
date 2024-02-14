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

import pixelitor.GUIMode;
import pixelitor.gui.View;
import pixelitor.tools.Tools;
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
            "<b>drag</b> the anchor and control points. " +
            "<b>Right-click</b> the anchor points for options. " +
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
        DraggablePoint hit = path.handleWasHit(x, y, altDown);
        if (hit != null) {
            if (e.isPopupTrigger() && hit instanceof AnchorPoint ap) {
                ap.showPopup((int) x, (int) y);
            } else if (altDown) {
                altMousePressedHit(hit, x, y);
            } else {
                // Alt is not down, normal editing
                hit.setActive(true);
                hit.mousePressed(x, y);
            }
        }
    }

    private static void altMousePressedHit(DraggablePoint hit, double x, double y) {
        if (hit instanceof ControlPoint cp) {
            altMousePressedHitControl(cp, x, y);
        } else if (hit instanceof AnchorPoint ap) {
            altMousePressedHitAnchor(ap, x, y);
        }
    }

    private static void altMousePressedHitControl(ControlPoint cp, double x, double y) {
        if (cp.isRetracted()) {
            cp.getAnchor().setType(SYMMETRIC);
        } else {
            cp.getAnchor().setType(CUSP);
        }
        cp.setActive(true);
        cp.mousePressed(x, y);
    }

    private static void altMousePressedHitAnchor(AnchorPoint ap, double x, double y) {
        ap.retractHandles();
        ap.setType(SYMMETRIC);
        ap.ctrlOut.setActive(true);
        ap.ctrlOut.mousePressed(x, y);
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        double x = e.getCoX();
        double y = e.getCoY();

        if (activePoint != null) {
            activePoint.mouseDragged(x, y, e.isShiftDown());
        }
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
        double x = e.getCoX();
        double y = e.getCoY();

        if (activePoint != null) {
            if (e.isPopupTrigger() && activePoint instanceof AnchorPoint ap) {
                ap.showPopup((int) x, (int) y);
            } else {
                activePoint.mouseReleased(x, y, e.isShiftDown());
                activePoint.createMovedEdit(e.getComp()).ifPresent(path::handleMoved);
            }
        }
    }

    @Override
    public boolean mouseMoved(MouseEvent e, View view) {
        if (path == null) {
            // shouldn't happen, but it is very annoying for the user
            // if an exception dialog is shown whenever the mouse moves
            if (GUIMode.isDevelopment()) {
                throw new IllegalStateException("null path in path edit mode");
            }
            return false;
        }

        DraggablePoint hit = path.handleWasHit(e.getX(), e.getY(), e.isAltDown());
        if (hit != null) {
            hit.setActive(true);
            return true;
        } else {
            if (activePoint != null) {
                activePoint.setActive(false);
                return true;
            }
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
    public void start() {
        Tools.PEN.startMode(EDIT, false);
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

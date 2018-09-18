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

package pixelitor.tools.pen;

import pixelitor.gui.ImageComponent;
import pixelitor.history.History;
import pixelitor.tools.Tools;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PMouseEvent;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

import static pixelitor.tools.pen.AnchorPointType.CUSP;
import static pixelitor.tools.pen.AnchorPointType.SYMMETRIC;
import static pixelitor.tools.pen.PenTool.path;
import static pixelitor.tools.util.DraggablePoint.activePoint;

/**
 * A pen tool interaction mode where a path can be edited
 */
public class PathEditor implements PenToolMode {
    public static final PathEditor INSTANCE = new PathEditor();
    private static final String EDIT_HELP_MESSAGE =
            "<html>Pen Tool Edit Mode: " +
                    "<b>drag</b> the anchor and control points. " +
                    "<b>Right-click</b> the anchor points for options. " +
                    "<b>Alt-drag</b> pulls out or breaks handles, " +
                    "<b>Shift</b> constrains angles.";

    private PathEditor() {
    }

    @Override
    public void paint(Graphics2D g) {
        if (path != null) {
            path.paintForEditing(g);
        }
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        double x = e.getCoX();
        double y = e.getCoY();

        boolean altDown = e.isAltDown();
        DraggablePoint hit = path.handleWasHit(x, y, altDown);
        if (hit != null) {
            if (altDown) {
                if (hit instanceof ControlPoint) {
                    ControlPoint cp = (ControlPoint) hit;
                    if (cp.isRetracted()) {
                        cp.getAnchor().setType(SYMMETRIC);
                    } else {
                        cp.getAnchor().setType(CUSP);
                    }
                    cp.setActive(true);
                    cp.mousePressed(x, y);
                } else if (hit instanceof AnchorPoint) {
                    AnchorPoint ap = (AnchorPoint) hit;
                    ap.retractHandles();
                    ap.setType(SYMMETRIC);
                    ap.ctrlOut.setActive(true);
                    ap.ctrlOut.mousePressed(x, y);
                }
            } else {
                // Alt is not down, normal editing
                hit.setActive(true);
                hit.mousePressed(x, y);
            }
        }
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
            if (e.isPopupTrigger() && activePoint instanceof AnchorPoint) {
                AnchorPoint ap = (AnchorPoint) activePoint;
                ap.showPopup((int) x, (int) y);
            } else {
                activePoint.mouseReleased(x, y, e.isShiftDown());
                activePoint
                        .createMovedEdit(e.getComp())
                        .ifPresent(History::addEdit);
            }
        }
    }

    @Override
    public boolean mouseMoved(MouseEvent e, ImageComponent ic) {
        int x = e.getX();
        int y = e.getY();
        DraggablePoint hit = path.handleWasHit(x, y, e.isAltDown());
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
    public String getToolMessage() {
        return EDIT_HELP_MESSAGE;
    }

    @Override
    public void start() {
        Tools.PEN.startEditing(false);
    }

    @Override
    public String toString() {
        return "Edit";
    }
}

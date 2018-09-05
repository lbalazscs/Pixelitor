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
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PMouseEvent;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

import static pixelitor.tools.util.DraggablePoint.activePoint;

/**
 * A pen tool interaction mode where a path can be edited
 */
public class PathEditor implements PenToolMode {
    public static final PathEditor INSTANCE = new PathEditor();
    public static final String EDIT_HELP_MESSAGE =
            "<html>Pen Tool Edit Mode: " +
                    "<b>drag</b> the anchor and control points. " +
                    "<b>Right-click</b> the anchor points for options. " +
                    "<b>Alt-drag</b> to pull out handles.";
    private Path path;

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

        DraggablePoint draggablePoint = path.handleWasHit(x, y, e.isAltDown());
        if (draggablePoint != null) {
            draggablePoint.setActive(true);
            draggablePoint.mousePressed(x, y);
        }
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        double x = e.getCoX();
        double y = e.getCoY();

        if (activePoint != null) {
            activePoint.mouseDragged(x, y);
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
                activePoint.mouseReleased(x, y);
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
        DraggablePoint hitPoint = path.handleWasHit(x, y, e.isAltDown());
        if (hitPoint != null) {
            hitPoint.setActive(true);
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
    public Path getPath() {
        return path;
    }

    @Override
    public void setPath(Path path, String reason) {
//        System.out.printf("PathEditor::setPath: reason = '%s'%n", reason);
        assert path != null : "null path because " + reason;
        this.path = path;
    }

    @Override
    public String getToolMessage() {
        return EDIT_HELP_MESSAGE;
    }

    @Override
    public void modeStarted() {

    }

    @Override
    public void modeEnded() {

    }

    @Override
    public String toString() {
        return "Edit";
    }
}

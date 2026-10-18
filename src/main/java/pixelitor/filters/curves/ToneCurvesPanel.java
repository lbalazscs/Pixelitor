/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.curves;

import pixelitor.filters.util.Channel;

import javax.swing.*;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;

/**
 * Panel for displaying and interacting with tone curves across sRGB and Oklab channels.
 * It's responsible for handling user input and repainting the curves.
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurvesPanel extends JPanel implements MouseListener, MouseMotionListener {
    private static final Dimension PANEL_SIZE = new Dimension(
        ToneCurves.PANEL_SIZE, ToneCurves.PANEL_SIZE);

    // the ToneCurves instance this panel represents
    private final ToneCurves toneCurves;

    private int draggedKnotIndex = -1; // index of the knot being dragged
    private int deletedKnotIndex = -1; // index of a knot deleted by dragging it out of bounds
    private Runnable onChange;

    public ToneCurvesPanel(ToneCurves toneCurves) {
        this.toneCurves = toneCurves;

        setPreferredSize(PANEL_SIZE);
        setMinimumSize(PANEL_SIZE);

        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public void setChangeListener(Runnable onChange) {
        this.onChange = onChange;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.translate(getDrawingOffsetX(), getDrawingOffsetY());
            toneCurves.draw(g2);
        } finally {
            g2.dispose();
        }
    }

    public void modelChanged() {
        repaint();
        if (onChange != null) {
            onChange.run();
        }
    }

    private Point2D.Float normalizeMousePos(MouseEvent e) {
        return ToneCurves.toCurveSpace(e.getX() - getDrawingOffsetX(), e.getY() - getDrawingOffsetY());
    }

    public void setActiveChannel(Channel channel) {
        // prevent duplicate filter executions when changing the color space
        if (toneCurves.setActiveChannel(channel)) {
            modelChanged();
        }
    }

    public void resetActiveCurve() {
        toneCurves.getActiveCurve().reset();
        modelChanged();
    }

    public void randomize() {
        toneCurves.randomize();
        modelChanged();
    }

    public void resetAllCurves() {
        toneCurves.reset();
        modelChanged();
    }

    private static boolean isMovableKnot(ToneCurve curve, int index) {
        return index > 0 && index < curve.curveData.x.length - 1;
    }

    private int getDrawingOffsetX() {
        return (getWidth() - ToneCurves.PANEL_SIZE) / 2;
    }

    private int getDrawingOffsetY() {
        return (getHeight() - ToneCurves.PANEL_SIZE) / 2;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) {
            return;
        }

        ToneCurve activeCurve = toneCurves.getActiveCurve();
        if (draggedKnotIndex >= 0) {  // a knot is being dragged
            Point2D.Float mousePos = normalizeMousePos(e);
            if (activeCurve.isDraggedOutOfRange(draggedKnotIndex, mousePos.x, mousePos.y)) {
                // delete the knot if dragged too far and track it for potential restoration
                activeCurve.deleteKnot(draggedKnotIndex);
                deletedKnotIndex = draggedKnotIndex;
                draggedKnotIndex = -1;
                modelChanged();
            } else if (isMovableKnot(activeCurve, draggedKnotIndex)) {
                // move the dragged knot
                activeCurve.setKnotPosition(draggedKnotIndex, mousePos.x, mousePos.y);
                modelChanged();
            }
        } else if (deletedKnotIndex >= 0) { // a knot was just deleted by dragging
            Point2D.Float mousePos = normalizeMousePos(e);
            if (activeCurve.canRestoreKnotAt(deletedKnotIndex, mousePos.x, mousePos.y)) {
                // restore the recently deleted knot if dragged back into range
                draggedKnotIndex = activeCurve.addKnot(mousePos.x, mousePos.y, false);
                deletedKnotIndex = -1;
                modelChanged();
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        ToneCurve activeCurve = toneCurves.getActiveCurve();
        Point2D.Float mousePos = normalizeMousePos(e);
        if (activeCurve.isOverKnot(mousePos)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else if (ToneCurve.isInsideCurveArea(mousePos)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e) || e.getClickCount() > 1) {
            return;
        }

        ToneCurve activeCurve = toneCurves.getActiveCurve();
        // pressing the mouse either selects an existing knot for dragging...
        Point2D.Float mousePos = normalizeMousePos(e);
        draggedKnotIndex = activeCurve.getKnotIndexAt(mousePos);

        if (draggedKnotIndex < 0) {
            // ...or adds a new knot if clicking on an empty area
            draggedKnotIndex = activeCurve.addKnot(mousePos.x, mousePos.y, true);
            if (draggedKnotIndex >= 0) {
                modelChanged();
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) {
            return;
        }

        ToneCurve activeCurve = toneCurves.getActiveCurve();
        // delete a knot by dropping it on top of another one
        if (draggedKnotIndex >= 0
            && isMovableKnot(activeCurve, draggedKnotIndex)
            && activeCurve.overlapsOtherKnot(draggedKnotIndex)) {
            activeCurve.deleteKnot(draggedKnotIndex);
            modelChanged();
        }
        // reset dragging state at the end of the gesture
        draggedKnotIndex = -1;
        deletedKnotIndex = -1;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e) || e.getClickCount() != 2) {
            return;
        }

        ToneCurve activeCurve = toneCurves.getActiveCurve();
        // re-check knot index on double-click to ensure we are on a knot
        Point2D.Float mousePos = normalizeMousePos(e);
        int knotIndex = activeCurve.getKnotIndexAt(mousePos);
        if (isMovableKnot(activeCurve, knotIndex)) {
            // double-clicking on a knot deletes it
            activeCurve.deleteKnot(knotIndex);
            modelChanged();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}

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

package pixelitor.filters.curves;

import pixelitor.filters.util.Channel;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.EventListener;

/**
 * Panel for displaying and interacting with tone curves for [RGB, R ,G, B] channels.
 * It's responsible for handling all user input and for the repainting of the curves.
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurvesPanel extends JPanel implements MouseMotionListener, MouseListener {
    // the ToneCurves instance this panel represents
    public final ToneCurves toneCurves;

    // the fixed size of the drawing area for the curves:
    // grid(255px) + curvePadding(2*10px) + scales(20px)
    private static final int DRAWING_AREA_SIZE = 295;
    private static final Dimension MIN_SIZE = new Dimension(
        DRAWING_AREA_SIZE, DRAWING_AREA_SIZE);

    private int mouseKnotIndex = -1; // index of the knot being dragged
    private int deletedKnotIndex = -1; // index of a knot deleted by dragging it out of bounds
    private final EventListenerList actionListenerList = new EventListenerList();

    public ToneCurvesPanel(ToneCurves toneCurves) {
        this.toneCurves = toneCurves;

        toneCurves.setSize(DRAWING_AREA_SIZE, DRAWING_AREA_SIZE);
        addMouseMotionListener(this);
        addMouseListener(this);
    }

    public void addActionListener(ActionListener listener) {
        actionListenerList.add(ActionListener.class, listener);
    }

    public void removeActionListener(ActionListener listener) {
        actionListenerList.remove(ActionListener.class, listener);
    }

    private void fireActionPerformed(ActionEvent event) {
        EventListener[] listeners = actionListenerList.getListeners(ActionListener.class);
        for (EventListener eventListener : listeners) {
            ((ActionListener) eventListener).actionPerformed(event);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.translate(getDrawingOffsetX(), getDrawingOffsetY());
            toneCurves.draw(g2d);
        } finally {
            g2d.dispose();
        }
    }

    public void stateChanged() {
        repaint();
        // notify listeners that the curve state has changed
        fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
    }

    private Point2D.Float normalizeMousePos(MouseEvent e) {
        var mousePos = new Point2D.Float(e.getX() - getDrawingOffsetX(), e.getY() - getDrawingOffsetY());
        toneCurves.normalizePoint(mousePos);
        return mousePos;
    }

    public void setActiveCurve(Channel channel) {
        toneCurves.setActiveChannel(channel);
        stateChanged();
    }

    public void resetActiveCurve() {
        toneCurves.getActiveCurve().reset();
        stateChanged();
    }

    public void randomize() {
        toneCurves.randomize();
        stateChanged();
    }

    public void resetAllCurves() {
        toneCurves.reset();
        stateChanged();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (mouseKnotIndex >= 0) {  // a knot is being dragged
            Point2D.Float mousePos = normalizeMousePos(e);
            if (toneCurves.getActiveCurve().isDraggedOutOfRange(mouseKnotIndex, mousePos)) {
                // delete the knot if dragged too far and track it for potential restoration
                toneCurves.getActiveCurve().deleteKnot(mouseKnotIndex);
                deletedKnotIndex = mouseKnotIndex;
                mouseKnotIndex = -1;
            } else {
                // move the dragged knot
                toneCurves.getActiveCurve().setKnotPosition(mouseKnotIndex, mousePos);
            }
            stateChanged();
        } else if (deletedKnotIndex >= 0) { // a knot was just deleted by dragging
            Point2D.Float mousePos = normalizeMousePos(e);
            if (toneCurves.getActiveCurve().isDraggedIn(deletedKnotIndex, mousePos)) {
                // restore the recently deleted knot if dragged back into range
                mouseKnotIndex = toneCurves.getActiveCurve().addKnot(mousePos, false);
                deletedKnotIndex = -1;
                stateChanged();
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Point2D.Float mousePos = normalizeMousePos(e);
        ToneCurve activeCurve = toneCurves.getActiveCurve();
        if (activeCurve.isOverKnot(mousePos)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else if (ToneCurve.isOverChart(mousePos)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getClickCount() > 1 || e.isConsumed()) {
            return;
        }

        // pressing the mouse either selects an existing knot for dragging...
        Point2D.Float mousePos = normalizeMousePos(e);
        mouseKnotIndex = toneCurves.getActiveCurve().getKnotIndexAt(mousePos);

        if (mouseKnotIndex < 0) {
            e.consume();
            // ...or adds a new knot if clicking on an empty area
            mouseKnotIndex = toneCurves.getActiveCurve().addKnot(mousePos, true);
            stateChanged();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // delete a knot by dropping it on top of another one
        if (mouseKnotIndex >= 0 && toneCurves.getActiveCurve().isOverKnot(mouseKnotIndex)) {
            toneCurves.getActiveCurve().deleteKnot(mouseKnotIndex);
            stateChanged();
        }
        // reset dragging state at the end of the gesture
        mouseKnotIndex = -1;
        deletedKnotIndex = -1;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // do nothing
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // do nothing
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && !e.isConsumed()) {
            e.consume();
            // re-check knot index on double-click to ensure we are on a knot
            Point2D.Float mousePos = normalizeMousePos(e);
            int knotIndex = toneCurves.getActiveCurve().getKnotIndexAt(mousePos);
            if (knotIndex >= 0) {
                // double-clicking on a knot deletes it
                toneCurves.getActiveCurve().deleteKnot(knotIndex);
                stateChanged();
            }
        }
    }

    @Override
    public Dimension getMinimumSize() {
        return MIN_SIZE;
    }

    @Override
    public Dimension getPreferredSize() {
        return MIN_SIZE;
    }

    private int getDrawingOffsetX() {
        return (getWidth() - DRAWING_AREA_SIZE) / 2;
    }

    private int getDrawingOffsetY() {
        return (getHeight() - DRAWING_AREA_SIZE) / 2;
    }
}

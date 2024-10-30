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

package pixelitor.filters.curves;

import pixelitor.filters.levels.Channel;

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
    // The ToneCurves instance this panel represents
    public final ToneCurves toneCurves;

    private int mouseKnotIndex = -1; // the index of the dragged knot
    private int deletedKnotIndex = -1; // the index of the recently deleted knot
    private final EventListenerList actionListenerList = new EventListenerList();
    private final Dimension panelSize;

    public ToneCurvesPanel(ToneCurves toneCurves) {
        this.toneCurves = toneCurves;

        //size: grid(255px) + curvePadding(2*10px) + scales(20px)
        panelSize = new Dimension(295, 295);

        setPreferredSize(panelSize);
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
    public void paint(Graphics g) {
        super.paint(g);

        toneCurves.setSize(panelSize.width, panelSize.height);
        toneCurves.draw((Graphics2D) g);
    }

    public void stateChanged() {
        repaint();

        // notifies the actions listeners that the curve state has changed
        fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
    }

    private Point2D.Float normalizeMousePos(MouseEvent e) {
        var mousePos = new Point2D.Float(e.getX(), e.getY());
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

    public void resetAllCurves() {
        toneCurves.reset();
        stateChanged();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (mouseKnotIndex >= 0) {  // we are dragging a knot
            Point2D.Float mousePos = normalizeMousePos(e);
            if (toneCurves.getActiveCurve().isDraggedOutOfRange(mouseKnotIndex, mousePos)) {
                // delete the dragged knot
                toneCurves.getActiveCurve().deleteKnot(mouseKnotIndex);
                deletedKnotIndex = mouseKnotIndex;
                mouseKnotIndex = -1;
            } else {
                // move the dragged knot
                toneCurves.getActiveCurve().setKnotPosition(mouseKnotIndex, mousePos);
            }

            stateChanged();
        } else if (deletedKnotIndex >= 0) { // we used to drag a now deleted knot
            Point2D.Float mousePos = normalizeMousePos(e);
            if (toneCurves.getActiveCurve().isDraggedIn(deletedKnotIndex, mousePos)) {
                // add the recently deleted knot back
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

        // pressing the mouse either selects an existing know for dragging...
        Point2D.Float mousePos = normalizeMousePos(e);
        mouseKnotIndex = toneCurves.getActiveCurve().getKnotIndexAt(mousePos);

        if (mouseKnotIndex < 0) {
            e.consume();
            // ...or adds a new knot
            mouseKnotIndex = toneCurves.getActiveCurve().addKnot(mousePos, true);
            stateChanged();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (mouseKnotIndex >= 0) {
            if (toneCurves.getActiveCurve().isOverKnot(mouseKnotIndex)) {
                toneCurves.getActiveCurve().deleteKnot(mouseKnotIndex);
                stateChanged();
            }
        }
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
        if (e.getClickCount() == 2 && !e.isConsumed() && mouseKnotIndex >= 0) {
            e.consume();
            // double-clicking on a knot deletes it
            toneCurves.getActiveCurve().deleteKnot(mouseKnotIndex);
            stateChanged();
        }
    }
}

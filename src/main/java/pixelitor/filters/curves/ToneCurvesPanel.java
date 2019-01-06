/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.EventListener;

/**
 * ToneCurvesFilter Panel container for [RGB,R,G,B] curves
 * responsible for handling all user input and repainting curves
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurvesPanel extends JPanel implements MouseMotionListener, MouseListener {
    public final ToneCurves toneCurves = new ToneCurves();
    private int mouseKnotIndex = -1;
    private int mouseKnotIndexDeleted = -1;
    private final BufferedImage img;
    private final EventListenerList actionListenerList = new EventListenerList();

    public ToneCurvesPanel() {
        //size: grid(255px) + curvePadding(2*10px) + scales(20px)
        Dimension size = new Dimension(295, 295);
        this.setPreferredSize(size);
        addMouseMotionListener(this);
        addMouseListener(this);

        img = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);

        toneCurves.setG2D(img.createGraphics());
        toneCurves.setSize(img.getWidth(), img.getHeight());
        toneCurves.draw();
    }

    public void addActionListener(ActionListener actionListener) {
        actionListenerList.add(ActionListener.class, actionListener);
    }

    public void removeActionListener(ActionListener actionListener) {
        actionListenerList.remove(ActionListener.class, actionListener);
    }

    private void fireActionPerformed(ActionEvent actionEvent) {
        EventListener[] listenerList = actionListenerList.getListeners(ActionListener.class);
        for (EventListener eventListener : listenerList) {
            ((ActionListener) eventListener).actionPerformed(actionEvent);
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        g.drawImage(img, 0, 0, null);
    }

    private void stateChanged() {
        toneCurves.draw();
        repaint();

        fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
    }

    private Point.Float getNormalizedMousePos(MouseEvent e) {
        Point.Float mousePos = new Point.Float(e.getX(), e.getY());
        toneCurves.normalizePoint(mousePos);
        return mousePos;
    }

    public void setActiveCurve(ToneCurveType type) {
        toneCurves.setActiveCurve(type);
        stateChanged();
    }

    public void resetActiveCurve() {
        toneCurves.getActiveCurve().reset();
        stateChanged();
    }

    public void reset() {
        toneCurves.reset();
        stateChanged();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (mouseKnotIndex >= 0) {
            Point.Float mousePos = getNormalizedMousePos(e);
            if (toneCurves.getActiveCurve().isDraggedOff(mouseKnotIndex, mousePos)) {
                toneCurves.getActiveCurve().deleteKnot(mouseKnotIndex);
                mouseKnotIndexDeleted = mouseKnotIndex;
                mouseKnotIndex = -1;
            } else {
                toneCurves.getActiveCurve().setKnotPosition(mouseKnotIndex, mousePos);
            }

            stateChanged();
        } else if (mouseKnotIndexDeleted >= 0) {
            Point.Float mousePos = getNormalizedMousePos(e);
            if (toneCurves.getActiveCurve().isDraggedIn(mouseKnotIndexDeleted, mousePos)) {
                mouseKnotIndex = toneCurves.getActiveCurve().addKnot(mousePos, false);
                mouseKnotIndexDeleted = -1;
                stateChanged();
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Point.Float mousePos = getNormalizedMousePos(e);
        ToneCurve activeCurve = toneCurves.getActiveCurve();
        if (activeCurve.isOverKnot(mousePos)) {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else if (ToneCurve.isOverChart(mousePos)) {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getClickCount() > 1 || e.isConsumed()) {
            return;
        }

        Point.Float mousePos = getNormalizedMousePos(e);
        mouseKnotIndex = toneCurves.getActiveCurve().getKnotIndexAt(mousePos);

        if (mouseKnotIndex < 0) {
            e.consume();
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
        // nothing
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // nothing
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() ==2 && !e.isConsumed() && mouseKnotIndex >= 0) {
            e.consume();
            toneCurves.getActiveCurve().deleteKnot(mouseKnotIndex);
            stateChanged();
        }
    }
}

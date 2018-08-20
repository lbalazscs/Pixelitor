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

import pixelitor.gui.View;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Shapes;
import pixelitor.utils.debug.Ansi;

import javax.swing.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.geom.Line2D;

import static pixelitor.tools.pen.AnchorPointType.CUSP;
import static pixelitor.tools.pen.AnchorPointType.SMOOTH;
import static pixelitor.tools.pen.AnchorPointType.SYMMETRIC;

/**
 * A point on a {@link SubPath}
 */
public class AnchorPoint extends DraggablePoint {
    public final ControlPoint ctrlIn;
    public final ControlPoint ctrlOut;
    private SubPath path;

    private AnchorPointType type = SYMMETRIC;

    private static final Color ANCHOR_COLOR = Color.WHITE;
    private static final Color ANCHOR_ACTIVE_COLOR = Color.RED;
    private static final Color CTRL_IN_COLOR = Color.WHITE;
    private static final Color CTRL_IN_ACTIVE_COLOR = Color.RED;
    private static final Color CTRL_OUT_COLOR = Color.WHITE;
    private static final Color CTRL_OUT_ACTIVE_COLOR = Color.RED;

    public AnchorPoint(double x, double y, View view) {
        super("anchor", x, y, view, ANCHOR_COLOR, ANCHOR_ACTIVE_COLOR);

        ctrlIn = new ControlPoint("ctrlIn", x, y, view, this,
                CTRL_IN_COLOR, CTRL_IN_ACTIVE_COLOR);
        ctrlOut = new ControlPoint("ctrlOut", x, y, view, this,
                CTRL_OUT_COLOR, CTRL_OUT_ACTIVE_COLOR);
        ctrlIn.setSibling(ctrlOut);
        ctrlOut.setSibling(ctrlIn);
    }

    public AnchorPoint(PPoint p) {
        this(p.getCoX(), p.getCoY(), p.getIC());
    }

    public AnchorPoint(AnchorPoint other, boolean copyControlPositions) {
        this(other.x, other.y, other.view);
        this.path = other.path;
        this.type = other.type;
        if (copyControlPositions) {
            this.ctrlIn.x = other.ctrlIn.x;
            this.ctrlIn.y = other.ctrlIn.y;
            this.ctrlOut.x = other.ctrlOut.x;
            this.ctrlOut.y = other.ctrlOut.y;
        }
    }

    public void paintHandles(Graphics2D g, boolean paintIn, boolean paintOut) {
        boolean ctrlOutActive = ctrlOut.isActive();
        boolean ctrlInActive = ctrlIn.isActive();
        boolean ctrlOutRetracted = ctrlOut.isRetracted();
        boolean ctrlInRetracted = ctrlIn.isRetracted();

        if (paintIn && !ctrlInRetracted) {
            Line2D.Double lineIn = new Line2D.Double(x, y, ctrlIn.x, ctrlIn.y);
            Shapes.drawVisible(g, lineIn);
            if (!ctrlInActive) {
                ctrlIn.paintHandle(g);
            }
        }
        if (paintOut && !ctrlOutRetracted) {
            Line2D.Double lineOut = new Line2D.Double(x, y, ctrlOut.x, ctrlOut.y);
            Shapes.drawVisible(g, lineOut);
            if (!ctrlOutActive) {
                ctrlOut.paintHandle(g);
            }
        }

        paintHandle(g);

        // paint active control points on the top, even if they are retracted
        if (ctrlOutActive) {
            ctrlOut.paintHandle(g);
        } else if (ctrlInActive) {
            ctrlIn.paintHandle(g);
        }
    }

    @Override
    public void setLocation(double x, double y) {
        double oldX = this.x;
        double oldY = this.y;
        super.setLocation(x, y);

        double dx = x - oldX;
        double dy = y - oldY;

        ctrlOut.translateOnlyThis(dx, dy);
        ctrlIn.translateOnlyThis(dx, dy);
    }

    public DraggablePoint handleOrCtrlHandleWasHit(int x, int y, boolean altDown) {
        if (altDown) {
            // check the control handles first so that
            // retracted handles can be dragged out with Alt-drag
            if (ctrlOut.handleContains(x, y)) {
                return ctrlOut;
            }
            if (ctrlIn.handleContains(x, y)) {
                return ctrlIn;
            }
            if (handleContains(x, y)) {
                return this;
            }
        } else {
            // check the anchor handle first
            if (handleContains(x, y)) {
                return this;
            }
            if (ctrlOut.handleContains(x, y)) {
                return ctrlOut;
            }
            if (ctrlIn.handleContains(x, y)) {
                return ctrlIn;
            }
        }
        return null;
    }

    @Override
    protected void afterMouseReleasedActions() {
        calcImCoords();
        ctrlIn.calcImCoords();
        ctrlOut.calcImCoords();
    }

    public AnchorPointType getType() {
        return type;
    }

    public void setType(AnchorPointType type) {
        this.type = type;
    }

    public void changeTypeForEditing(boolean pathWasBuiltInteractively) {
        boolean inRetracted = ctrlIn.isRetracted(1.0);
        boolean outRetracted = ctrlOut.isRetracted(1.0);

        if (inRetracted && outRetracted) {
            // so that they can be easily dragged out
            type = SYMMETRIC;
        } else if (inRetracted || outRetracted) {
            type = CUSP;
        } else if (pathWasBuiltInteractively) {
            type = SMOOTH;
        } else {
            type = calcHeuristicType();
        }
    }

    // tries to determine a type based on the current
    // positions of control points
    private AnchorPointType calcHeuristicType() {
        double dOutX = ctrlOut.x - this.x;
        double dOutY = ctrlOut.y - this.y;
        double dInX = ctrlIn.x - this.x;
        double dInY = ctrlIn.y - this.y;

        double symThreshold = 2.0;
        if (Math.abs(dOutX + dInX) < symThreshold
                && Math.abs(dOutY + dInY) < symThreshold) {
            return SYMMETRIC;
        }

        // Are they at least collinear?
        // Checks the slope equality while avoids dividing by 0
        if (Math.abs(dOutY * dInX - dOutX * dInY) < 0.1) {
            return SMOOTH;
        }

        return CUSP;
    }

    public void showPopup(int x, int y) {
        JPopupMenu p = new JPopupMenu();
        AnchorPointType.addTypePopupItems(this, p);
        p.addSeparator();
        p.add(new AbstractAction("Dump") {
            @Override
            public void actionPerformed(ActionEvent e) {
                AnchorPoint.this.dump();
            }
        });
        p.add(new AbstractAction("Delete Point") {
            @Override
            public void actionPerformed(ActionEvent e) {
                AnchorPoint.this.delete();
            }
        });
        p.add(new AbstractAction("Retract Handles") {
            @Override
            public void actionPerformed(ActionEvent e) {
                AnchorPoint.this.retractHandles();
            }
        });
        p.show((JComponent) view, x, y);
    }

    private void retractHandles() {
        ctrlIn.retract();
        ctrlOut.retract();
        setType(SYMMETRIC);
        view.repaint();
    }

    public void setPath(SubPath path) {
        this.path = path;
    }

    @Override
    public void setView(View view) {
        super.setView(view);
        ctrlIn.setView(view);
        ctrlOut.setView(view);
    }

    private void delete() {
        path.deletePoint(this);
        view.repaint();
    }

    public void dump() {
        System.out.println(Ansi.red(getType()));
        System.out.println("    " + toColoredString());
        System.out.println("    " + ctrlIn.toColoredString());
        System.out.println("    " + ctrlOut.toColoredString());
    }
}

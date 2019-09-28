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

import pixelitor.Build;
import pixelitor.gui.View;
import pixelitor.history.History;
import pixelitor.tools.pen.history.AnchorPointChangeEdit;
import pixelitor.tools.pen.history.SubPathEdit;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Shapes;
import pixelitor.utils.debug.Ansi;

import javax.swing.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.IllegalComponentStateException;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
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
    private final SubPath subPath;

    private static long debugCounter = 0;

    private AnchorPointType type = SYMMETRIC;

    private static final Color ANCHOR_COLOR = Color.WHITE;
    private static final Color ANCHOR_ACTIVE_COLOR = Color.RED;
    private static final Color CTRL_IN_COLOR = Color.WHITE;
    private static final Color CTRL_IN_ACTIVE_COLOR = Color.RED;
    private static final Color CTRL_OUT_COLOR = Color.WHITE;
    private static final Color CTRL_OUT_ACTIVE_COLOR = Color.RED;

    // not to be confused with DraggablePoint.lastActive!
    public static AnchorPoint recentlyEditedPoint = null;

    public AnchorPoint(double x, double y, View view, SubPath subPath) {
        super("AP" + debugCounter++, x, y, view, ANCHOR_COLOR, ANCHOR_ACTIVE_COLOR);
        this.subPath = subPath;

        ctrlIn = new ControlPoint("ctrlIn", x, y, view, this,
                CTRL_IN_COLOR, CTRL_IN_ACTIVE_COLOR);
        ctrlOut = new ControlPoint("ctrlOut", x, y, view, this,
                CTRL_OUT_COLOR, CTRL_OUT_ACTIVE_COLOR);
        ctrlIn.setSibling(ctrlOut);
        ctrlOut.setSibling(ctrlIn);
    }

    public AnchorPoint(PPoint p, SubPath subPath) {
        this(p.getCoX(), p.getCoY(), p.getView(), subPath);
    }

    public AnchorPoint(AnchorPoint other, SubPath subPath,
                       boolean copyControlPositions) {
        this(other.x, other.y, other.view, subPath);
        this.type = other.type;
        if (copyControlPositions) {
            this.ctrlIn.copyPositionFrom(other.ctrlIn);
            this.ctrlOut.copyPositionFrom(other.ctrlOut);
        }
    }

    public void paintHandles(Graphics2D g, boolean paintIn, boolean paintOut) {
        boolean ctrlOutActive = ctrlOut.isActive();
        boolean ctrlInActive = ctrlIn.isActive();

        if (paintIn && !ctrlIn.isRetracted()) {
            Line2D.Double lineIn = new Line2D.Double(x, y, ctrlIn.x, ctrlIn.y);
            Shapes.drawVisible(g, lineIn);
            if (!ctrlInActive) {
                ctrlIn.paintHandle(g);
            }
        }
        if (paintOut && !ctrlOut.isRetracted()) {
            Line2D.Double lineOut = new Line2D.Double(x, y, ctrlOut.x, ctrlOut.y);
            Shapes.drawVisible(g, lineOut);
            if (!ctrlOutActive) {
                ctrlOut.paintHandle(g);
            }
        }

        paintHandle(g);

        // the active control handles should be painted after (over) the anchor handle,
        // even if they are retracted, otherwise it looks wrong when dragging them out
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

    @Override
    public void storeTransformRefPoint() {
        super.storeTransformRefPoint();
        ctrlIn.storeTransformRefPoint();
        ctrlOut.storeTransformRefPoint();
    }

    @Override
    public void imTransform(AffineTransform at, boolean useRefPoint) {
        imTransformOnlyThis(at, useRefPoint);
        ctrlIn.imTransformOnlyThis(at, useRefPoint);
        ctrlOut.imTransformOnlyThis(at, useRefPoint);
    }

    public DraggablePoint handleOrCtrlHandleWasHit(double x, double y,
                                                   boolean altDown) {
        if (altDown) {
            // check the control handles first, so that
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

    public AnchorPointType getType() {
        return type;
    }

    public void setType(AnchorPointType type) {
        this.type = type;
    }

    public void changeTypeFromSymToSmooth() {
        if (type == SYMMETRIC) { // set to smooth only if it wasn't broken
            setType(SMOOTH);
        }
    }

    public void setHeuristicType() {
        boolean inRetracted = ctrlIn.isRetracted(1.0);
        boolean outRetracted = ctrlOut.isRetracted(1.0);

        if (inRetracted && outRetracted) {
            // so that they can be easily dragged out
            type = SYMMETRIC;
        } else if (inRetracted || outRetracted) {
            // so that dragging out the retraced doesn't cause surprises
            type = CUSP;
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

        p.add(new AbstractAction("Retract Handles") {
            @Override
            public void actionPerformed(ActionEvent e) {
                AnchorPoint.this.retractHandles();
            }
        });

        p.addSeparator();

        if (Build.isDevelopment()) {
            p.add(new AbstractAction("Dump") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    AnchorPoint.this.dump();
                }
            });
        }

        boolean singleSubPath = subPath.isSingle();
        boolean isLastPoint = singleSubPath && subPath.getNumAnchors() == 1;

        if (!isLastPoint) {
            p.add(new AbstractAction("Delete Point") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    AnchorPoint.this.delete();
                }
            });
        }

        if (!singleSubPath) {
            p.add(new AbstractAction("Delete Subpath") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    subPath.delete();
                }
            });
        }

        p.add(new AbstractAction("Delete Path") {
            @Override
            public void actionPerformed(ActionEvent e) {
                subPath.deletePath();
            }
        });

        try {
            p.show(view, x, y);
        } catch (IllegalComponentStateException e) {
            // ignore: happens in RandomGUITest, but works OK otherwise
            // ("component must be showing on the screen to determine its location")
            // probably related to the always-on-top state of RandomGUITest,
            // see https://bugs.openjdk.java.net/browse/JDK-8179665
        }
    }

    public void retractHandles() {
        AnchorPoint backup = new AnchorPoint(this, subPath, true);
        ctrlIn.retract();
        ctrlOut.retract();
        setType(SYMMETRIC);
        view.repaint();

        History.addEdit(new AnchorPointChangeEdit("Retract Handles",
                subPath.getComp(), backup, this));
    }

    @Override
    public void setView(View view) {
        super.setView(view);
        ctrlIn.setView(view);
        ctrlOut.setView(view);
    }

    public void delete() {
        SubPath backup = this.subPath.copyForUndo(subPath.getPath());
        this.subPath.deletePoint(this);
        History.addEdit(new SubPathEdit("Delete Anchor Point",
                backup, subPath));
        view.repaint();
    }

    public boolean isRecentlyEdited() {
        return this == recentlyEditedPoint;
    }

    @Override
    public void setActive(boolean active) {
        super.setActive(active);
        if (active) {
            recentlyEditedPoint = this;
        }
    }

    public SubPath getSubPath() {
        return subPath;
    }

    @Override
    public String getMoveEditName() {
        return "Move Anchor Point";
    }

    public void dump() {
        System.out.println(Ansi.red(getType()));
        System.out.println("    " + toColoredString());
        System.out.println("    " + ctrlIn.toColoredString());
        System.out.println("    " + ctrlOut.toColoredString());
    }
}

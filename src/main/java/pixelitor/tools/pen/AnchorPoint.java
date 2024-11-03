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
import pixelitor.gui.utils.PAction;
import pixelitor.history.History;
import pixelitor.tools.pen.history.AnchorPointChangeEdit;
import pixelitor.tools.pen.history.SubPathEdit;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Shapes;
import pixelitor.utils.debug.Ansi;

import javax.swing.*;
import java.awt.Graphics2D;
import java.awt.IllegalComponentStateException;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.io.Serial;

import static pixelitor.tools.pen.AnchorPointType.CUSP;
import static pixelitor.tools.pen.AnchorPointType.SMOOTH;
import static pixelitor.tools.pen.AnchorPointType.SYMMETRIC;

/**
 * A point on a {@link SubPath}
 */
public class AnchorPoint extends DraggablePoint {
    // compatible with Pixelitor 4.2.3
    @Serial
    private static final long serialVersionUID = -7001569188242665053L;

    public final ControlPoint ctrlIn;
    public final ControlPoint ctrlOut;
    private final SubPath subPath;

    private static long debugCounter = 0;

    private AnchorPointType type = SYMMETRIC;

    // not to be confused with DraggablePoint.lastActive!
    public static AnchorPoint recentlyEditedPoint = null;

    public AnchorPoint(double coX, double coY, View view, SubPath subPath) {
        this(new PPoint(coX, coY, view), view, subPath);
    }

    public AnchorPoint(PPoint p, View view, SubPath subPath) {
        super("AP" + debugCounter++, p, view);

        this.subPath = subPath;

        ctrlIn = new ControlPoint("ctrlIn", p, view, this);
        ctrlOut = new ControlPoint("ctrlOut", p, view, this);
        ctrlIn.setSibling(ctrlOut);
        ctrlOut.setSibling(ctrlIn);
    }

    public AnchorPoint(PPoint p, SubPath subPath) {
        this(p, p.getView(), subPath);
    }

    public AnchorPoint(AnchorPoint other, SubPath subPath,
                       boolean copyControlPositions) {
        this(other.x, other.y, other.view, subPath);
        type = other.type;
        if (copyControlPositions) {
            ctrlIn.copyPositionFrom(other.ctrlIn);
            ctrlOut.copyPositionFrom(other.ctrlOut);
        }
    }

    public void paintHandles(Graphics2D g, boolean paintIn, boolean paintOut) {
        boolean ctrlOutActive = ctrlOut.isActive();
        boolean ctrlInActive = ctrlIn.isActive();

        if (paintIn && !ctrlIn.isRetracted()) {
            paintControlHandle(ctrlIn, ctrlInActive, g);
        }
        if (paintOut && !ctrlOut.isRetracted()) {
            paintControlHandle(ctrlOut, ctrlOutActive, g);
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

    private void paintControlHandle(ControlPoint controlPoint,
                                    boolean ctrlActive,
                                    Graphics2D g) {
        Shapes.drawVisibly(g, new Line2D.Double(x, y, controlPoint.x, controlPoint.y));
        if (!ctrlActive) {
            controlPoint.paintHandle(g);
        }
    }

    @Override
    public void setLocation(double x, double y) {
        double dx = x - this.x;
        double dy = y - this.y;

        super.setLocation(x, y);

        ctrlOut.translateOnlyThis(dx, dy);
        ctrlIn.translateOnlyThis(dx, dy);
    }

    @Override
    public void saveImTransformRefPoint() {
        super.saveImTransformRefPoint();
        ctrlIn.saveImTransformRefPoint();
        ctrlOut.saveImTransformRefPoint();
    }

    @Override
    public void imTransform(AffineTransform at, boolean useRefPoint) {
        imTransformOnlyThis(at, useRefPoint);
        ctrlIn.imTransformOnlyThis(at, useRefPoint);
        ctrlOut.imTransformOnlyThis(at, useRefPoint);
    }

    @Override
    public void imTranslate(double dx, double dy) {
        super.imTranslate(dx, dy);
        ctrlIn.imTranslate(dx, dy);
        ctrlOut.imTranslate(dx, dy);
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
        double dOutX = ctrlOut.x - x;
        double dOutY = ctrlOut.y - y;
        double dInX = ctrlIn.x - x;
        double dInY = ctrlIn.y - y;

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
        var popup = new JPopupMenu();

        for (var anchorPointType : AnchorPointType.values()) {
            popup.add(anchorPointType.createConvertMenuItem(this));
        }

        popup.addSeparator();

        popup.add(new PAction("Retract Handles", this::retractHandles));

        popup.addSeparator();

        if (AppMode.isDevelopment()) {
            popup.add(new PAction("Dump", this::dump));
        }

        boolean singleSubPath = subPath.isSingle();
        boolean isLastPoint = singleSubPath && subPath.getNumAnchors() == 1;

        if (!isLastPoint) {
            popup.add(new PAction("Delete Point", this::delete));
        }

        if (!singleSubPath) {
            popup.add(new PAction("Delete Subpath", subPath::delete));
        }

        popup.add(new PAction("Delete Path", subPath::deletePath));

        try {
            popup.show(view, x, y);
        } catch (IllegalComponentStateException e) {
            // ignore: happens in RandomGUITest, but works OK otherwise
            // ("component must be showing on the screen to determine its location")
            // probably related to the always-on-top state of RandomGUITest,
            // see https://bugs.openjdk.java.net/browse/JDK-8179665
        }
    }

    public void retractHandles() {
        var backup = new AnchorPoint(this, subPath, true);
        ctrlIn.retract();
        ctrlOut.retract();
        setType(SYMMETRIC);
        view.repaint();

        History.add(new AnchorPointChangeEdit("Retract Handles",
            subPath.getComp(), backup, this));
    }

    @Override
    public void setView(View view) {
        super.setView(view);
        ctrlIn.setView(view);
        ctrlOut.setView(view);
    }

    public void delete() {
        SubPath backup = subPath.deepCopy(subPath.getPath(), view.getComp());
        subPath.deletePoint(this);
        History.add(new SubPathEdit(
            "Delete Anchor Point", backup, subPath));
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

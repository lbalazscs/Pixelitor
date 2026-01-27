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

package pixelitor.tools.pen;

import pixelitor.AppMode;
import pixelitor.gui.View;
import pixelitor.gui.utils.TaskAction;
import pixelitor.history.History;
import pixelitor.tools.pen.history.AnchorPointChangeEdit;
import pixelitor.tools.pen.history.SubPathEdit;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Shapes;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.Graphics2D;
import java.awt.IllegalComponentStateException;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.Serial;

import static pixelitor.tools.pen.AnchorPointType.CUSP;
import static pixelitor.tools.pen.AnchorPointType.SMOOTH;
import static pixelitor.tools.pen.AnchorPointType.SYMMETRIC;

/**
 * An anchor point on a {@link SubPath}.
 */
public class AnchorPoint extends DraggablePoint {
    @Serial
    private static final long serialVersionUID = -7001569188242665053L;

    private static final double SYMMETRY_THRESHOLD = 2.0;
    private static final double COLLINEARITY_THRESHOLD = 0.1;
    public static final double RETRACTION_TOLERANCE = 1.0;

    // an anchor point has two associated control points that define
    // the curvature of the path segments connected to this point
    public final ControlPoint ctrlIn;
    public final ControlPoint ctrlOut;

    private final SubPath subPath;

    private static long idCounter = 0;

    private AnchorPointType type = SYMMETRIC;

    // not to be confused with DraggablePoint.lastActive!
    public static AnchorPoint recentlyEditedPoint = null;

    public AnchorPoint(double coX, double coY, View view, SubPath subPath) {
        this(new PPoint(coX, coY, view), view, subPath);
    }

    public AnchorPoint(PPoint pos, View view, SubPath subPath) {
        super("AP" + idCounter++, pos, view);

        this.subPath = subPath;

        ctrlIn = new ControlPoint(name + " ctrlIn", pos, view, this);
        ctrlOut = new ControlPoint(name + " ctrlOut", pos, view, this);
        ctrlIn.setSibling(ctrlOut);
        ctrlOut.setSibling(ctrlIn);
    }

    public AnchorPoint(PPoint p, SubPath subPath) {
        this(p, p.getView(), subPath);
    }

    /**
     * Creates a copy of an existing anchor point.
     */
    public AnchorPoint(AnchorPoint source, SubPath newParent,
                       boolean copyControlPositions) {
        this(source.x, source.y, source.view, newParent);
        type = source.type;
        if (copyControlPositions) {
            ctrlIn.copyPositionFrom(source.ctrlIn);
            ctrlOut.copyPositionFrom(source.ctrlOut);
        }
    }

    public void paintHandles(Graphics2D g, boolean paintIn, boolean paintOut) {
        boolean ctrlOutActive = ctrlOut.isActive();
        boolean ctrlInActive = ctrlIn.isActive();

        // paint the lines and the inactive handles first
        if (paintIn && !ctrlIn.isRetracted()) {
            paintControl(ctrlIn, ctrlInActive, g);
        }
        if (paintOut && !ctrlOut.isRetracted()) {
            paintControl(ctrlOut, ctrlOutActive, g);
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

    private void paintControl(ControlPoint controlPoint,
                              boolean ctrlActive,
                              Graphics2D g) {
        assert checkShapeBounds();
        assert controlPoint.checkShapeBounds();

        Shapes.drawVisibly(g, new Line2D.Double(x, y, controlPoint.x, controlPoint.y));
        if (!ctrlActive) {
            controlPoint.paintHandle(g);
        }
    }

    @Override
    public void setLocation(double coX, double coY) {
        double dx = coX - this.x;
        double dy = coY - this.y;

        super.setLocation(coX, coY);

        // move the control points along with the anchor
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

    /**
     * Tries to locate a handle (anchor or control) at the given coordinates.
     * When Alt is pressed, it prioritizes finding control handles over the anchor point.
     */
    public DraggablePoint findHandleAt(double x, double y,
                                       boolean altDown) {
        return altDown
            ? findControlHandleFirst(x, y)
            : findAnchorHandleFirst(x, y);
    }

    // checks the control handles first, so that
    // retracted handles can be dragged out with Alt-drag
    private DraggablePoint findControlHandleFirst(double x, double y) {
        if (ctrlOut.contains(x, y)) {
            return ctrlOut;
        }
        if (ctrlIn.contains(x, y)) {
            return ctrlIn;
        }
        if (contains(x, y)) {
            return this;
        }
        return null;
    }

    // checks the anchor handle first
    private DraggablePoint findAnchorHandleFirst(double x, double y) {
        if (contains(x, y)) {
            return this;
        }
        if (ctrlOut.contains(x, y)) {
            return ctrlOut;
        }
        if (ctrlIn.contains(x, y)) {
            return ctrlIn;
        }
        return null;
    }

    public AnchorPointType getType() {
        return type;
    }

    public void setType(AnchorPointType type) {
        if (this.type == type) {
            return;
        }
        this.type = type;
        if (type == SMOOTH) {
            ctrlIn.rememberDistFromAnchor();
            ctrlOut.rememberDistFromAnchor();
        }
    }

    public void changeTypeFromSymToSmooth() {
        if (type == SYMMETRIC) { // set to smooth only if it wasn't broken
            setType(SMOOTH);
        }
    }

    /**
     * Determines the appropriate handle type based on control point positions.
     */
    public void setHeuristicType() {
        boolean inRetracted = ctrlIn.isRetracted(RETRACTION_TOLERANCE);
        boolean outRetracted = ctrlOut.isRetracted(RETRACTION_TOLERANCE);

        if (inRetracted && outRetracted) {
            // so that they can be easily dragged out
            setType(SYMMETRIC);
        } else if (inRetracted || outRetracted) {
            // so that dragging out the retraced doesn't cause surprises
            setType(CUSP);
        } else {
            setType(calcHeuristicType());
        }
    }

    private AnchorPointType calcHeuristicType() {
        double dOutX = ctrlOut.x - x;
        double dOutY = ctrlOut.y - y;
        double dInX = ctrlIn.x - x;
        double dInY = ctrlIn.y - y;

        // Are they symmetric?
        if (Math.abs(dOutX + dInX) < SYMMETRY_THRESHOLD
            && Math.abs(dOutY + dInY) < SYMMETRY_THRESHOLD) {
            return SYMMETRIC;
        }

        // Are they at least collinear?
        // Checks the slope equality while avoids dividing by 0
        if (Math.abs(dOutY * dInX - dOutX * dInY) < COLLINEARITY_THRESHOLD) {
            return SMOOTH;
        }

        return CUSP;
    }

    public void showPopup(PMouseEvent e) {
        var popup = new JPopupMenu();

        for (var anchorPointType : AnchorPointType.values()) {
            popup.add(anchorPointType.createConvertMenuItem(this));
        }

        popup.addSeparator();

        popup.add(new TaskAction("Retract Handles", this::retractHandles));

        String flipName = subPath.isSingle()
            ? "Flip Path Direction"
            : "Flip Subpath Direction";
        popup.add(new TaskAction(flipName, () -> subPath.flipDirection(flipName)));

        popup.addSeparator();

        if (AppMode.isDevelopment()) {
            popup.add(new TaskAction("Debug Anchor", this::showDebugDialog));
            popup.add(new TaskAction("Debug Subpath", subPath::showDebugDialog));
            popup.add(new TaskAction("Debug Path", () -> subPath.getPath().showDebugDialog()));
            popup.addSeparator();
        }

        boolean singleSubPath = subPath.isSingle();
        boolean isLastPoint = singleSubPath && subPath.getNumAnchors() == 1;

        if (!isLastPoint) {
            popup.add(new TaskAction("Delete Point", this::delete));
        }

        if (!singleSubPath) {
            popup.add(new TaskAction("Delete Subpath", subPath::delete));
        }

        popup.add(new TaskAction("Delete Path", subPath::deletePath));

        try {
            popup.show(view, (int) e.getCoX(), (int) e.getCoY());
        } catch (IllegalComponentStateException ex) {
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

    public void swapControlPositions() {
        Point2D prevInPos = ctrlIn.getImLocationCopy();
        Point2D prevOutPos = ctrlOut.getImLocationCopy();

        ctrlIn.setImLocationOnlyForThis(prevOutPos);
        ctrlOut.setImLocationOnlyForThis(prevInPos);
    }

    @Override
    public void setView(View view) {
        super.setView(view);
        ctrlIn.setView(view);
        ctrlOut.setView(view);
    }

    public void delete() {
        SubPath backup = subPath.deepCopy(subPath.getPath(), view.getComp());
        subPath.deleteAnchor(this);
        History.add(new SubPathEdit(
            "Delete Anchor Point", backup, subPath));
        view.repaint();
    }

    public boolean wasRecentlyEdited() {
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

    void checkInvariants() {
        if (ctrlIn.getAnchor() != this) {
            throw new IllegalStateException("ctrlIn problem in " + name);
        }
        if (ctrlIn.getSibling() != ctrlOut) {
            throw new IllegalStateException("ctrlIn problem in " + name);
        }
        if (ctrlOut.getAnchor() != this) {
            throw new IllegalStateException("ctrlOut problem in " + name);
        }
        if (ctrlOut.getSibling() != ctrlIn) {
            throw new IllegalStateException("ctrlOut problem in " + name);
        }
        if (ctrlIn == ctrlOut) {
            throw new IllegalStateException("same controls in " + name);
        }
    }

    private void showDebugDialog() {
        createDebugNode().showInDialog("Anchor Point " + name);
    }

    @Override
    public DebugNode createDebugNode() {
        DebugNode node = super.createDebugNode();

        node.addString("type", type.toString());
        node.addBoolean("recently edited", wasRecentlyEdited());

        node.add(ctrlIn.createDebugNode());
        node.add(ctrlOut.createDebugNode());

        return node;
    }
}

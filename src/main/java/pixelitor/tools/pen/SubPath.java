/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import com.bric.geom.ShapeUtils;
import pixelitor.AppContext;
import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.history.History;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.history.AddAnchorPointEdit;
import pixelitor.tools.pen.history.CloseSubPathEdit;
import pixelitor.tools.pen.history.FinishSubPathEdit;
import pixelitor.tools.pen.history.SubPathStartEdit;
import pixelitor.tools.transform.TransformBox;
import pixelitor.tools.transform.Transformable;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.debug.Ansi;
import pixelitor.utils.debug.DebugNode;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.io.PrintStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.ToDoubleFunction;

import static java.util.stream.Collectors.joining;
import static pixelitor.tools.pen.AnchorPointType.SMOOTH;
import static pixelitor.tools.pen.BuildState.*;

/**
 * A subpath within a {@link Path}.
 *
 * It is a composite Bézier curve: a series of Bézier curves
 * joined end to end where the last point of one curve
 * coincides with the starting point of the next curve.
 *
 * https://en.wikipedia.org/wiki/Composite_B%C3%A9zier_curve
 */
public class SubPath implements Serializable, Transformable {
    @Serial
    private static final long serialVersionUID = 1L;

    private static long nextId = 0;
    private final String id; // for debugging

    private List<AnchorPoint> anchorPoints = new ArrayList<>();
    private MovingPoint moving;

    private final Path path;
    private final Composition comp;

    private boolean closed = false;
    private boolean finished = false;

    // caching the transform box ensures that its
    // image-space coordinates are correct after undo
    private transient TransformBox box;

    public SubPath(Path path, Composition comp) {
        assert path != null;
        assert comp != null;
        this.path = path;
        this.comp = comp;
        id = "SP" + nextId++;
    }

    public SubPath deepCopy(Path copyParent, Composition newComp) {
        SubPath copy = new SubPath(copyParent, newComp);
        copy.closed = closed;
        copy.finished = finished;

        for (AnchorPoint point : anchorPoints) {
            var anchorCopy = new AnchorPoint(point, copy, true);
            copy.anchorPoints.add(anchorCopy);
        }

        return copy;
    }

    public void addFirstPoint(AnchorPoint p, boolean addToHistory) {
        anchorPoints.add(p);

        if (addToHistory) {
            History.add(new SubPathStartEdit(comp, path, this));
        }
    }

    // not used in the builder, only when converting from external shape
    public void addPoint(AnchorPoint p) {
        anchorPoints.add(p);
    }

    @VisibleForTesting
    public void addPoint(double x, double y) {
        addPoint(new AnchorPoint(x, y, comp.getView(), this));
    }

    public void setMovingPoint(MovingPoint p) {
        if (finished && p != null && AppContext.isDevelopment()) {
            throw new IllegalStateException();
        }
        moving = p;
    }

    public MovingPoint getMovingPoint() {
        return moving;
    }

    public void setMovingPointLocation(double x, double y, boolean nullOK) {
        if (moving != null) {
            moving.setLocation(x, y);
        } else {
            if (!nullOK) {
                throw new IllegalStateException("no moving point in " + path);
            }
        }
    }

    public AnchorPoint addMovingPointAsAnchor() {
        AnchorPoint ap = moving.toAnchor();
        anchorPoints.add(ap);
        setMovingPoint(null);

        AnchorPoint last = getLast();
        History.add(new AddAnchorPointEdit(comp, this, last));
        return last;
    }

    public AnchorPoint getFirst() {
        return anchorPoints.get(0);
    }

    public AnchorPoint getLast() {
        return anchorPoints.get(anchorPoints.size() - 1);
    }

    public int getNumAnchors() {
        return anchorPoints.size();
    }

    public void addToComponentSpaceShape(GeneralPath path) {
        addToShape(path, p -> p.x, p1 -> p1.y);
    }

    public Shape toComponentSpaceShape() {
        GeneralPath gp = new GeneralPath();
        addToComponentSpaceShape(gp);
        return gp;
    }

    public void addToImageSpaceShape(GeneralPath path) {
        addToShape(path, p -> p.imX, p1 -> p1.imY);
    }

    private void addToShape(GeneralPath gp,
                            ToDoubleFunction<DraggablePoint> toX,
                            ToDoubleFunction<DraggablePoint> toY) {

        if (anchorPoints.isEmpty()) {
            return;
        }

        // moveTo is the beginning of a new subpath
        AnchorPoint first = getFirst();
        gp.moveTo(toX.applyAsDouble(first), toY.applyAsDouble(first));
        AnchorPoint prev = first;

        for (int i = 1; i < anchorPoints.size(); i++) {
            AnchorPoint curr = anchorPoints.get(i);
            ControlPoint prevCtrlOut = prev.ctrlOut;
            ControlPoint currCtrlIn = curr.ctrlIn;
            if (prevCtrlOut.isRetracted() && currCtrlIn.isRetracted()) {
                gp.lineTo(
                    toX.applyAsDouble(curr),
                    toY.applyAsDouble(curr));
            } else {
                gp.curveTo(
                    toX.applyAsDouble(prevCtrlOut),
                    toY.applyAsDouble(prevCtrlOut),
                    toX.applyAsDouble(currCtrlIn),
                    toY.applyAsDouble(currCtrlIn),
                    toX.applyAsDouble(curr),
                    toY.applyAsDouble(curr));
            }
            prev = curr;
        }

        AnchorPoint last = getLast();
        if (moving != null && path.getBuildState() == MOVING_TO_NEXT_ANCHOR && Tools.PEN.showRubberBand()) {
            double movingX = toX.applyAsDouble(moving);
            double movingY = toY.applyAsDouble(moving);
            gp.curveTo(
                toX.applyAsDouble(last.ctrlOut),
                toY.applyAsDouble(last.ctrlOut),
                movingX, // the "ctrl in" of the moving is "retracted"
                movingY, // the "ctrl in" of the moving is "retracted"
                movingX,
                movingY);
        }

        if (closed) {
            ControlPoint lastCtrlOut = last.ctrlOut;
            ControlPoint firstCtrlIn = first.ctrlIn;
            if (lastCtrlOut.isRetracted() && firstCtrlIn.isRetracted()) {
                gp.lineTo(
                    toX.applyAsDouble(first),
                    toY.applyAsDouble(first));
            } else {
                gp.curveTo(
                    toX.applyAsDouble(lastCtrlOut),
                    toY.applyAsDouble(lastCtrlOut),
                    toX.applyAsDouble(firstCtrlIn),
                    toY.applyAsDouble(firstCtrlIn),
                    toX.applyAsDouble(first),
                    toY.applyAsDouble(first));
            }
            // We reached the first point again,
            // however call this to add a clean SEG_CLOSE.
            gp.closePath();
        }
    }

    public void paintHandlesForBuilding(Graphics2D g, BuildState state) {
        assert checkConsistency();

        // paint first all anchor points, without the handles
        int numPoints = anchorPoints.size();
        for (int i = 0; i < numPoints; i++) {
            AnchorPoint point = anchorPoints.get(i);
            if (point.isRecentlyEdited()) {
                // except when any of the anchor or its controls were recently edited
                point.paintHandles(g, true, true);

                // also paint the "out" handle of the previous point...
                if (i > 0) {
                    AnchorPoint prev = anchorPoints.get(i - 1);
                    prev.paintHandles(g, false, true);
                } else if (closed) {
                    AnchorPoint prev = anchorPoints.get(numPoints - 1);
                    prev.paintHandles(g, false, true);
                }

                // ...and the "in" handle of the next point
                if (i < numPoints - 1) {
                    AnchorPoint next = anchorPoints.get(i + 1);
                    next.paintHandles(g, true, false);
                } else if (closed) {
                    AnchorPoint next = anchorPoints.get(0);
                    next.paintHandles(g, true, false);
                }
            } else {
                point.paintHandle(g);
            }
        }

        if (finished) {
            return;
        }

        // paint some extra handles if not finished
        if (state == DRAGGING_THE_CONTROL_OF_LAST || state == MOVING_TO_NEXT_ANCHOR) {
            getLast().paintHandles(g, true, true);

            if (numPoints >= 2) {
                AnchorPoint lastButOne = anchorPoints.get(numPoints - 2);
                lastButOne.paintHandles(g, false, true);
            }
        }
    }

    public void paintHandlesForEditing(Graphics2D g) {
        assert checkConsistency();

        for (AnchorPoint point : anchorPoints) {
            point.paintHandles(g, true, true);
        }
    }

    public DraggablePoint handleWasHit(double x, double y, boolean altDown) {
        for (AnchorPoint anchor : anchorPoints) {
            DraggablePoint hit = anchor.handleOrCtrlHandleWasHit(x, y, altDown);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    /**
     * This is a workaround for a JDK bug: when a path is created from shapes,
     * often two almost-overlapping anchors are created instead of one.
     */
    public void mergeOverlappingAnchors() {
        List<AnchorPoint> newPoints = new ArrayList<>();
        int numPoints = anchorPoints.size();
        int index = 0;
        boolean mergedWithFirst = false;
        while (index < numPoints) {
            AnchorPoint current = anchorPoints.get(index);
            AnchorPoint next;
            boolean comparingWithFirst = false;
            if (index < numPoints - 1) {
                next = anchorPoints.get(index + 1);
            } else if (closed) {
                next = anchorPoints.get(0);
                comparingWithFirst = true;
            } else {
                // reached the last point, and it is not closed
                break;
            }
            if (tryMerging(current, next)) {
                if (comparingWithFirst) {
                    mergedWithFirst = true;
                }
                index++; // skip the next
            }
            newPoints.add(current);
            index++; // advance the while loop
        }
        if (mergedWithFirst) {
            // We could simply remove the first point, but for some unit
            // tests it is important that the first point remains first
            // even if the shape is closed.
            // Remove the last and set it as first
            AnchorPoint removedLast = newPoints.remove(newPoints.size() - 1);
            newPoints.set(0, removedLast);
        }
        anchorPoints = newPoints;
    }

    private static boolean tryMerging(AnchorPoint ap1, AnchorPoint ap2) {
        if (ap1.samePositionAs(ap2, 1.0)
            && ap1.ctrlOut.isRetracted()
            && ap2.ctrlIn.isRetracted()) {
            // the first will be kept, so copy the out ctrl of the second to the first
            ap1.ctrlOut.copyPositionFrom(ap2.ctrlOut);
            return true;
        }
        return false;
    }

    public void close(boolean addToHistory) {
        assert !closed;

        setMovingPoint(null);
        setClosed(true);

        // since a closing edit is added,
        // it is not necessary to also add a finish edit
        finish(comp, false);

        if (addToHistory) {
            History.add(new CloseSubPathEdit(comp, this));
        }

        // the controls of the first anchor should not be visible after
        // closing just because the anchor is activated during closing
        AnchorPoint.recentlyEditedPoint = null;
    }

    private void setClosed(boolean closed) {
        this.closed = closed;
    }

    private void setFinished(boolean finished) {
        this.finished = finished;
        if (finished) {
            setMovingPoint(null);
        }
    }

    public void undoClosing() {
        setClosed(false);
        setFinished(false);
        setMovingPoint(PenToolMode.BUILD.createMovingPoint(this));
    }

    public void undoFinishing() {
        assert finished : "was not finished";
        setFinished(false);
        path.setBuildingInProgressState();
    }

    public boolean isClosed() {
        return closed;
    }

    public void coCoordsChanged(View view) {
        for (AnchorPoint point : anchorPoints) {
            point.restoreCoordsFromImSpace(view);
            point.ctrlIn.restoreCoordsFromImSpace(view);
            point.ctrlOut.restoreCoordsFromImSpace(view);
        }
    }

    public void imCoordsChanged(AffineTransform at) {
        for (AnchorPoint point : anchorPoints) {
            point.imTransform(at, false);
        }
    }

    /**
     * Checks whether all the objects are wired together correctly
     */
    @SuppressWarnings("SameReturnValue")
    private void checkWiring() {
        int numPoints = anchorPoints.size();
        for (int i = 0; i < numPoints; i++) {
            AnchorPoint point = anchorPoints.get(i);
            ControlPoint ctrlIn = point.ctrlIn;
            ControlPoint ctrlOut = point.ctrlOut;
            if (point.getSubPath() != this) {
                throw new IllegalStateException("wrong subpath in point " + i
                    + ": anchor subpath is " + point.getSubPath()
                    + ", this is " + this);
            }
            if (ctrlIn.getAnchor() != point) {
                throw new IllegalStateException("ctrlIn problem in point " + i);
            }
            if (ctrlIn.getSibling() != ctrlOut) {
                throw new IllegalStateException("ctrlIn problem in point " + i);
            }
            if (ctrlOut.getAnchor() != point) {
                throw new IllegalStateException("ctrlOut problem in point " + i);
            }
            if (ctrlOut.getSibling() != ctrlIn) {
                throw new IllegalStateException("ctrlOut problem in point " + i);
            }
            if (ctrlIn == ctrlOut) {
                throw new IllegalStateException("same controls in point " + i);
            }
        }
    }

    @SuppressWarnings("SameReturnValue")
    public boolean checkConsistency() {
        checkWiring();

        if (closed && !finished) {
            throw new IllegalStateException(
                "subpath " + this + " is closed but not finished");
        }
        if (finished && moving != null) {
            throw new IllegalStateException(
                "subpath " + this + " is finished, but moving");
        }
        return true;
    }

    public void dump() {
        int numPoints = anchorPoints.size();
        PrintStream out = System.out;
        if (numPoints == 0) {
            out.println("Empty path");
        }
        for (int i = 0; i < numPoints; i++) {
            AnchorPoint point = anchorPoints.get(i);
            out.print(Ansi.purple("Point " + i + " (" + point.getName() + "): "));
            if (point == getFirst()) {
                out.print("first ");
            }
            if (point == getLast()) {
                out.print("last ");
            }
            point.dump();
        }
        out.println(Ansi.purple("Moving: ") + moving);
    }

    public AnchorPoint getAnchor(int index) {
        return anchorPoints.get(index);
    }

    public void replacePoint(AnchorPoint before, AnchorPoint after) {
        boolean replaced = false;
        for (int i = 0; i < anchorPoints.size(); i++) {
            AnchorPoint point = anchorPoints.get(i);
            if (point == before) { // has to be reference equality
                anchorPoints.set(i, after);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            throw new IllegalStateException(
                "point " + before + " not found in " + this);
        }
    }

    public void setHeuristicTypes() {
        for (AnchorPoint point : anchorPoints) {
            point.setHeuristicType();
        }
    }

    public void deletePoint(AnchorPoint ap) {
        // don't use List.remove, because it uses equals
        // and not reference equality
        int index = -1;
        for (int i = 0; i < anchorPoints.size(); i++) {
            if (anchorPoints.get(i) == ap) {
                index = i;
                break;
            }
        }

        anchorPoints.remove(index);
    }

    public void deleteLast() {
        int indexOfLast = anchorPoints.size() - 1;
        anchorPoints.remove(indexOfLast);
    }

    public void setView(View view) {
        for (AnchorPoint ap : anchorPoints) {
            ap.setView(view);
        }
        if (moving != null) {
            moving.setView(view);
        }
    }

    public boolean hasMovingPoint() {
        return moving != null;
    }

    public boolean isFinished() {
        return finished;
    }

    /**
     * Return whether this subpath is the only subpath in the parent path
     */
    public boolean isSingle() {
        return path.getNumSubpaths() == 1;
    }

    public void delete() {
        path.delete(this);
    }

    public void deletePath() {
        path.delete();
    }

    public Composition getComp() {
        return comp;
    }

    public Path getPath() {
        return path;
    }

    private boolean shouldBeClosed(double x, double y) {
        return getFirst().handleContains(x, y) && getNumAnchors() >= 2;
    }

    // return true if it could be closed
    boolean tryClosing(double x, double y) {
        if (shouldBeClosed(x, y)) {
            getFirst().setActive(false);
            close(true);
            return true;
        }
        return false;
    }

    void finishByCtrlClick(Composition comp) {
        finish(comp, true);
    }

    // A subpath can be finished either by closing it or by ctrl-clicking.
    // Either way, we end up in this method.
    public void finish(Composition comp, boolean addToHistory) {
        if (comp != this.comp) {
            // shouldn't happen, but it did happen somehow
            // (only in Mac random gui tests)
            return;
        }
        assert !finished;

        setFinished(true);
        path.setBuildState(NO_INTERACTION);
        comp.setActivePath(path);
        Tools.PEN.enableActions(true);

        if (addToHistory) {
            History.add(new FinishSubPathEdit(comp, this));
        }
    }

    public void addLine(double newX, double newY, View view) {
        newX = view.imageXToComponentSpace(newX);
        newY = view.imageYToComponentSpace(newY);
        AnchorPoint ap = new AnchorPoint(newX, newY, view, this);
        addPoint(ap);
    }

    public void addCubicCurve(double c1x, double c1y,
                              double c2x, double c2y,
                              double newX, double newY, View view) {
        ControlPoint lastOut = getLast().ctrlOut;
        c1x = view.imageXToComponentSpace(c1x);
        c1y = view.imageYToComponentSpace(c1y);
        lastOut.setLocationOnlyForThis(c1x, c1y);
        lastOut.afterMovingActionsForThis();

        newX = view.imageXToComponentSpace(newX);
        newY = view.imageYToComponentSpace(newY);
        AnchorPoint next = new AnchorPoint(newX, newY, view, this);
        addPoint(next);
        next.setType(SMOOTH);

        c2x = view.imageXToComponentSpace(c2x);
        c2y = view.imageYToComponentSpace(c2y);
        ControlPoint nextIn = next.ctrlIn;
        nextIn.setLocationOnlyForThis(c2x, c2y);
        nextIn.afterMovingActionsForThis();
    }

    public void addQuadCurve(double cx, double cy,
                             double newX, double newY, View view) {
        cx = view.imageXToComponentSpace(cx);
        cy = view.imageYToComponentSpace(cy);
        newX = view.imageXToComponentSpace(newX);
        newY = view.imageYToComponentSpace(newY);
        AnchorPoint last = getLast();

        // convert the quadratic bezier (with one control point)
        // into a cubic one (with two control points), see
        // https://stackoverflow.com/questions/3162645/convert-a-quadratic-bezier-to-a-cubic
        double qp1x = cx;
        double qp1y = cy;
        double qp0x = last.x;
        double qp0y = last.y;
        double qp2x = newX;
        double qp2y = newY;

        double twoThirds = 2.0 / 3.0;
        double cp1x = qp0x + twoThirds * (qp1x - qp0x);
        double cp1y = qp0y + twoThirds * (qp1y - qp0y);
        double cp2x = qp2x + twoThirds * (qp1x - qp2x);
        double cp2y = qp2y + twoThirds * (qp1y - qp2y);

        ControlPoint lastOut = last.ctrlOut;
        lastOut.setLocationOnlyForThis(cp1x, cp1y);
        lastOut.afterMovingActionsForThis();

        AnchorPoint next = new AnchorPoint(newX, newY, view, this);
        addPoint(next);
        next.setType(SMOOTH);

        ControlPoint nextIn = next.ctrlIn;
        nextIn.setLocationOnlyForThis(cp2x, cp2y);
        nextIn.afterMovingActionsForThis();
    }

    public TransformBox createTransformBox() {
        if (box != null) {
            return box;
        }
        if (isEmpty()) {
            return null;
        }
        saveImTransformRefPoints();

        Shape coShape = toComponentSpaceShape();

        assert ShapeUtils.isValid(coShape) : "invalid shape for " + toDetailedString();

        Rectangle2D coBoundingBox = coShape.getBounds2D();
        if (coBoundingBox.isEmpty()) {
            // it can still be empty if all x or y coordinates are the same
            return null;
        }

        box = new TransformBox(coBoundingBox, comp.getView(), this, true);
        return box;
    }

    public boolean isEmpty() {
        return getNumAnchors() < 2;
    }

    @Override
    public void imTransform(AffineTransform at) {
        for (AnchorPoint point : anchorPoints) {
            point.imTransform(at, true);
        }
    }

    @Override
    public void updateUI(View view) {
        assert view.getComp() == comp :
            "subpath comp = " + comp.getDebugName() +
                ", view comp = " + view.getComp().getDebugName();
        comp.repaint();
    }

    public void saveImTransformRefPoints() {
        for (AnchorPoint point : anchorPoints) {
            point.saveImTransformRefPoint();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SubPath subPath = (SubPath) o;
        return Objects.equals(id, subPath.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public boolean isActive() {
        return path.getActiveSubpath() == this;
    }

    @Override
    public DebugNode createDebugNode() {
        String name = isActive() ? "active " : "";
        name += "subpath " + getId();

        var node = new DebugNode(name, this);

        node.addString("comp debug name", comp.getDebugName());
        node.addString("name", getId());
        node.addBoolean("closed", isClosed());
        node.addBoolean("finished", isFinished());
        node.addBoolean("has moving point", hasMovingPoint());

        for (AnchorPoint point : anchorPoints) {
            node.add(point.createDebugNode());
        }

        return node;
    }

    @VisibleForTesting
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id + anchorPoints.stream()
            .map(AnchorPoint::getName)
            .collect(joining(",", " [", "]"));
    }

    // like toString(), but also includes the anchor point positions
    public String toDetailedString() {
        return id + anchorPoints.stream()
            .map(AnchorPoint::toString)
            .collect(joining(",", " [", "]"));
    }
}

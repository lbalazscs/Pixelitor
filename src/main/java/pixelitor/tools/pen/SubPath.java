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

package pixelitor.tools.pen;

import com.bric.geom.ShapeUtils;
import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.history.History;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.history.*;
import pixelitor.tools.transform.TransformBox;
import pixelitor.tools.transform.Transformable;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.debug.DebugNode;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.function.ToDoubleFunction;

import static java.util.stream.Collectors.joining;
import static pixelitor.tools.pen.BuildState.DRAGGING_LAST_CONTROL;
import static pixelitor.tools.pen.BuildState.IDLE;
import static pixelitor.tools.pen.BuildState.MOVING_TO_NEXT_ANCHOR;

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

    // unique identifier for debugging and equality comparisons
    private final String id;
    private static long nextId = 0;

    private List<AnchorPoint> anchorPoints = new ArrayList<>();

    // temporary point used during path construction
    private transient MovingPoint moving;

    private final Path path;
    private final Composition comp;

    // true if last anchor connects back to first anchor
    private boolean closed = false;

    // true if path construction is complete (no more points can be added)
    private boolean finished = false;

    public SubPath(Path path, Composition comp) {
        assert path != null;
        this.path = path;
        this.comp = comp;
        id = "SP" + nextId++;
    }

    public SubPath deepCopy(Path newParent, Composition newComp) {
        SubPath copy = new SubPath(newParent, newComp);
        copy.closed = closed;
        copy.finished = finished;

        for (AnchorPoint anchor : anchorPoints) {
            var anchorCopy = new AnchorPoint(anchor, copy, true);
            copy.anchorPoints.add(anchorCopy);
        }
        // we can have a moving point here, but it's not copied

        return copy;
    }

    public void addStartingAnchor(AnchorPoint p, boolean addToHistory) {
        anchorPoints.add(p);

        if (addToHistory) {
            History.add(new SubPathStartEdit(comp, path, this));
        }
    }

    // not used in the builder, only when converting from external shape
    public void addPoint(AnchorPoint p) {
        anchorPoints.add(p);
    }

    public void addPoint(double x, double y) {
        addPoint(new AnchorPoint(x, y, comp.getView(), this));
    }

    public void setMovingPoint(MovingPoint p) {
        if (finished && p != null && AppMode.isDevelopment()) {
            throw new IllegalStateException();
        }
        moving = p;
    }

    public MovingPoint getMovingPoint() {
        return moving;
    }

    public void moveMovingPointTo(double x, double y, boolean nullOK) {
        if (moving != null) {
            moving.setLocation(x, y);
        } else {
            if (!nullOK) {
                throw new IllegalStateException("no moving point in " + path);
            }
        }
    }

    public AnchorPoint convertMovingPointToAnchor() {
        AnchorPoint newAnchor = moving.toAnchor();
        anchorPoints.add(newAnchor);
        setMovingPoint(null);

        assert newAnchor == getLastAnchor();

        History.add(new AddAnchorPointEdit(comp, this, newAnchor));
        return newAnchor;
    }

    public AnchorPoint getFirstAnchor() {
        return anchorPoints.getFirst();
    }

    public AnchorPoint getLastAnchor() {
        return anchorPoints.getLast();
    }

    public int getNumAnchors() {
        return anchorPoints.size();
    }

    public Shape toComponentSpaceShape() {
        GeneralPath gp = new GeneralPath();
        addToComponentSpaceShape(gp);
        return gp;
    }

    public void addToComponentSpaceShape(GeneralPath path) {
        addToShape(path, p -> p.x, p1 -> p1.y);
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

        AnchorPoint first = getFirstAnchor();
        // moveTo marks the beginning of a new subpath in GeneralPath
        gp.moveTo(toX.applyAsDouble(first), toY.applyAsDouble(first));
        AnchorPoint prev = first;

        // add each curve segment
        for (int i = 1; i < anchorPoints.size(); i++) {
            AnchorPoint curr = anchorPoints.get(i);
            ControlPoint prevCtrlOut = prev.ctrlOut;
            ControlPoint currCtrlIn = curr.ctrlIn;

            if (prevCtrlOut.isRetracted() && currCtrlIn.isRetracted()) {
                // both control points are retracted - use a straight line
                gp.lineTo(
                    toX.applyAsDouble(curr),
                    toY.applyAsDouble(curr));
            } else {
                // use a cubic Bézier curve with both control points
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

        AnchorPoint last = getLastAnchor();

        // handle the "rubber band "preview of the next point during path construction
        if (moving != null && Tools.PEN.getBuildState() == MOVING_TO_NEXT_ANCHOR && Tools.PEN.showPathPreview()) {
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

        // close the path if needed
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

    /**
     * Renders the path handles in the Pen Tool (building mode).
     */
    public void paintHandlesForBuilding(Graphics2D g, BuildState state) {
        assert checkInvariants();

        // paints all anchor points, showing the handles
        // only for recently edited points and their neighbors
        int numAnchors = anchorPoints.size();
        for (int i = 0; i < numAnchors; i++) {
            AnchorPoint anchor = anchorPoints.get(i);
            if (anchor.wasRecentlyEdited()) {
                paintRecentAnchor(g, anchor, i, numAnchors);
            } else {
                anchor.paintHandle(g);
            }
        }

        if (finished) {
            return;
        }

        // paint some extra handles if not finished
        if (state == DRAGGING_LAST_CONTROL || state == MOVING_TO_NEXT_ANCHOR) {
            getLastAnchor().paintHandles(g, true, true);

            if (numAnchors >= 2) {
                AnchorPoint lastButOne = anchorPoints.get(numAnchors - 2);
                lastButOne.paintHandles(g, false, true);
            }
        }
    }

    private void paintRecentAnchor(Graphics2D g, AnchorPoint anchor, int i, int numPoints) {
        // paint the handles when any of the anchor or
        // its controls were recently edited
        anchor.paintHandles(g, true, true);

        // also paint the "out" handle of the previous anchor...
        if (i > 0) {
            AnchorPoint prev = anchorPoints.get(i - 1);
            prev.paintHandles(g, false, true);
        } else if (closed) {
            AnchorPoint prev = anchorPoints.get(numPoints - 1);
            prev.paintHandles(g, false, true);
        }

        // ...and the "in" handle of the next anchor
        if (i < numPoints - 1) {
            AnchorPoint next = anchorPoints.get(i + 1);
            next.paintHandles(g, true, false);
        } else if (closed) {
            AnchorPoint next = anchorPoints.getFirst();
            next.paintHandles(g, true, false);
        }
    }

    /**
     * Renders the path handles in the Node Tool (editing mode).
     * Shows all control points for all anchors.
     */
    public void paintHandlesForEditing(Graphics2D g) {
        assert checkInvariants();

        for (AnchorPoint anchor : anchorPoints) {
            anchor.paintHandles(g, true, true);
        }
    }

    /**
     * Checks if the given mouse coordinates intersect with any handle.
     */
    public DraggablePoint findHandleAt(double x, double y, boolean altDown) {
        for (AnchorPoint anchor : anchorPoints) {
            DraggablePoint handle = anchor.findHandleAt(x, y, altDown);
            if (handle != null) {
                return handle;
            }
        }
        return null;
    }

    /**
     * Merges anchor points that are very close to each other.
     * This is a workaround for a JDK bug: when a path is created from shapes,
     * often two almost-overlapping anchors are created instead of one.
     * It also handles the case when a path is created from a closed
     * shape, and the first and last anchors are actually identical.
     */
    public void mergeCloseAnchors() {
        List<AnchorPoint> mergedPoints = new ArrayList<>();
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
                next = anchorPoints.getFirst();
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
            mergedPoints.add(current);
            index++; // advance the while loop
        }
        if (mergedWithFirst) {
            // We could simply remove the first point, but for some unit
            // tests it is important that the first point remains first
            // even if the shape is closed.
            // Remove the last and set it as first.
            AnchorPoint removedLast = mergedPoints.removeLast();
            mergedPoints.set(0, removedLast);
        }
        anchorPoints = mergedPoints;
    }

    private static boolean tryMerging(AnchorPoint ap1, AnchorPoint ap2) {
        if (ap1.hasSameImPosAs(ap2, 1.0)
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

    public boolean isClosed() {
        return closed;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
        if (finished) {
            setMovingPoint(null);
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public void undoClosing() {
        setClosed(false);
        setFinished(false);
        setMovingPoint(Tools.PEN.createMovingPoint(this));
    }

    public void undoFinishing() {
        assert finished : "was not finished";
        setFinished(false);
        Tools.PEN.setBuildingInProgressState();
    }

    public void coCoordsChanged(View view) {
        for (AnchorPoint anchor : anchorPoints) {
            anchor.restoreCoordsFromImSpace(view);
            anchor.ctrlIn.restoreCoordsFromImSpace(view);
            anchor.ctrlOut.restoreCoordsFromImSpace(view);
        }
    }

    public void imCoordsChanged(AffineTransform at) {
        for (AnchorPoint anchor : anchorPoints) {
            anchor.imTransform(at, false);
        }
    }

    @SuppressWarnings("SameReturnValue")
    public boolean checkInvariants() {
        int numPoints = anchorPoints.size();
        for (int i = 0; i < numPoints; i++) {
            AnchorPoint anchor = anchorPoints.get(i);
            if (anchor.getSubPath() != this) {
                throw new IllegalStateException("wrong subpath in anchor " + i
                    + ": anchor subpath is " + anchor.getSubPath()
                    + ", this is " + this);
            }
            anchor.checkInvariants();
        }

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

    public AnchorPoint getAnchor(int index) {
        return anchorPoints.get(index);
    }

    public void replaceAnchor(AnchorPoint before, AnchorPoint after) {
        boolean replaced = false;
        for (int i = 0; i < anchorPoints.size(); i++) {
            AnchorPoint anchor = anchorPoints.get(i);
            if (anchor == before) { // has to be reference equality
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
        for (AnchorPoint anchor : anchorPoints) {
            anchor.setHeuristicType();
        }
    }

    public void deleteAnchor(AnchorPoint ap) {
        // doesn't use List.remove, because we want to delete by ==, not equals
        int index = -1;
        for (int i = 0; i < anchorPoints.size(); i++) {
            if (anchorPoints.get(i) == ap) {
                index = i;
                break;
            }
        }
        anchorPoints.remove(index);

        comp.pathChanged();
    }

    public void deleteLastAnchor() {
        anchorPoints.removeLast();
    }

    public void setView(View view) {
        for (AnchorPoint anchor : anchorPoints) {
            anchor.setView(view);
        }
        if (moving != null) {
            moving.setView(view);
        }
    }

    public boolean hasMovingPoint() {
        return moving != null;
    }

    /**
     * Return whether this subpath is the only subpath in the parent path
     */
    public boolean isSingle() {
        return path.getNumSubPaths() == 1;
    }

    public void delete() {
        path.delete(this);
    }

    public void deletePath() {
        path.delete();
    }

    public void flipDirection(String editName) {
        Path backup = path.deepCopy(comp);

        anchorPoints = new ArrayList<>(anchorPoints);
        Collections.reverse(anchorPoints);
        for (AnchorPoint anchor : anchorPoints) {
            anchor.swapControlPositions();
        }

        comp.pathChanged();
        comp.repaint();

        History.add(new PathEdit(editName, comp, backup, path, null));
    }

    public Composition getComp() {
        return comp;
    }

    public Path getPath() {
        return path;
    }

    private boolean shouldBeClosed(double x, double y) {
        return getFirstAnchor().contains(x, y) && getNumAnchors() >= 2;
    }

    // return true if it could be closed
    boolean tryClosing(double x, double y) {
        if (shouldBeClosed(x, y)) {
            getFirstAnchor().setActive(false);
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
        Tools.PEN.setBuildState(IDLE);
        if (comp != null) {
            comp.setActivePath(path);
            PathActions.setActionsEnabled(true);
        }

        if (addToHistory) {
            History.add(new FinishSubPathEdit(comp, this));
        }
    }

    public void addLine(double newX, double newY, View view) {
        PPoint point = PPoint.lazyFromIm(newX, newY, view);
        AnchorPoint anchor = new AnchorPoint(point, view, this);
        addPoint(anchor);
    }

    public void addCubicCurve(double c1x, double c1y,
                              double c2x, double c2y,
                              double nextX, double nextY, View view) {
        ControlPoint lastOut = getLastAnchor().ctrlOut;
        PPoint c1 = PPoint.lazyFromIm(c1x, c1y, view);
        lastOut.setLocationOnlyForThis(c1);

        PPoint nextLoc = PPoint.lazyFromIm(nextX, nextY, view);
        AnchorPoint next = new AnchorPoint(nextLoc, view, this);
        addPoint(next);

        PPoint c2 = PPoint.lazyFromIm(c2x, c2y, view);
        next.ctrlIn.setLocationOnlyForThis(c2);
    }

    public void addQuadCurve(double cx, double cy,
                             double nextX, double nextY, View view) {
        AnchorPoint last = getLastAnchor();

        // convert the quadratic bezier (with one control point)
        // into a cubic one (with two control points), see
        // https://stackoverflow.com/questions/3162645/convert-a-quadratic-bezier-to-a-cubic
        double qp1x = cx;
        double qp1y = cy;
        double qp0x = last.x;
        double qp0y = last.y;
        double qp2x = nextX;
        double qp2y = nextY;

        double twoThirds = 2.0 / 3.0;
        double cp1x = qp0x + twoThirds * (qp1x - qp0x);
        double cp1y = qp0y + twoThirds * (qp1y - qp0y);
        PPoint cp1 = PPoint.lazyFromIm(cp1x, cp1y, view);

        double cp2x = qp2x + twoThirds * (qp1x - qp2x);
        double cp2y = qp2y + twoThirds * (qp1y - qp2y);
        PPoint cp2 = PPoint.lazyFromIm(cp2x, cp2y, view);

        last.ctrlOut.setLocationOnlyForThis(cp1);

        PPoint nextLoc = PPoint.lazyFromIm(nextX, nextY, view);
        AnchorPoint next = new AnchorPoint(nextLoc, view, this);
        addPoint(next);

        next.ctrlIn.setLocationOnlyForThis(cp2);
    }

    public TransformBox createTransformBox() {
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

        return new TransformBox(coBoundingBox, comp.getView(), this, true);
    }

    public boolean isEmpty() {
        return getNumAnchors() < 2;
    }

    @Override
    public void imTransform(AffineTransform at) {
        for (AnchorPoint anchor : anchorPoints) {
            anchor.imTransform(at, true);
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
        for (AnchorPoint anchor : anchorPoints) {
            anchor.saveImTransformRefPoint();
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

    public void showDebugDialog() {
        createDebugNode(id).showInDialog("Subpath " + id);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        // the index of the subpath inside the parent path is already in the key
        String name = isActive() ? "active " : "";
        name += key + " " + getId();

        var node = new DebugNode(name, this);

        node.addString("comp debug name", comp.getDebugName());
        node.addString("name", getId());
        node.addBoolean("closed", isClosed());
        node.addBoolean("finished", isFinished());
        node.addBoolean("has moving point", hasMovingPoint());

        for (AnchorPoint anchor : anchorPoints) {
            node.add(anchor.createDebugNode());
        }

        return node;
    }

    public String getId() {
        return id;
    }

    public void randomize(Random rng, double amount) {
        for (AnchorPoint anchorPoint : anchorPoints) {
            double dx = (rng.nextDouble() * 2 - 1) * amount;
            double dy = (rng.nextDouble() * 2 - 1) * amount;
            anchorPoint.imTranslate(dx, dy);
        }
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

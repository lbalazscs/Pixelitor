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

import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.history.HandleMovedEdit;
import pixelitor.history.History;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.history.PathEdit;
import pixelitor.tools.transform.TransformBox;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Shapes;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.Debuggable;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.util.stream.Collectors.joining;
import static pixelitor.tools.pen.AnchorPointType.SMOOTH;
import static pixelitor.tools.pen.BuildState.DRAGGING_LAST_CONTROL;
import static pixelitor.tools.pen.BuildState.IDLE;
import static pixelitor.tools.pen.BuildState.MOVE_EDITING_PREVIOUS;
import static pixelitor.tools.pen.BuildState.MOVING_TO_NEXT_ANCHOR;

/**
 * A path contains the same information as a {@link PathIterator},
 * but in a way that makes it possible to interactively build and edit it.
 *
 * Technically, it consists of a series of geometrically distinct
 * {@link SubPath} objects (typically only one).
 */
public class Path implements Serializable, Debuggable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<SubPath> subPaths = new ArrayList<>();
    private final Composition comp;

    private SubPath activeSubPath;

    private final String id; // unique identifier for debugging
    private static long nextId = 0;

    private transient BuildState buildState = IDLE;
    private transient BuildState prevBuildState = IDLE;

    public Path(Composition comp, boolean setAsActive) {
        this.comp = comp;
        if (setAsActive) {
            comp.setActivePath(this);
        }
        id = "P" + nextId++;
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        buildState = IDLE;
        prevBuildState = IDLE;
    }

    public Path deepCopy(Composition newComp) {
        assert buildState == IDLE : buildState.toString();
        assert prevBuildState == IDLE : prevBuildState.toString();

        Path copy = new Path(newComp, false);
        for (SubPath subPath : subPaths) {
            SubPath subPathCopy = subPath.deepCopy(copy, newComp);
            copy.subPaths.add(subPathCopy);

            if (subPath == activeSubPath) {
                copy.activeSubPath = subPathCopy;
            }
        }

        assert copy.checkInvariants();
        return copy;
    }

    /**
     * Renders the path in the Pen Tool (building mode).
     */
    public void paintForBuilding(Graphics2D g) {
        Shapes.drawVisibly(g, toComponentSpaceShape());

        for (SubPath subPath : subPaths) {
            subPath.paintHandlesForBuilding(g, buildState);
        }
    }

    /**
     * Renders the path in the Node Tool (editing mode).
     */
    public void paintForEditing(Graphics2D g) {
        Shapes.drawVisibly(g, toComponentSpaceShape());

        for (SubPath subPath : subPaths) {
            subPath.paintHandlesForEditing(g);
        }
    }

    /**
     * Renders the path in the transform tool.
     */
    public void paintForTransforming(Graphics2D g) {
        Shapes.drawVisibly(g, toComponentSpaceShape());
    }

    public DraggablePoint findHandleAt(double x, double y, boolean altDown) {
        for (SubPath subPath : subPaths) {
            DraggablePoint handle = subPath.findHandleAt(x, y, altDown);
            if (handle != null) {
                return handle;
            }
        }
        return null;
    }

    public Path2D toImageSpaceShape() {
        GeneralPath path = new GeneralPath();
        for (SubPath subPath : subPaths) {
            subPath.addToImageSpaceShape(path);
        }
        return path;
    }

    public Shape toComponentSpaceShape() {
        GeneralPath path = new GeneralPath();
        for (SubPath subPath : subPaths) {
            subPath.addToComponentSpaceShape(path);
        }
        return path;
    }

    /**
     * Returns true if there are no more subpaths left
     */
    public boolean deleteLastSubPath() {
        int lastIndex = subPaths.size() - 1;
        assert activeSubPath == subPaths.get(lastIndex);

        // this should be called only for undo
        assert activeSubPath.getNumAnchors() == 1;

        subPaths.remove(lastIndex);
        if (lastIndex == 0) { // the last subpath was deleted
            return true;
        }
        activeSubPath = subPaths.get(lastIndex - 1);
        return false;
    }

    public void addSubPath(SubPath subPath) {
        subPaths.add(subPath);
        activeSubPath = subPath;
    }

    public void mergeCloseAnchors() {
        for (SubPath subPath : subPaths) {
            subPath.mergeCloseAnchors();
        }
    }

    public void setHeuristicTypes() {
        for (SubPath subPath : subPaths) {
            subPath.setHeuristicTypes();
        }
    }

    public void coCoordsChanged(View view) {
        for (SubPath subPath : subPaths) {
            subPath.coCoordsChanged(view);
        }
    }

    public void imCoordsChanged(AffineTransform at) {
        for (SubPath subPath : subPaths) {
            subPath.imCoordsChanged(at);
        }
    }

    public SubPath startNewSubpath(double x, double y, View view) {
        SubPath subPath = startNewSubpath();
        AnchorPoint first = new AnchorPoint(PPoint.lazyFromIm(x, y, view), subPath);
        first.setType(SMOOTH);
        subPath.addStartingAnchor(first, false);
        return subPath;
    }

    public SubPath startNewSubpath() {
        activeSubPath = new SubPath(this, comp);
        subPaths.add(activeSubPath);
        return activeSubPath;
    }

    public void setView(View view) {
        for (SubPath subPath : subPaths) {
            subPath.setView(view);
        }
    }

    public void finishActiveSubpath(boolean addToHistory) {
        activeSubPath.finish(comp, addToHistory);
    }

    public void delete(SubPath subPath) {
        assert comp.getActivePath() == this;

        Path backup = deepCopy(comp);
        subPaths.removeIf(sp -> sp == subPath);
        assert !subPaths.isEmpty(); // should never be called for the last subpath
        activeSubPath = subPaths.getLast();
        comp.repaint();

        History.add(new PathEdit("Delete Subpath", comp, backup, this, null));

        assert comp.getActivePath() == this;
    }

    public void delete() {
        assert comp.getActivePath() == this;

        comp.setActivePath(null);

        assert Tools.activeIsPathTool();
        PathTool tool = (PathTool) Tools.getActive();

        // create the edit before the actual removing
        // so that it can remember the pen tool mode
        PathEdit edit = new PathEdit("Delete Path", comp, this, null, tool);

        tool.removePath();
        comp.pathChanged(true);
        comp.repaint();

        History.add(edit);
    }

    public void handleMoved(HandleMovedEdit edit) {
        comp.pathChanged();
        History.add(edit);
    }

    public void replaceSubPathAt(int index, SubPath subPath) {
        subPaths.set(index, subPath);
        activeSubPath = subPath;
    }

    public List<TransformBox> createTransformBoxes() {
        List<TransformBox> boxes = new ArrayList<>();
        for (SubPath subPath : subPaths) {
            TransformBox box = subPath.createTransformBox();
            if (box != null) {
                boxes.add(box);
            }
        }
        return boxes;
    }

    public void setBuildState(BuildState newState) {
        if (buildState == newState) {
            return;
        }

        prevBuildState = buildState;

        if (newState == MOVING_TO_NEXT_ANCHOR && !hasMovingPoint()) {
            MovingPoint mp = Tools.PEN.createMovingPoint(activeSubPath);
            activeSubPath.setMovingPoint(mp);
        }

        buildState = newState;

        if (newState == IDLE) {
            prevBuildState = IDLE;
        }
    }

    public BuildState getPrevBuildState() {
        return prevBuildState;
    }

    // called only by the undo/redo mechanism
    public void setBuildingInProgressState() {
        // this check would not be necessary if tool switching
        // and mode switching were recorded as events in the history
        if ((Tools.getActive() != Tools.PEN)) {
            return;
        }

        boolean mouseDown = Tools.EventDispatcher.isMouseDown();
        if (mouseDown) {
            setBuildState(DRAGGING_LAST_CONTROL);
        } else {
            setBuildState(MOVING_TO_NEXT_ANCHOR);
        }
    }

    void finishSubPathByCtrlClick(Composition comp) {
        BuildState state = getBuildState();
        assert state == MOVING_TO_NEXT_ANCHOR
            || state == MOVE_EDITING_PREVIOUS : "state = " + state;

        activeSubPath.finishByCtrlClick(comp);
    }

    public int indexOf(SubPath subPath) {
        return subPaths.indexOf(subPath);
    }

    public BuildState getBuildState() {
        return buildState;
    }

    // return true if it could be closed
    boolean tryClosing(double x, double y) {
        return activeSubPath.tryClosing(x, y);
    }

    public void assertStateIs(BuildState state) {
        if (buildState != state) {
            throw new AssertionError("Expected " + state + ", found " + buildState);
        }
    }

    public String getId() {
        return id;
    }

    public Rectangle getImBounds() {
        return toImageSpaceShape().getBounds();
    }

    public void randomize(Random rng, double amount) {
        for (SubPath subPath : subPaths) {
            subPath.randomize(rng, amount);
        }
    }

    public SubPath getActiveSubpath() {
        return activeSubPath;
    }

    public Composition getComp() {
        return comp;
    }

    public AnchorPoint getFirstAnchor() {
        return activeSubPath.getFirstAnchor();
    }

    public AnchorPoint getLastAnchor() {
        return activeSubPath.getLastAnchor();
    }

    public AnchorPoint convertMovingPointToAnchor() {
        return activeSubPath.convertMovingPointToAnchor();
    }

    public MovingPoint getMovingPoint() {
        return activeSubPath.getMovingPoint();
    }

    public void moveMovingPointTo(double x, double y, boolean nullOK) {
        activeSubPath.moveMovingPointTo(x, y, nullOK);
    }

    public boolean hasMovingPoint() {
        return activeSubPath.hasMovingPoint();
    }

    public int getNumSubPaths() {
        return subPaths.size();
    }

    public SubPath getSubPath(int index) {
        return subPaths.get(index);
    }

    /**
     * Checks whether all the objects are wired together correctly
     */
    @SuppressWarnings("SameReturnValue")
    public boolean checkInvariants() {
        for (SubPath subPath : subPaths) {
            if (subPath.getPath() != this) {
                throw new IllegalStateException("wrong parent in subpath " + subPath);
            }
            if (subPath.getComp() != comp) {
                throw new IllegalStateException("wrong comp in subpath " + subPath);
            }

            subPath.checkInvariants();
        }
        return true;
    }

    public void showDebugDialog() {
        createDebugNode(id).showInDialog("Path " + id);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        checkInvariants();
        var node = new DebugNode(key, this);

        int numSubpaths = getNumSubPaths();
        node.addInt("number of subpaths", numSubpaths);
        node.addAsString("build state", getBuildState());

        for (int i = 0; i < numSubpaths; i++) {
            var subPath = getSubPath(i);
            node.add(subPath.createDebugNode("subPath " + i));
        }

        return node;
    }

    @Override
    public String toString() {
        String s = getId() + " ";
        s += subPaths.stream()
            .map(SubPath::getId)
            .collect(joining(",", "[", "]"));
        return s;
    }

    // also includes the anchor point positions
    public String toDetailedString() {
        String s = getId() + " ";
        s += subPaths.stream()
            .map(SubPath::toDetailedString)
            .collect(joining(",", "[", "]"));
        return s;
    }
}

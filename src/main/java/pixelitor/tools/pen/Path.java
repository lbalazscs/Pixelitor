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

package pixelitor.tools.pen;

import pixelitor.Build;
import pixelitor.Composition;
import pixelitor.gui.CompositionView;
import pixelitor.gui.View;
import pixelitor.history.History;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.history.PathEdit;
import pixelitor.tools.transform.TransformBox;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Messages;
import pixelitor.utils.Shapes;
import pixelitor.utils.VisibleForTesting;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.joining;
import static pixelitor.tools.pen.AnchorPointType.SMOOTH;
import static pixelitor.tools.pen.BuildState.DRAGGING_THE_CONTROL_OF_LAST;
import static pixelitor.tools.pen.BuildState.MOVE_EDITING_PREVIOUS;
import static pixelitor.tools.pen.BuildState.MOVING_TO_NEXT_ANCHOR;

/**
 * A path contains the same information as a {@link PathIterator},
 * but in a way that makes it possible to interactively build and edit it.
 *
 * Technically, it consists of a series of geometrically distinct
 * {@link SubPath} objects (typically only one).
 */
public class Path implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final List<SubPath> subPaths = new ArrayList<>();
    private final Composition comp;
    private SubPath activeSubPath;

    private static long debugCounter = 0;
    private final String id; // for debugging
    private BuildState buildState;
    private BuildState prevBuildState;
    private transient PenToolMode preferredPenToolMode;

    public Path(Composition comp, boolean setAsActive) {
        this.comp = comp;
        if (setAsActive) {
            comp.setActivePath(this);
        }
        id = "P" + (debugCounter++);
        buildState = BuildState.NO_INTERACTION;
    }

    public Path copyForUndo() {
        Path copy = new Path(comp, false);
        for (SubPath sp : subPaths) {
            copy.subPaths.add(sp.copyForUndo(copy));
        }
        int activeIndex = subPaths.indexOf(activeSubPath);
        copy.activeSubPath = copy.subPaths.get(activeIndex);
        return copy;
    }

    public PenToolMode getPreferredPenToolMode() {
        return preferredPenToolMode;
    }

    public void setPreferredPenToolMode(PenToolMode preferredPenToolMode) {
        this.preferredPenToolMode = preferredPenToolMode;
    }

    @VisibleForTesting
    public SubPath getActiveSubpath() {
        return activeSubPath;
    }

    public Composition getComp() {
        return comp;
    }

    public void paintForBuilding(Graphics2D g) {
        Shapes.drawVisible(g, toComponentSpaceShape());

        for (SubPath sp : subPaths) {
            sp.paintHandlesForBuilding(g, buildState);
        }
    }

    public void paintForEditing(Graphics2D g) {
        Shapes.drawVisible(g, toComponentSpaceShape());

        for (SubPath sp : subPaths) {
            sp.paintHandlesForEditing(g);
        }
    }

    public void paintForTransforming(Graphics2D g) {
        Shapes.drawVisible(g, toComponentSpaceShape());
    }

    public DraggablePoint handleWasHit(double x, double y, boolean altDown) {
        for (SubPath sp : subPaths) {
            DraggablePoint hit = sp.handleWasHit(x, y, altDown);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    public Shape toImageSpaceShape() {
        GeneralPath path = new GeneralPath();
        for (SubPath sp : subPaths) {
            sp.addToImageSpaceShape(path);
        }
        return path;
    }

    public Shape toComponentSpaceShape() {
        GeneralPath path = new GeneralPath();
        for (SubPath sp : subPaths) {
            sp.addToComponentSpaceShape(path);
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
        if (lastIndex == 0) {
            return true;
        }
        activeSubPath = subPaths.get(lastIndex - 1);
        return false;
    }

    public void addSubPath(SubPath subPath) {
        subPaths.add(subPath);
        activeSubPath = subPath;
    }

    public AnchorPoint getFirst() {
        return activeSubPath.getFirst();
    }

    public AnchorPoint getLast() {
        return activeSubPath.getLast();
    }

    public AnchorPoint addMovingPointAsAnchor() {
        return activeSubPath.addMovingPointAsAnchor();
    }

    public MovingPoint getMoving() {
        return activeSubPath.getMoving();
    }

    public void setMovingLocation(double x, double y, boolean nullOK) {
        activeSubPath.setMovingLocation(x, y, nullOK);
    }

    public void dump() {
        checkConsistency();
        System.out.println("Path " + toString());
        for (SubPath sp : subPaths) {
            if (sp.isClosed()) {
                System.out.println("New closed subpath " + sp.getId());
            } else {
                System.out.println("New unclosed subpath" + sp.getId());
            }

            sp.dump();
        }
    }

    /**
     * Checks whether all the objects are wired together correctly
     */
    @SuppressWarnings("SameReturnValue")
    public boolean checkConsistency() {
        for (SubPath subPath : subPaths) {
            if (subPath.getPath() != this) {
                throw new IllegalStateException("wrong parent in subpath " + subPath);
            }
            if (subPath.getComp() != comp) {
                throw new IllegalStateException("wrong comp in subpath " + subPath);
            }

            subPath.checkConsistency();
        }
        return true;
    }

    public void mergeOverlappingAnchors() {
        for (SubPath sp : subPaths) {
            sp.mergeOverlappingAnchors();
        }
    }

    public void setHeuristicTypes() {
        for (SubPath sp : subPaths) {
            sp.setHeuristicTypes();
        }
    }

    public void coCoordsChanged(View view) {
        for (SubPath sp : subPaths) {
            sp.coCoordsChanged(view);
        }
    }

    public void imCoordsChanged(AffineTransform at) {
        for (SubPath subPath : subPaths) {
            subPath.imCoordsChanged(at);
        }
    }

    public SubPath startNewSubpath(double x, double y, CompositionView cv) {
        SubPath sp = startNewSubpath();
        AnchorPoint first = new AnchorPoint(PPoint.eagerFromIm(x, y, cv), sp);
        first.setType(SMOOTH);
        sp.addFirstPoint(first, false);
        return sp;
    }

    public SubPath startNewSubPath(AnchorPoint point, boolean addToHistory) {
        SubPath sp = startNewSubpath();
        sp.addFirstPoint(point, addToHistory);
        return sp;
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

    public void finishActiveSubpath(String reason) {
        activeSubPath.finish(comp, reason, true);
    }

    public boolean hasMovingPoint() {
        return activeSubPath.hasMovingPoint();
    }

    public int getNumSubpaths() {
        return subPaths.size();
    }

    public SubPath getSubPath(int index) {
        return subPaths.get(index);
    }

    public void delete(SubPath subPath) {
        assert comp.getActivePath() == this;

        Path backup = copyForUndo();
        subPaths.removeIf(sp -> sp == subPath);
        assert subPaths.size() >= 1; // should never be called for the last subpath
        activeSubPath = subPaths.get(subPaths.size() - 1);
        comp.repaint();

        PathEdit edit = new PathEdit("Delete Subpath", comp, backup, this);
        History.addEdit(edit);

        assert comp.getActivePath() == this;
    }

    public void delete() {
        assert comp.getActivePath() == this;

        comp.setActivePath(null);

        assert Tools.PEN.isActive();

        // create the edit before the actual removing
        // so that it can it can remember the pen tool mode
        PathEdit edit = new PathEdit("Delete Path", comp, this, null);

        Tools.PEN.removePath();
        comp.repaint();

        History.addEdit(edit);
    }

    public int indexOf(SubPath subPath) {
        return subPaths.indexOf(subPath);
    }

    public void changeSubPath(int index, SubPath subPath) {
        assert activeSubPath == subPaths.get(index);
        subPaths.set(index, subPath);
        activeSubPath = subPath;
    }

    public BuildState getBuildState() {
        return buildState;
    }

    public void setBuildState(BuildState newState, String reason) {
        if (this.buildState == newState) {
            return;
        }
        if (Build.isDevelopment()) {
            Messages.showInStatusBar("<html><font color=red>" + buildState
                    + "</font> \u21e8 <font color=#004E00>" + newState
                    + "</font> (" + reason + ")");
        }

        prevBuildState = buildState;

        if (newState == MOVING_TO_NEXT_ANCHOR) {
            if (!hasMovingPoint()) {
                MovingPoint m = PenToolMode.BUILD.createMovingAtLastMouseLoc(activeSubPath);
                activeSubPath.setMoving(m);
            }
        }

        this.buildState = newState;
    }

    public BuildState getPrevBuildState() {
        return prevBuildState;
    }

    // called only by the undo/redo mechanism
    public void setBuildingInProgressState(String reason) {
        boolean mouseDown = Tools.EventDispatcher.isMouseDown();
        if (mouseDown) {
            setBuildState(DRAGGING_THE_CONTROL_OF_LAST, reason);
        } else {
            setBuildState(MOVING_TO_NEXT_ANCHOR, reason);
        }
    }

    void finishByCtrlClick(Composition comp) {
        BuildState state = getBuildState();
        assert state == MOVING_TO_NEXT_ANCHOR
                || state == MOVE_EDITING_PREVIOUS : "state = " + state;

        activeSubPath.finishByCtrlClick(comp);
    }

    // return true if it could be closed
    boolean tryClosing(double x, double y, Composition comp) {
        return activeSubPath.tryClosing(x, y, comp);
    }

    @VisibleForTesting
    public void assertStateIs(BuildState state) {
        if (buildState != state) {
            throw new AssertionError("Expected " + state + ", found " + buildState);
        }
    }

    @VisibleForTesting
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        String s = getId() + " ";
        s += subPaths
                .stream()
                .map(SubPath::getId)
                .collect(joining(",", "[", "]"));
        return s;
    }

    // also includes the anchor point positions
    public String toDetailedString() {
        String s = getId() + " ";
        s += subPaths
                .stream()
                .map(SubPath::toDetailedString)
                .collect(joining(",", "[", "]"));
        return s;
    }

    public List<TransformBox> createTransformBoxes() {
        List<TransformBox> boxes = new ArrayList<>();
        for (SubPath subPath : subPaths) {
            boxes.add(subPath.createTransformBox());
        }
        return boxes;
    }
}

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
import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PMouseEvent;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

import static pixelitor.tools.pen.AnchorPointType.CUSP;
import static pixelitor.tools.pen.AnchorPointType.SYMMETRIC;
import static pixelitor.tools.pen.BuildState.DRAGGING_LAST_CONTROL;
import static pixelitor.tools.pen.BuildState.DRAG_EDITING_PREVIOUS;
import static pixelitor.tools.pen.BuildState.IDLE;
import static pixelitor.tools.pen.BuildState.MOVE_EDITING_PREVIOUS;
import static pixelitor.tools.pen.BuildState.MOVING_TO_NEXT_ANCHOR;
import static pixelitor.tools.pen.PenTool.hasPath;
import static pixelitor.tools.pen.PenTool.path;
import static pixelitor.tools.util.DraggablePoint.activePoint;
import static pixelitor.tools.util.DraggablePoint.lastActive;

/**
 * A pen tool interaction mode where a path can be built from scratch.
 * The path can also be edited, but only by using modifier keys.
 */
public final class PathBuilder implements PenToolMode {
    // The last mouse position. Important when the moving point
    // has to be restored after an undo
    private double lastX;
    private double lastY;

    public static final PathBuilder INSTANCE = new PathBuilder();

    private static final String BUILDER_HELP_MESSAGE =
        "Pen Tool Build Mode: " +
            "<b>click, drag</b> and repeat to create, " +
            "<b>Ctrl-click</b> or close it to finish it. " +
            "<b>Ctrl-drag</b> moves points, " +
            "<b>Alt-drag</b> breaks handles, " +
            "<b>Shift-drag</b> constrains angles.";

    private PathBuilder() {
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        if (path == null) {
            //always called on the EDT, therefore thread safety isn't a concern
            //noinspection NonThreadSafeLazyInitialization
            path = new Path(e.getComp(), true);
            path.setPreferredPenToolMode(this);
        }

        BuildState state = path.getBuildState();
        if (state == DRAGGING_LAST_CONTROL) {
            state = handleUnexpectedDragState("mousePressed", e.getView());
        }

        double x = e.getCoX();
        double y = e.getCoY();
        lastX = x;
        lastY = y;

        boolean altDown = e.isAltDown();
        boolean controlDown = e.isControlDown();

        if (state == IDLE) {
            if (controlDown) {
                if (handleCtrlPressBeforeSubpath(altDown, x, y)) {
                    return;
                }
            } else if (altDown) {
                if (handleAltPressBeforeSubpath(x, y)) {
                    return;
                }
            }

            // only add a point directly if previously we were
            // in the IDLE state. Normally points
            // are added by finalizing (in mousePressed)
            // the moving point (created in mouseReleased)
            SubPath sp = path.startNewSubpath();
            AnchorPoint ap = new AnchorPoint(e, sp);
            sp.addFirstPoint(ap, true);
            ap.ctrlOut.mousePressed(x, y);
        } else if (state.isMoving()) {
            if (state == MOVING_TO_NEXT_ANCHOR) {
                assert path.hasMovingPoint();
            }

            if (controlDown) {
                handleCtrlPressWhileMoving(e, altDown, x, y);
                return;
            }

            boolean altDownHitNothing = false;
            if (altDown) {
                DraggablePoint handle = path.findHandleAt(x, y, altDown);
                if (handle != null) {
                    if (handle instanceof ControlPoint cp) {
                        breakAndStartMoving(cp, x, y);
                        return;
                    } else if (handle instanceof AnchorPoint ap) {
                        startDraggingOutNewHandles(ap, x, y);
                        return;
                    }
                } else {
                    altDownHitNothing = true;
                }
            }

            if (path.tryClosing(x, y)) {
                return;
            }
            // if no special case was true, then create
            // a new anchor from the moving point
            finalizeMovingPoint(x, y, altDownHitNothing, e.isShiftDown());
        }

        path.setBuildState(DRAGGING_LAST_CONTROL);
        assert path.checkConsistency();
    }

    private static void finalizeMovingPoint(double x, double y, boolean altDownHitNothing, boolean shiftDown) {
        path.getMovingPoint().mouseReleased(x, y, shiftDown);
        AnchorPoint ap = path.addMovingPointAsAnchor();
        if (altDownHitNothing) {
            ap.setType(CUSP);
        }
        ap.ctrlOut.mousePressed(x, y);
    }

    private static boolean handleCtrlPressBeforeSubpath(boolean altDown,
                                                        double x, double y) {
        // if we are over an old point, just move it
        DraggablePoint handle = path.findHandleAt(x, y, altDown);
        if (handle != null) {
            startMovingPrevious(handle, x, y);
            return true;
        }
        return false;
    }

    private static boolean handleAltPressBeforeSubpath(double x, double y) {
        // if only alt is down, then break the control points
        DraggablePoint handle = path.findHandleAt(x, y, true);
        if (handle != null) {
            if (handle instanceof ControlPoint cp) {
                breakAndStartMoving(cp, x, y);
                return true;
            } else if (handle instanceof AnchorPoint ap) {
                startDraggingOutNewHandles(ap, x, y);
                return true;
            }
        }
        return false;
    }

    private static void handleCtrlPressWhileMoving(PMouseEvent e, boolean altDown,
                                                   double x, double y) {
        DraggablePoint handle = path.findHandleAt(x, y, altDown);
        if (handle != null) {
            startMovingPrevious(handle, x, y);
        } else {
            // control is down, but nothing was hit
            path.finishByCtrlClick(e.getComp());
        }
    }

    private static void startDraggingOutNewHandles(AnchorPoint ap, double x, double y) {
        ap.retractHandles();
        // drag the retracted handles out
        ap.setType(SYMMETRIC);
        startMovingPrevious(ap.ctrlOut, x, y);
    }

    private static void breakAndStartMoving(ControlPoint cp, double x, double y) {
        if (!cp.isRetracted()) {
            // alt-press on an anchor point should break the handle...
            cp.getAnchor().setType(CUSP);
        } else {
            // ...except when it's retracted: then drag out symmetrically
            cp.getAnchor().setType(SYMMETRIC);
        }
        // after breaking, move it as usual
        startMovingPrevious(cp, x, y);
    }

    private static void startMovingPrevious(DraggablePoint point, double x, double y) {
        point.setActive(true);
        point.mousePressed(x, y);
        path.setBuildState(DRAG_EDITING_PREVIOUS);
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        if (path == null) {
            return;
        }
        BuildState state = path.getBuildState();
        if (state == IDLE) {
            // normally it should not happen, but in some rare cases
            // a dragged event comes without a preceding pressed event
            return;
        }

        if (state.isMoving()) {
            state = handleUnexpectedMoveState("mouseDragged", e.getView(), state);
            if (state == IDLE) {
                return;
            }
        }

        double x = e.getCoX();
        double y = e.getCoY();
        lastX = x;
        lastY = y;

        if (state == DRAG_EDITING_PREVIOUS) {
            activePoint.mouseDragged(x, y, e.isShiftDown());
            path.setMovingPointLocation(x, y, true);
            return;
        }

        boolean breakHandle = e.isAltDown();
        AnchorPoint last = path.getLastAnchor();
        if (breakHandle) {
            last.setType(CUSP);
        } else {
            last.setType(SYMMETRIC);
        }
        last.ctrlOut.mouseDragged(x, y, e.isShiftDown());

        path.setBuildState(DRAGGING_LAST_CONTROL);
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
        if (path == null) {
            return;
        }
        BuildState state = path.getBuildState();
        if (state == IDLE) {
            return;
        }

        if (state.isMoving()) {
            state = handleUnexpectedMoveState("mouseReleased", e.getView(), state);
            if (state == IDLE) {
                return;
            }
        }
        assert state.isDragging() : "state = " + state;

        double x = e.getCoX();
        double y = e.getCoY();
        lastX = x;
        lastY = y;

        if (state == DRAG_EDITING_PREVIOUS) {
            activePoint.mouseReleased(x, y, e.isShiftDown());
            activePoint.createMovedEdit(e.getComp()).ifPresent(path::handleMoved);
            // after the dragging is finished, determine the next state
            if (path.getPrevBuildState() == IDLE) {
                path.setBuildState(IDLE);
            } else {
                if (e.isControlDown() || e.isAltDown()) {
                    path.setBuildState(MOVE_EDITING_PREVIOUS);
                } else {
                    path.setBuildState(MOVING_TO_NEXT_ANCHOR);
                }
            }

            return;
        } else if (state == DRAGGING_LAST_CONTROL) {
            // finalize the dragged out control of the last anchor
            AnchorPoint last = path.getLastAnchor();
            ControlPoint ctrlOut = last.ctrlOut;
            ctrlOut.mouseReleased(x, y, e.isShiftDown());
            if (!ctrlOut.isRetracted()) {
                last.changeTypeFromSymToSmooth();
            }

            // initialize the moving point for the next state
            SubPath sp = path.getActiveSubpath();
            MovingPoint moving = new MovingPoint(x, y, last, last.getView());
            moving.mousePressed(x, y);
            sp.setMovingPoint(moving);
        }

        if (e.isControlDown() || e.isAltDown()) {
            path.setBuildState(MOVE_EDITING_PREVIOUS);
        } else {
            path.setBuildState(MOVING_TO_NEXT_ANCHOR);
        }

        assert path.checkConsistency();
    }

    @Override
    public boolean mouseMoved(MouseEvent e, View view) {
        if (path == null) {
            return false;
        }
        BuildState state = path.getBuildState();
        if (state == DRAGGING_LAST_CONTROL) {
            state = handleUnexpectedDragState("mouseMoved", view);
        }

        int x = e.getX();
        int y = e.getY();
        lastX = x;
        lastY = y;

        boolean altDown = e.isAltDown();
        boolean controlDown = e.isControlDown();
        if (controlDown || altDown) {
            if (state != IDLE) {
                path.setBuildState(MOVE_EDITING_PREVIOUS);
            }

            DraggablePoint handle = path.findHandleAt(x, y, altDown);
            if (handle != null) {
                handle.setActive(true);
            } else {
                activePoint = null;
            }
        } else {
            if (state == IDLE) {
                return false;
            }

            path.setBuildState(MOVING_TO_NEXT_ANCHOR);

            // when the mouse is moved, the moving point is conceptually dragged
            path.getMovingPoint().mouseDragged(x, y, e.isShiftDown());

            AnchorPoint first = path.getFirstAnchor();
            if (first.handleContains(x, y)) {
                first.setActive(true);
            } else {
                activePoint = null;
            }
        }

        return true;
    }

    // Getting here shouldn't happen, but it did happen somehow
    // (only in Mac random gui tests)
    private static BuildState handleUnexpectedDragState(String where, View view) {
        if (AppMode.isDevelopment()) {
            System.out.printf("PathBuilder::recoverFromUnexpectedDragState: " +
                "where = '%s, active = %s'%n", where, view.isActive());
        }

        path.setBuildState(MOVING_TO_NEXT_ANCHOR);
        return path.getBuildState();
    }

    // Getting here shouldn't happen, but it did happen somehow
    // (only in Mac random gui tests)
    private static BuildState handleUnexpectedMoveState(String where, View view, BuildState state) {
        if (AppMode.isDevelopment()) {
            System.out.printf("PathBuilder::recoverFromUnexpectedMoveState: " +
                "where = '%s, active = %s'%n", where, view.isActive());
        }

        BuildState dragState = IDLE;
        if (state == MOVE_EDITING_PREVIOUS) {
            dragState = DRAG_EDITING_PREVIOUS;
        }
        path.setBuildState(dragState);
        return path.getBuildState();
    }

    @Override
    public void paint(Graphics2D g) {
        if (hasPath()) {
            path.paintForBuilding(g);
        }
    }

    @Override
    public void coCoordsChanged(View view) {
        // do nothing
    }

    @Override
    public void imCoordsChanged(AffineTransform at, View view) {
        // do nothing
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key, View view) {
        if (path == null) {
            // there's nothing to nudge
            return false;
        }

        // an active point is always checked first
        if (activePoint != null) {
            activePoint.arrowKeyPressed(key);
            return true;
        }

        BuildState state = path.getBuildState();
        if (state == MOVING_TO_NEXT_ANCHOR || state == DRAGGING_LAST_CONTROL) {
            // If a new subpath is being created,
            // move the most recently placed anchor point
            AnchorPoint last = path.getLastAnchor();
            if (last != null) {
                last.arrowKeyPressed(key);
                return true;
            }
        } else {
            // move the last active point
            if (lastActive != null) {
                lastActive.arrowKeyPressed(key);
                return true;
            }
        }

        return false;
    }

    @Override
    public String getToolMessage() {
        return BUILDER_HELP_MESSAGE;
    }

    @Override
    public boolean requiresExistingPath() {
        return false;
    }

    @Override
    public void modeDeactivated(Composition comp) {
        if (hasPath() && !path.getActiveSubpath().isFinished()) {
            // Adding something to the history at this point is risky,
            // because this code could be called while undoing something.
            path.finishActiveSubpath(false);
        } else {
            assertStateIs(IDLE);
        }
        PenToolMode.super.modeDeactivated(comp);
    }

    private static void assertStateIs(BuildState s) {
        if (path == null) {
            if (s != IDLE) {
                throw new IllegalStateException("null path");
            }
        } else {
            if (path.getBuildState() != s) {
                throw new IllegalStateException("state = " + path.getBuildState());
            }
        }
    }

    public MovingPoint createMovingPoint(SubPath sp) {
        AnchorPoint last = sp.getLastAnchor();
        return new MovingPoint(lastX, lastY, last, last.getView());
    }

    @Override
    public String toString() {
        return "Build";
    }
}

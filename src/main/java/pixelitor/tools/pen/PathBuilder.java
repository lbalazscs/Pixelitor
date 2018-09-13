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
import pixelitor.Composition;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.history.History;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.history.FinishSubPathEdit;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Messages;
import pixelitor.utils.debug.DebugNode;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

import static pixelitor.tools.pen.AnchorPointType.CUSP;
import static pixelitor.tools.pen.AnchorPointType.SMOOTH;
import static pixelitor.tools.pen.AnchorPointType.SYMMETRIC;
import static pixelitor.tools.pen.PathBuilder.State.BEFORE_SUBPATH;
import static pixelitor.tools.pen.PathBuilder.State.CTRL_DRAGGING_PREVIOUS;
import static pixelitor.tools.pen.PathBuilder.State.DRAGGING_THE_CONTROL_OF_LAST;
import static pixelitor.tools.pen.PathBuilder.State.MOVING_TO_NEXT_ANCHOR;
import static pixelitor.tools.util.DraggablePoint.activePoint;

/**
 * A pen tool interaction mode where a path can be built from scratch
 */
public class PathBuilder implements PenToolMode {
    private double lastX;
    private double lastY;

    public static final PathBuilder INSTANCE = new PathBuilder();
    private static final String BUILDER_HELP_MESSAGE =
            "<html>Pen Tool Build Mode: " +
                    "<b>click</b> and <b>drag</b> to create a Bezier curve. " +
                    "<b>Ctrl-click</b> or close the path to finish. " +
                    "<b>Ctrl-drag</b> points to move them, " +
                    "<b>Alt-drag</b> handles to break them.";

    public enum State {
        // the initial state, and also the state after a subpath is finished
        BEFORE_SUBPATH(false),
        DRAGGING_THE_CONTROL_OF_LAST(true),
        MOVING_TO_NEXT_ANCHOR(false),
        CTRL_DRAGGING_PREVIOUS(true);

        private final boolean dragging;

        State(boolean dragging) {
            this.dragging = dragging;
        }

        public boolean isDragging() {
            return dragging;
        }
    }

    private State state;
    private State prevState;

    private Path path;

    private boolean rubberBand = true;

    private PathBuilder() {
        setState(BEFORE_SUBPATH, "constructor");
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public void setPath(Path path, String reason) {
//        if (this.path != path) {
//            System.out.println("PB.setPath: "
//                    + red(this.path) + " => " + green(path)
//                    + "(" + reason + ")");
//        }
        if (this.path != path || path == null) {
            setState(BEFORE_SUBPATH, "PB.setPath to " + path + " (" + reason + ")");
        }
        this.path = path;
    }

    public void setState(State state, String reason) {
        if (this.state == state) {
            return;
        }
//            System.out.println("PB.setState: " + red(this.state)
//                    + " => " + green(state) + " (" + reason + "), path = " + path);
        if (Build.CURRENT.isDevelopment()) {
            Messages.showInStatusBar("<html><font color=red>" + this.state
                    + "</font><font size=+1> \u21e8 </font><font color=#004E00>" + state
                    + "</font> (" + reason + ")");
        }
        if (state == MOVING_TO_NEXT_ANCHOR) {
            if (path == null) {
                throw new IllegalStateException("no path, change: "
                        + this.state + " => " + state
                        + ", reason = " + reason);
            }
            if (!path.hasMovingPoint()) {
                ImageComponent ic = ImageComponents.getActiveIC();
                path.setMoving(new AnchorPoint(lastX, lastY, ic), reason);
            }
        }

        prevState = this.state;
        this.state = state;
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        assert state == BEFORE_SUBPATH
                || state == MOVING_TO_NEXT_ANCHOR : "state = " + state;

        double x = e.getCoX();
        double y = e.getCoY();
        lastX = x;
        lastY = y;
        boolean altDown = e.isAltDown();
        boolean controlDown = e.isControlDown();

        if (state == BEFORE_SUBPATH) {
            if (path == null) {
                Path p = new Path(e.getComp(), true);
                setPath(p, "mousePressed");
            }

            if (controlDown) {
                // if we are over an old point, just move it
                DraggablePoint point = path.handleWasHit(x, y, altDown);
                if (point != null) {
                    if (point instanceof ControlPoint) {
                        // if the old point is a control point, then the opposite
                        // point should move smoothly, not symmetrically
                        ControlPoint cp = (ControlPoint) point;
                        cp.getAnchor().setType(SMOOTH);
                    }
                    startMovingPrevious(x, y, point, "ctrl-press in BEFORE_SUBPATH");
                    return;
                }
            }
            if (altDown) {
                // if alt is down, then break control points
                DraggablePoint point = path.handleWasHit(x, y, altDown);
                if (point != null) {
                    if (point instanceof ControlPoint) {
                        breakTheControlAndStartMovingIt(x, y, point);
                        return;
                    }
                }
            }

            // only add a point if previously we were
            // in the BEFORE_SUBPATH state. Normally points
            // are added in mouseReleased
            AnchorPoint p = new AnchorPoint(e);

            path.startNewSubPath(p, true);
        } else if (state == MOVING_TO_NEXT_ANCHOR) {
            assert path.hasMovingPoint();

            if (controlDown) {
                DraggablePoint point = path.handleWasHit(x, y, altDown);
                if (point != null) {
                    startMovingPrevious(x, y, point, "ctrl-click on previous");
                    return;
                } else {
                    // control is down, but nothing was hit
                    finishByCtrlClick(e);
                    return;
                }
            }

            boolean altDownNothingHit = false;
            if (altDown) {
                DraggablePoint point = path.handleWasHit(x, y, altDown);
                if (point != null) {
                    if (point instanceof ControlPoint) {
                        breakTheControlAndStartMovingIt(x, y, point);
                        return;
                    } else if (point instanceof AnchorPoint) {
                        startDraggingOutNewHandles(x, y, (AnchorPoint) point);
                        return;
                    }
                } else {
                    altDownNothingHit = true;
                }
            }

            if (tryClosing(x, y, e.getComp())) {
                return;
            } else {
                // fix the final position of the moved curve point
                AnchorPoint ap = path.addMovingPointAsAnchor(x, y);
                if (altDownNothingHit) {
                    ap.setType(CUSP);
                }
            }
        }
        setState(DRAGGING_THE_CONTROL_OF_LAST, "mousePressed");
        assert path.checkWiring();
    }

    private void startDraggingOutNewHandles(double x, double y, AnchorPoint point) {
        AnchorPoint ap = point;
        ap.retractHandles();
        // drag the retracted handles out
        ap.setType(SYMMETRIC);
        startMovingPrevious(x, y, ap.ctrlOut, "alt-drag out handles of previous");
    }

    private void breakTheControlAndStartMovingIt(double x, double y, DraggablePoint point) {
        // alt-press on an anchor point should break the handle
        ControlPoint cp = (ControlPoint) point;
        cp.getAnchor().setType(CUSP);
        // after breaking, move it as usual
        startMovingPrevious(x, y, point, "alt-click on previous");
    }

    private void startMovingPrevious(double x, double y, DraggablePoint point, String s) {
        // if the mouse was ctrl-pressed over
        // a visible anchor or control point, move it
        point.setActive(true);
        point.mousePressed(x, y);
        setState(CTRL_DRAGGING_PREVIOUS, s);
    }

    // return true if it could be closed
    private boolean tryClosing(double x, double y, Composition comp) {
        AnchorPoint first = path.getFirst();
        if (shouldBeClosed(first, x, y)) {
            first.setActive(false);
            path.close(true);
            finish(comp, "closing");
            return true;
        }
        return false;
    }

    private boolean shouldBeClosed(AnchorPoint first, double x, double y) {
        return first.handleContains(x, y)
                && path.getNumAnchorPointsInActiveSubpath() >= 2;
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        if (state == BEFORE_SUBPATH) {
            // normally it should not happen, but in some rare cases
            // a dragged event comes without a preceding pressed event
            return;
        }

        assert state.isDragging() : "state = " + state;
        double x = e.getCoX();
        double y = e.getCoY();
        lastX = x;
        lastY = y;

        if (state == CTRL_DRAGGING_PREVIOUS) {
            activePoint.mouseDragged(x, y);
            path.setMovingLocation(x, y, true);
            return;
        }

        boolean breakHandle = e.isAltDown();
        AnchorPoint last = path.getLast();
        if (breakHandle) {
            last.setType(CUSP);
        } else {
            last.setType(SYMMETRIC);
        }
        ControlPoint ctrlOut = last.ctrlOut;
        ctrlOut.setLocation(x, y);

        setState(DRAGGING_THE_CONTROL_OF_LAST, "mouseDragged");
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
        if (state == BEFORE_SUBPATH) {
            return;
        }
        assert state.isDragging() : "state = " + state;

        double x = e.getCoX();
        double y = e.getCoY();
        lastX = x;
        lastY = y;

        if (state == CTRL_DRAGGING_PREVIOUS) {
            activePoint.mouseReleased(x, y);
            activePoint
                    .createMovedEdit(e.getComp())
                    .ifPresent(History::addEdit);
            // after this brief interruption, return to the previous state
            setState(prevState, "mouseReleased in CTRL_DRAGGING_PREVIOUS");
            return;
        } else if (state == DRAGGING_THE_CONTROL_OF_LAST) {
            ControlPoint ctrlOut = path.getLast().ctrlOut;
            ctrlOut.setLocation(x, y);
            ctrlOut.afterMouseReleasedActions();
            path.setMoving(new AnchorPoint(x, y, e.getView()), "mouseReleased");
        }

        setState(MOVING_TO_NEXT_ANCHOR, "mouseReleased");
        assert path.checkWiring();
    }

    @Override
    public boolean mouseMoved(MouseEvent e, ImageComponent ic) {
        if (state == BEFORE_SUBPATH) {
            return false;
        }

        assert state == MOVING_TO_NEXT_ANCHOR : "state = " + state;

        int x = e.getX();
        int y = e.getY();
        lastX = x;
        lastY = y;

        path.setMovingLocation(x, y, false);

        AnchorPoint first = path.getFirst();
        if (first.handleContains(x, y)) {
            first.setActive(true);
        } else {
            first.setActive(false);
        }

        return true;
    }

    @Override
    public void paint(Graphics2D g) {
        if (path != null) {
            path.paintForBuilding(g, state);
        }
    }

    private void finishByCtrlClick(PMouseEvent e) {
        assert state == MOVING_TO_NEXT_ANCHOR : "state = " + state;

        History.addEdit(new FinishSubPathEdit(e.getComp(), path.getActiveSubpath()));
        finish(e.getComp(), "ctrl-click");
    }

    // A subpath can be finished either by closing it or by ctrl-clicking.
    // Either way, we end up in this method.
    private void finish(Composition comp, String reason) {
        String r = "finish(" + reason + ")";
        path.finishActiveSubpath(r);
        setState(BEFORE_SUBPATH, r);
        comp.setActivePath(path);
        Tools.PEN.enableActionsBasedOnFinishedPath(true);
    }

    @Override
    public String getToolMessage() {
        return BUILDER_HELP_MESSAGE;
    }

    @Override
    public void modeEnded() {
        if (path != null) {
            path.finishActiveSubpath("PB.modeEnded");
            setState(BEFORE_SUBPATH, "PB.modeEnded");
        } else {
            assertStateIs(BEFORE_SUBPATH);
        }
        PenToolMode.super.modeEnded();
    }

    public void assertStateIs(State s) {
        assert state == s : "state = " + state;
    }

    public boolean showRubberBand() {
        return rubberBand;
    }

    public void setShowRubberBand(boolean showRubberBand) {
        this.rubberBand = showRubberBand;
    }

    @Override
    public String toString() {
        return "Build";
    }

    @Override
    public DebugNode createDebugNode() {
        DebugNode node = PenToolMode.super.createDebugNode();
        node.addString("State", state.toString());

        return node;
    }
}

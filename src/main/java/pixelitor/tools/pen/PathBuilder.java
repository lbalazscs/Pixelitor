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

import pixelitor.gui.ImageComponent;
import pixelitor.tools.Tools;
import pixelitor.tools.util.PMouseEvent;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

import static pixelitor.tools.pen.PathBuilder.State.BEFORE_SUBPATH;
import static pixelitor.tools.pen.PathBuilder.State.DRAGGING_THE_CONTROL_OF_LAST;
import static pixelitor.tools.pen.PathBuilder.State.MOVING_TO_NEXT_CURVE_POINT;

/**
 * A pen tool interaction mode where a path can be built from scratch
 */
public class PathBuilder implements PenToolMode {
    public static final PathBuilder INSTANCE = new PathBuilder();
    public static final String BUILDER_HELP_MESSAGE =
            "<html>Pen Tool Build Mode: " +
                    "<b>click</b> and <b>drag</b> to create a Bezier curve. " +
                    "<b>Ctrl-click</b> or close the path to finish. " +
                    "Press <b>Esc</b> to start from scratch.";

    public enum State {
        // the initial state, and also the state after a subpath is finished
        BEFORE_SUBPATH {
        }, DRAGGING_THE_CONTROL_OF_LAST {
        }, MOVING_TO_NEXT_CURVE_POINT {
        }
    }

    private State state;

    private Path path;

    private PathBuilder() {
        setState(BEFORE_SUBPATH);
    }

    @Override
    public void setPath(Path path) {
        this.path = path;
        if (path == null) {
            setState(BEFORE_SUBPATH);
        }
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        assert state == BEFORE_SUBPATH
                || state == MOVING_TO_NEXT_CURVE_POINT : "state = " + state;

        if (state == BEFORE_SUBPATH) {
            // only add a point if previously we were
            // in the initial mode. Normally points
            // are added in mouseReleased
            AnchorPoint p = new AnchorPoint(e);

            path.startNewSubPath(p, true);
        } else if (state == MOVING_TO_NEXT_CURVE_POINT) {
            if (e.isControlDown()) {
                // TODO should check whether we pressed the mouse over a visible
                // anchor point or a control point, and if yes, move them

                // TODO similarly, Alt-Click should mean breaking the control handle

                finishByCtrlClick(e);
                return;
            }

            double x = e.getCoX();
            double y = e.getCoY();
            AnchorPoint first = path.getFirst();
            if (shouldBeClosed(first, x, y)) {
                first.setActive(false);
                path.close(true);
                finish(e);
                return;
            } else {
                // fix the final position of the moved curve point
                path.finalizeMovingPoint(x, y, false);
            }
        }
        setState(DRAGGING_THE_CONTROL_OF_LAST);
        assert path.checkWiring();
    }

    private boolean shouldBeClosed(AnchorPoint first, double x, double y) {
        return first.handleContains(x, y)
                && path.getNumPointsInActiveSubpath() >= 2;
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        if (state == BEFORE_SUBPATH) {
            // normally it should not happen, but in some rare cases
            // a dragged event comes without a preceding pressed event
            return;
        }

        assert state == DRAGGING_THE_CONTROL_OF_LAST : "state = " + state;

        double x = e.getCoX();
        double y = e.getCoY();
        ControlPoint ctrlOut = path.getLast().ctrlOut;
        ctrlOut.setLocation(x, y);

        setState(DRAGGING_THE_CONTROL_OF_LAST);
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
        if (state == BEFORE_SUBPATH) {
            return;
        }

        double x = e.getCoX();
        double y = e.getCoY();

        assert state == DRAGGING_THE_CONTROL_OF_LAST : "state = " + state;

        ControlPoint ctrlOut = path.getLast().ctrlOut;
        ctrlOut.setLocation(x, y);
        ctrlOut.afterMouseReleasedActions();

        path.setMoving(new AnchorPoint(x, y, e.getView()));
        setState(MOVING_TO_NEXT_CURVE_POINT);
        assert path.checkWiring();
    }

    @Override
    public boolean mouseMoved(MouseEvent e, ImageComponent ic) {
        if (state == BEFORE_SUBPATH) {
            return false;
        }

        assert state == MOVING_TO_NEXT_CURVE_POINT : "state = " + state;

        int x = e.getX();
        int y = e.getY();

        AnchorPoint moving = path.getMoving();
        moving.setLocation(x, y);

        AnchorPoint first = path.getFirst();
        if (first.handleContains(x, y)) {
            first.setActive(true);
        } else {
            first.setActive(false);
        }

        setState(MOVING_TO_NEXT_CURVE_POINT);
        return true;
    }

    @Override
    public void paint(Graphics2D g) {
        if (path != null) {
            path.paintForBuilding(g, state);
        }
    }

    private void finishByCtrlClick(PMouseEvent e) {
        double x = e.getCoX();
        double y = e.getCoY();

        if (state == DRAGGING_THE_CONTROL_OF_LAST) {
            ControlPoint p = path.getLast().ctrlOut;
            p.setLocation(x, y);
            p.calcImCoords();
        } else if (state == MOVING_TO_NEXT_CURVE_POINT) {
            path.finalizeMovingPoint(x, y, true);
        }
        finish(e);
    }

    // A subpath can be finished either by closing it or by ctrl-clicking.
    // Either way, we end up in this method.
    private void finish(PMouseEvent e) {
        path.finishActiveSubpath();
        setState(BEFORE_SUBPATH);
        e.getComp().setActivePath(path);
        Tools.PEN.enableConvertToSelection(true);
    }

    public void assertStateIs(State s) {
        assert state == s : "state = " + state;
    }

    @Override
    public String toString() {
        return "Build";
    }

    @Override
    public String getToolMessage() {
        return BUILDER_HELP_MESSAGE;
    }
}

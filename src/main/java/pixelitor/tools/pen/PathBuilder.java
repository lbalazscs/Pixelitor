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
import pixelitor.tools.util.PMouseEvent;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

import static pixelitor.tools.pen.PathBuilder.State.DRAGGING_THE_CONTROL_OF_LAST;
import static pixelitor.tools.pen.PathBuilder.State.FINISHED;
import static pixelitor.tools.pen.PathBuilder.State.INITIAL;
import static pixelitor.tools.pen.PathBuilder.State.MOVING_TO_NEXT_CURVE_POINT;

/**
 * A pen tool interaction mode where a path can be built from scratch
 */
public class PathBuilder implements PenToolMode {
    public enum State {
        INITIAL {
        }, DRAGGING_THE_CONTROL_OF_LAST {
        }, MOVING_TO_NEXT_CURVE_POINT {
        }, FINISHED {
        }
    }

//    private int lastMousePressX;
//    private int lastMousePressY;

    private State state;

    //    private SubPath activeSubPath;
    private Path path;

    public PathBuilder() {
        state = INITIAL;
    }

    @Override
    public void setPath(Path path) {
        this.path = path;
        if (path == null) {
            state = INITIAL;
        }
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        if (state == FINISHED) {
            return;
        }

        if (e.isControlDown()) {
            finishByCtrlClick(e);
            return;
        }

        assert state == INITIAL
                || state == MOVING_TO_NEXT_CURVE_POINT
                || state == FINISHED : "state = " + state;

        if (state == INITIAL) {
            // only add a point if previously we were
            // in the initial mode. Normally points
            // are added in mouseReleased
            AnchorPoint p = new AnchorPoint(e);

            path.startNewSubPath(p, true);
//            this.activeSubPath = path.getActiveSubpath();
        } else if (state == MOVING_TO_NEXT_CURVE_POINT) {
            int x = e.getCoX();
            int y = e.getCoY();
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

    private boolean shouldBeClosed(AnchorPoint first, int x, int y) {
        return first.handleContains(x, y)
                && path.getNumPointsInActiveSubpath() > 2;
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        if (state == FINISHED) {
            return;
        }
        assert state == DRAGGING_THE_CONTROL_OF_LAST : "state = " + state;

        int x = e.getCoX();
        int y = e.getCoY();
        ControlPoint ctrlOut = path.getLast().ctrlOut;
        ctrlOut.setLocation(x, y);

        setState(DRAGGING_THE_CONTROL_OF_LAST);
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
        if (state == FINISHED) {
            return;
        }

        int x = e.getCoX();
        int y = e.getCoY();

        assert state == DRAGGING_THE_CONTROL_OF_LAST : "state = " + state;

        ControlPoint ctrlOut = path.getLast().ctrlOut;
        ctrlOut.setLocation(x, y);
        ctrlOut.afterMouseReleasedActions();

        path.setMoving(new AnchorPoint(x, y, e.getIC()));
        setState(MOVING_TO_NEXT_CURVE_POINT);
        assert path.checkWiring();
    }

    @Override
    public boolean mouseMoved(MouseEvent e, ImageComponent ic) {
        if (state == INITIAL || state == FINISHED) {
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
        int x = e.getCoX();
        int y = e.getCoY();

        if (state == DRAGGING_THE_CONTROL_OF_LAST) {
            ControlPoint p = path.getLast().ctrlOut;
            p.setLocation(x, y);
            p.calcImCoords();
        } else if (state == MOVING_TO_NEXT_CURVE_POINT) {
            path.finalizeMovingPoint(x, y, true);
        }
        finish(e);
    }

    private void finish(PMouseEvent e) {
        setState(FINISHED);
        e.getComp().setActivePath(path);
    }

    public void assertStateIs(State s) {
        assert state == s : "state = " + state;
    }
}

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
import pixelitor.tools.PMouseEvent;

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
    enum State {
        INITIAL {
        }, DRAGGING_THE_CONTROL_OF_LAST {
        }, MOVING_TO_NEXT_CURVE_POINT {
        }, FINISHED {
        }
    }

//    private int lastMousePressX;
//    private int lastMousePressY;

    private State state;

    private SubPath activeSubPath;
    private final Path path;

    public PathBuilder(Path path) {
        this.path = path;
        state = INITIAL;
    }

    private void setState(State state) {
        if (this.state == FINISHED) {
            throw new IllegalStateException();
        }
        this.state = state;
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        if (state == FINISHED) {
            return;
        }

        int x = e.getCoX();
        int y = e.getCoY();

        if (e.isControlDown()) {
            finish(x, y);
            return;
        }

//        System.out.printf("PathBuilder::mousePressed: x = %d, y = %d, state = %s%n", x, y, state);

//        lastMousePressX = x;
//        lastMousePressY = y;

        assert state == INITIAL
                || state == MOVING_TO_NEXT_CURVE_POINT
                || state == FINISHED : "state = " + state;

        if (state == INITIAL) {
            // only add a point if previously we were
            // in the initial mode. Normally points
            // are added in mouseReleased
            AnchorPoint p = new AnchorPoint(x, y, e.getIC());

            path.startNewSubPath(p);
            this.activeSubPath = path.getActiveSubpath();
        } else if (state == MOVING_TO_NEXT_CURVE_POINT) {
            AnchorPoint first = activeSubPath.getFirst();
            if (first.handleContains(x, y) && activeSubPath.getNumPoints() > 2) {
                first.setActive(false);
                activeSubPath.close();
                setState(FINISHED);
                return;
            } else {
                // fix the final position of the moved curve point
                activeSubPath.finalizeMovingPoint(x, y);
            }
        }
        setState(DRAGGING_THE_CONTROL_OF_LAST);
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        if (state == FINISHED) {
            return;
        }
        assert state == DRAGGING_THE_CONTROL_OF_LAST : "state = " + state;

        int x = e.getCoX();
        int y = e.getCoY();
        ControlPoint p = activeSubPath.getLast().ctrlOut;
        p.setLocation(x, y);

        setState(DRAGGING_THE_CONTROL_OF_LAST);
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
        if (state == FINISHED) {
            return;
        }

        int x = e.getCoX();
        int y = e.getCoY();

//        System.out.printf("PathBuilder::mouseReleased: x = %d, y = %d, state = %s%n", x, y, state);

        assert state == DRAGGING_THE_CONTROL_OF_LAST : "state = " + state;

        ControlPoint ctrlOut = activeSubPath.getLast().ctrlOut;
        ctrlOut.setLocation(x, y);
        ctrlOut.afterMouseReleasedActions();

        activeSubPath.setMovingPoint(new AnchorPoint(x, y, e.getIC()));
        setState(MOVING_TO_NEXT_CURVE_POINT);
    }

//    private boolean wasClick(int x, int y) {
//        return x == lastMousePressX && y == lastMousePressY;
//    }

    @Override
    public boolean mouseMoved(MouseEvent e, ImageComponent ic) {
        if (state == INITIAL || state == FINISHED) {
            return false;
        }

        assert state == MOVING_TO_NEXT_CURVE_POINT : "state = " + state;

        int x = e.getX();
        int y = e.getY();

//        System.out.printf("PathBuilder::mouseMoved: x = %d, y = %d, state = %s%n", x, y, state);

        activeSubPath.getMoving().setLocation(x, y);

        AnchorPoint first = activeSubPath.getFirst();
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
        path.paintForBuilding(g, state);
    }

    public SubPath getActiveSubPath() {
        return activeSubPath;
    }

    public void finish(int x, int y) {
//        System.out.printf("PathBuilder::finish: x = %d, y = %d, state = %s%n", x, y, state);

        if (state == DRAGGING_THE_CONTROL_OF_LAST) {
            ControlPoint p = activeSubPath.getLast().ctrlOut;
            p.setLocation(x, y);
            p.calcImCoords();
        } else if (state == MOVING_TO_NEXT_CURVE_POINT) {
            activeSubPath.finalizeMovingPoint(x, y);
        }
        setState(FINISHED);
    }

    public void assertStateIs(State s) {
        assert state == s : "state = " + state;
    }
}

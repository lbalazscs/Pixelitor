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

import static pixelitor.tools.pen.PathBuilder.State.DRAGGING_THE_CONTROLS;
import static pixelitor.tools.pen.PathBuilder.State.FINISHED;
import static pixelitor.tools.pen.PathBuilder.State.INITIAL;
import static pixelitor.tools.pen.PathBuilder.State.MOVING_TO_NEXT_CURVE_POINT;

/**
 * A pen tool interaction mode where a path can be built from scratch
 */
public class PathBuilder implements PenToolMode {
    enum State {
        INITIAL {
        }, DRAGGING_THE_CONTROLS {
        }, MOVING_TO_NEXT_CURVE_POINT {
        }, FINISHED {
        }
    }

    private int lastMousePressX;
    private int lastMousePressY;

    private State state;

    private Path path;

    public PathBuilder(Path path) {
        this.path = path;
        state = INITIAL;
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        int x = e.getCoX();
        int y = e.getCoY();

        lastMousePressX = x;
        lastMousePressY = y;

        assert state == INITIAL
                || state == MOVING_TO_NEXT_CURVE_POINT
                || state == FINISHED : "state = " + state;

        if (state == INITIAL) {
            // only add a point if previously we were
            // in the initial mode. Normally points
            // are added in mouseReleased
            CurvePoint p = new CurvePoint(x, y, e.getIC());
            path.addPoint(p);
        } else if (state == MOVING_TO_NEXT_CURVE_POINT) {
            // fix the final position of the moved curve point
            CurvePoint last = path.getLast();
            last.setLocation(x, y);
            last.calcImCoords();
        }
        state = DRAGGING_THE_CONTROLS;
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        assert state == DRAGGING_THE_CONTROLS : "state = " + state;

        int x = e.getCoX();
        int y = e.getCoY();
        ControlPoint p = path.getLast().ctrlOut;
        p.setLocation(x, y);

        state = DRAGGING_THE_CONTROLS;
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
        int x = e.getCoX();
        int y = e.getCoY();

        if (wasClick(x, y)) {
            // set the state as if mousePressed didn't happen
            state = MOVING_TO_NEXT_CURVE_POINT;
            if (path.getNumPoints() == 1) {
                // if there is only one point, it means
                // that it was added in mousePressed
                path.resetToInitialState();
            }
            return;
        }

        if (state == FINISHED) {
            return;
        }

        assert state == DRAGGING_THE_CONTROLS : "state = " + state;

        ControlPoint ctrlOut = path.getLast().ctrlOut;
        ctrlOut.setLocation(x, y);
        ctrlOut.afterMouseReleasedActions();

        path.addPoint(new CurvePoint(x, y, e.getIC()));
        state = MOVING_TO_NEXT_CURVE_POINT;
    }

    private boolean wasClick(int x, int y) {
        return x == lastMousePressX && y == lastMousePressY;
    }

    @Override
    public boolean mouseMoved(MouseEvent e, ImageComponent ic) {
        if (state == INITIAL || state == FINISHED) {
            return false;
        }

        assert state == MOVING_TO_NEXT_CURVE_POINT : "state = " + state;

        int x = e.getX();
        int y = e.getY();
        path.getLast().setLocation(x, y);

        state = MOVING_TO_NEXT_CURVE_POINT;
        return true;
    }

    @Override
    public void mouseClicked(PMouseEvent e) {
        if (e.getClickCount() >= 2) {
            int x = e.getCoX();
            int y = e.getCoY();
            finish(x, y);
        }
    }

    @Override
    public void paint(Graphics2D g) {
        path.paintForBuilding(g, state);
    }

    public Path getPath() {
        return path;
    }

    public void finish(int x, int y) {
        if (state == DRAGGING_THE_CONTROLS) {
            ControlPoint p = path.getLast().ctrlOut;
            p.setLocation(x, y);
            p.calcImCoords();
        }
        state = FINISHED;
    }

    public void assertStateIs(State s) {
        assert state == s : "state = " + state;
    }

    public void assertNumCurvePointsIs(int expected) {
        int actual = path.getNumPoints();
        assert actual == expected
                : "numPoints is " + actual + ", expecting " + expected;
    }
}

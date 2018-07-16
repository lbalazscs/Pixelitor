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
import pixelitor.gui.View;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.utils.Shapes;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

import static pixelitor.tools.pen.AnchorPointType.SMOOTH;

/**
 * A path contains the same information as a {@link PathIterator},
 * but in a way that makes it possible to interactively build and edit it.
 *
 * Technically, it consists of a series of geometrically distinct
 * {@link SubPath} objects (typically only one).
 */
public class Path {
    private final List<SubPath> subPaths = new ArrayList<>();
    private SubPath activeSubPath;

    public SubPath getActiveSubpath() {
        return activeSubPath;
    }

    public void paintForBuilding(Graphics2D g, PathBuilder.State state) {
        Shapes.drawVisible(g, toComponentSpaceShape());

        for (SubPath sp : subPaths) {
            sp.paintHandlesForBuilding(g, state);
        }
    }

    public void paintForEditing(Graphics2D g) {
        Shapes.drawVisible(g, toComponentSpaceShape());

        for (SubPath sp : subPaths) {
            sp.paintHandlesForEditing(g);
        }
    }

    public DraggablePoint handleWasHit(int x, int y, boolean altDown) {
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

    public void startNewSubPath(AnchorPoint first) {
        activeSubPath = new SubPath();
        subPaths.add(activeSubPath);
        activeSubPath.addFirstPoint(first);
    }

    public void addPoint(AnchorPoint ap) {
        activeSubPath.addPoint(ap);
    }

    public AnchorPoint getLast() {
        return activeSubPath.getLast();
    }

    public void close() {
        activeSubPath.close();
    }

    @SuppressWarnings("SameReturnValue")
    public boolean checkWiring() {
        for (SubPath sp : subPaths) {
            sp.checkWiring();
        }
        return true;
    }

    public void dump() {
        for (SubPath sp : subPaths) {
            System.out.println("New subpath");
            sp.dump();
        }
    }

    public void changeTypesForEditing(boolean pathWasBuiltInteractively) {
        for (SubPath sp : subPaths) {
            sp.changeTypesForEditing(pathWasBuiltInteractively);
        }
    }

    public void viewSizeChanged(View view) {
        for (SubPath sp : subPaths) {
            sp.viewSizeChanged(view);
        }
    }

    public void startNewSubpath(double x, double y, ImageComponent ic) {
        x = ic.imageXToComponentSpace(x);
        y = ic.imageYToComponentSpace(y);
        AnchorPoint first = new AnchorPoint(x, y, ic);
        first.setType(SMOOTH);
        startNewSubPath(first);
    }

    public void addLine(double newX, double newY, ImageComponent ic) {
        newX = ic.imageXToComponentSpace(newX);
        newY = ic.imageYToComponentSpace(newY);
        AnchorPoint ap = new AnchorPoint(newX, newY, ic);
        addPoint(ap);
    }

    public void addCubicCurve(double c1x, double c1y,
                              double c2x, double c2y,
                              double newX, double newY, ImageComponent ic) {
        ControlPoint lastOut = getLast().ctrlOut;
        c1x = ic.imageXToComponentSpace(c1x);
        c1y = ic.imageYToComponentSpace(c1y);
        lastOut.setLocationOnlyForThis(c1x, c1y);
        lastOut.afterMovingActionsForThis();

        newX = ic.imageXToComponentSpace(newX);
        newY = ic.imageYToComponentSpace(newY);
        AnchorPoint next = new AnchorPoint(newX, newY, ic);
        addPoint(next);
        next.setType(SMOOTH);

        c2x = ic.imageXToComponentSpace(c2x);
        c2y = ic.imageYToComponentSpace(c2y);
        ControlPoint nextIn = next.ctrlIn;
        nextIn.setLocationOnlyForThis(c2x, c2y);
        nextIn.afterMovingActionsForThis();
    }

    public void addQuadCurve(double cx, double cy,
                             double newX, double newY, ImageComponent ic) {
        cx = ic.imageXToComponentSpace(cx);
        cy = ic.imageYToComponentSpace(cy);
        newX = ic.imageXToComponentSpace(newX);
        newY = ic.imageYToComponentSpace(newY);
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

        AnchorPoint next = new AnchorPoint(newX, newY, ic);
        addPoint(next);
        next.setType(SMOOTH);

        ControlPoint nextIn = next.ctrlIn;
        nextIn.setLocationOnlyForThis(cp2x, cp2y);
        nextIn.afterMovingActionsForThis();
    }
}

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

import pixelitor.gui.View;
import pixelitor.tools.DraggablePoint;
import pixelitor.utils.Shapes;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

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
}

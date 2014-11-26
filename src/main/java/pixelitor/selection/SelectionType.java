/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.selection;

import pixelitor.tools.UserDrag;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;

/**
 * The type of a new selection created interactively by the user
 */
public enum SelectionType {
    RECTANGLE {
        @Override
        public Shape updateShape(UserDrag userDrag, Shape currentSelectionShape) {
            Rectangle dragRectangle = userDrag.createPositiveRectangle();
            return dragRectangle;
        }

        @Override
        public String toString() {
            return "Rectangle";
        }
    }, ELLIPSE {
        @Override
        public Shape updateShape(UserDrag userDrag, Shape currentSelectionShape) {
            Rectangle dr = userDrag.createPositiveRectangle();
            return new Ellipse2D.Float(dr.x, dr.y, dr.width, dr.height);
        }

        @Override
        public String toString() {
            return "Ellipse";
        }
    }, LASSO {
        @Override
        public Shape updateShape(UserDrag userDrag, Shape currentSelectionShape) {
            boolean createNew;
            if (currentSelectionShape == null) {
                createNew = true;
            } else if (currentSelectionShape instanceof GeneralPath) {
                createNew = false;
            } else { // it is an Area, meaning that a new shape has been started
                createNew = true;
            }

            if (createNew) {
                GeneralPath p = new GeneralPath();
                p.moveTo(userDrag.getStartX(), userDrag.getStartY());
                p.lineTo(userDrag.getEndX(), userDrag.getEndY());
                return p;
            } else {
                GeneralPath gp = (GeneralPath) currentSelectionShape;
                gp.lineTo(userDrag.getEndX(), userDrag.getEndY());

                return gp;
            }
        }

        @Override
        public String toString() {
            return "Freehand";
        }
//    }, POLYGONAL_LASSO {
//        @Override
//        public Shape updateShape(UserDrag userDrag, Shape currentSelectionShape) {
//            boolean createNew = false;
//            if (currentSelectionShape == null) {
//                createNew = true;
//            } else if (currentSelectionShape instanceof Polygon) {
//                createNew = false;
//            } else { // it is an Area, meaning that a new shape has been started
//                createNew = true;
//            }
//
//            if(createNew) {
//                Polygon polygon = new Polygon();
//                polygon.addPoint(userDrag.getStartX(), userDrag.getStartY());
//                polygon.addPoint(userDrag.getEndX(), userDrag.getEndY());
//                return polygon;
//            } else {
//                Polygon polygon = (Polygon) currentSelectionShape;
//                int[] xPoints = polygon.xpoints;
//                int[] yPoints = polygon.ypoints;
//                int nrPoints = polygon.npoints;
//                xPoints[nrPoints - 1] = userDrag.getEndX();
//                yPoints[nrPoints - 1] = userDrag.getEndY();
//                return polygon;
//            }
//        }
//
//
//        @Override
//        public String toString() {
//            return "Polygonal";
//        }
    };

    public abstract Shape updateShape(UserDrag userDrag, Shape currentSelectionShape);
}

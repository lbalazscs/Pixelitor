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

package pixelitor.selection;

import pixelitor.tools.ImDrag;
import pixelitor.tools.PMouseEvent;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

/**
 * The type of a new selection created interactively by the user.
 * Corresponds to the "Type" combo box in the Selection Tool.
 */
public enum SelectionType {
    RECTANGLE("Rectangle") {
        @Override
        public Shape createShape(Object mouseInfo, Shape oldShape) {
            ImDrag imDrag = (ImDrag) mouseInfo;
            return imDrag.createPositiveRect();
        }
    }, ELLIPSE("Ellipse") {
        @Override
        public Shape createShape(Object mouseInfo, Shape oldShape) {
            ImDrag imDrag = (ImDrag) mouseInfo;
            Rectangle2D dr = imDrag.createPositiveRect();
            return new Ellipse2D.Double(dr.getX(), dr.getY(), dr.getWidth(), dr.getHeight());
        }
    }, LASSO("Freehand") {
        @Override
        public Shape createShape(Object mouseInfo, Shape oldShape) {
            ImDrag imDrag = (ImDrag) mouseInfo;
            boolean createNew;
            if (oldShape == null) {
                createNew = true;
            } else if (oldShape instanceof GeneralPath) {
                createNew = false;
            } else { // it is an Area, meaning that a new shape has been started
                createNew = true;
            }

            if (createNew) {
                GeneralPath p = new GeneralPath();
                p.moveTo(imDrag.getStartX(), imDrag.getStartY());
                p.lineTo(imDrag.getEndX(), imDrag.getEndY());
                return p;
            } else {
                GeneralPath gp = (GeneralPath) oldShape;
                gp.lineTo(imDrag.getEndX(), imDrag.getEndY());

                return gp;
            }
        }
    }, POLYGONAL_LASSO("Polygonal") {
        @Override
        public Shape createShape(Object mouseInfo, Shape oldShape) {
            PMouseEvent pe = (PMouseEvent) mouseInfo;
            boolean createNew;
            if (oldShape == null) {
                createNew = true;
            } else if (oldShape instanceof GeneralPath) {
                createNew = false;
            } else { // it is an Area, meaning that a new shape has been started
                createNew = true;
            }

            if (createNew) {
                GeneralPath p = new GeneralPath();
                p.moveTo(pe.getImX(), pe.getImY());
                return p;
            } else {
                GeneralPath gp = (GeneralPath) oldShape;
                gp.lineTo(pe.getImX(), pe.getImY());

                return gp;
            }
        }
    };

    private final String guiName;

    SelectionType(String guiName) {
        this.guiName = guiName;
    }

    public abstract Shape createShape(Object mouseInfo, Shape oldShape);

    @Override
    public String toString() {
        return guiName;
    }
}

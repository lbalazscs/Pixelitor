/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.tools.selection.MagicWandSelectionTool;
import pixelitor.tools.util.Drag;
import pixelitor.tools.util.PMouseEvent;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * The different ways a selection shape can be created or updated interactively.
 * Depending on the tool's interaction model (drag-based vs. click-based),
 * each constant implements either createShapeFromDrag or createShapeFromEvent,
 * throwing an exception for the unsupported interaction type.
 */
public enum SelectionType {
    RECTANGLE("Rectangle") {
        @Override
        public Shape createShapeFromDrag(Drag drag, Shape oldShape) {
            // ignores oldShape, always creates a new rectangle from the drag
            return drag.createPositiveImRect();
        }

        @Override
        public Shape createShapeFromEvent(PMouseEvent e, Shape oldShape) {
            throw new UnsupportedOperationException("Rectangle selection uses Drag info");
        }
    }, ELLIPSE("Ellipse") {
        @Override
        public Shape createShapeFromDrag(Drag drag, Shape oldShape) {
            // ignores oldShape, always creates a new ellipse from the drag
            Rectangle2D r = drag.createPositiveImRect();
            return new Ellipse2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        }

        @Override
        public Shape createShapeFromEvent(PMouseEvent e, Shape oldShape) {
            throw new UnsupportedOperationException("Ellipse selection uses Drag info");
        }
    }, LASSO("Freehand") {
        @Override
        public Shape createShapeFromDrag(Drag drag, Shape oldShape) {
            if (oldShape instanceof Path2D path) {
                // extend the existing path
                path.lineTo(drag.getEndX(), drag.getEndY());
                return path;
            } else {
                // start a new path
                Path2D p = new Path2D.Double();
                p.moveTo(drag.getStartX(), drag.getStartY());
                p.lineTo(drag.getEndX(), drag.getEndY());
                return p;
            }
        }

        @Override
        public Shape createShapeFromEvent(PMouseEvent e, Shape oldShape) {
            throw new UnsupportedOperationException("Lasso selection uses Drag info");
        }
    }, POLYGONAL_LASSO("Polygonal") {
        @Override
        public Shape createShapeFromDrag(Drag drag, Shape oldShape) {
            throw new UnsupportedOperationException("Polygonal Lasso uses PMouseEvent info");
        }

        @Override
        public Shape createShapeFromEvent(PMouseEvent e, Shape oldShape) {
            if (oldShape instanceof Path2D path) {
                // extend the existing path
                path.lineTo(e.getImX(), e.getImY());
                return path;
            } else {
                // start a new path
                Path2D p = new Path2D.Double();
                p.moveTo(e.getImX(), e.getImY());
                // first point only defines the start, no line yet
                return p;
            }
        }
    }, MAGIC_WAND("Magic Wand") {
        @Override
        public Shape createShapeFromDrag(Drag drag, Shape oldShape) {
            throw new UnsupportedOperationException("Magic Wand uses PMouseEvent info");
        }

        @Override
        public Shape createShapeFromEvent(PMouseEvent e, Shape oldShape) {
            // ignores oldShape
            return MagicWandSelectionTool.createSelectionPath(e);
        }
    };

    private final String displayName;

    SelectionType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Creates or updates a selection shape based on drag input.
     * Some tools (like Marquee and Lasso) primarily provide drag
     * information (start/end points) encapsulated in a `Drag` object.
     */
    public abstract Shape createShapeFromDrag(Drag drag, Shape oldShape);

    /**
     * Creates or updates a selection shape based on mouse event input.
     * Some tools (like Polygonal Lasso and Magic Wand) primarily operate
     * based on individual mouse events.
     */
    public abstract Shape createShapeFromEvent(PMouseEvent e, Shape oldShape);

    @Override
    public String toString() {
        return displayName;
    }
}

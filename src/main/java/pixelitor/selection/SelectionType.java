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
        public Shape createShapeFromDrag(Drag drag, Shape currentShape) {
            // ignores currentShape, always creates a new rectangle from the drag
            return drag.toPosImRect();
        }

        @Override
        public Shape createShapeFromEvent(PMouseEvent e, Shape currentShape) {
            throw new UnsupportedOperationException("Rectangle uses Drag info");
        }
    }, ELLIPSE("Ellipse") {
        @Override
        public Shape createShapeFromDrag(Drag drag, Shape currentShape) {
            // ignores currentShape, always creates a new ellipse from the drag
            Rectangle2D r = drag.toPosImRect();
            return new Ellipse2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        }

        @Override
        public Shape createShapeFromEvent(PMouseEvent e, Shape currentShape) {
            throw new UnsupportedOperationException("Ellipse uses Drag info");
        }
    }, FREEHAND("Freehand") {
        @Override
        public Shape createShapeFromDrag(Drag drag, Shape currentShape) {
            if (currentShape instanceof Path2D path) {
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
        public Shape createShapeFromEvent(PMouseEvent e, Shape currentShape) {
            throw new UnsupportedOperationException("Freehand uses Drag info");
        }
    }, POLYGONAL("Polygonal") {
        @Override
        public Shape createShapeFromDrag(Drag drag, Shape currentShape) {
            throw new UnsupportedOperationException("Polygonal uses PMouseEvent info");
        }

        @Override
        public Shape createShapeFromEvent(PMouseEvent e, Shape currentShape) {
            if (currentShape instanceof Path2D path) {
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
        public Shape createShapeFromDrag(Drag drag, Shape currentShape) {
            throw new UnsupportedOperationException("Magic Wand uses PMouseEvent info");
        }

        @Override
        public Shape createShapeFromEvent(PMouseEvent e, Shape currentShape) {
            // ignores currentShape
            return MagicWandSelectionTool.createSelectionShape(e);
        }
    };

    private final String displayName;

    SelectionType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Creates or updates a selection shape based on drag input.
     * Some tools (like Marquee and Freehand) primarily provide drag
     * information (start/end points) encapsulated in a `Drag` object.
     */
    public abstract Shape createShapeFromDrag(Drag drag, Shape currentShape);

    /**
     * Creates or updates a selection shape based on mouse event input.
     * Some tools (like Polygonal and Magic Wand) primarily operate
     * based on individual mouse events.
     */
    public abstract Shape createShapeFromEvent(PMouseEvent e, Shape currentShape);

    @Override
    public String toString() {
        return displayName;
    }
}

/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.layers.Drawable;
import pixelitor.tools.Tools;
import pixelitor.tools.util.Drag;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Stack;

import static pixelitor.utils.ImageUtils.isGrayscale;

/**
 * The different ways a selection shape can be created or updated interactively.
 */
public enum SelectionType {
    RECTANGLE("Rectangle") {
        @Override
        public Shape createShapeFromDrag(Drag drag, Shape oldShape) {
            // ignores oldShape, always creates a new rectangle from the drag
            return drag.createPositiveImRect();
        }

        @Override
        public Shape createShapeFromEvent(PMouseEvent event, Shape oldShape) {
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
        public Shape createShapeFromEvent(PMouseEvent event, Shape oldShape) {
            throw new UnsupportedOperationException("Ellipse selection uses Drag info");
        }
    }, LASSO("Freehand") {
        @Override
        public Shape createShapeFromDrag(Drag drag, Shape oldShape) {
            if (oldShape instanceof GeneralPath gp) {
                // extend the existing path
                gp.lineTo(drag.getEndX(), drag.getEndY());
                return gp;
            } else {
                // start a new path
                GeneralPath p = new GeneralPath();
                p.moveTo(drag.getStartX(), drag.getStartY());
                p.lineTo(drag.getEndX(), drag.getEndY());
                return p;
            }
        }

        @Override
        public Shape createShapeFromEvent(PMouseEvent event, Shape oldShape) {
            throw new UnsupportedOperationException("Lasso selection uses Drag info");
        }
    }, POLYGONAL_LASSO("Polygonal") {
        @Override
        public Shape createShapeFromDrag(Drag drag, Shape oldShape) {
            throw new UnsupportedOperationException("Polygonal Lasso uses PMouseEvent info");
        }

        @Override
        public Shape createShapeFromEvent(PMouseEvent pe, Shape oldShape) {
            if (oldShape instanceof GeneralPath gp) {
                // extend the existing path
                gp.lineTo(pe.getImX(), pe.getImY());
                return gp;
            } else {
                // start a new path
                GeneralPath p = new GeneralPath();
                p.moveTo(pe.getImX(), pe.getImY());
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
        public Shape createShapeFromEvent(PMouseEvent pm, Shape oldShape) {
            // calculate the area selected by this specific click
            Area newlySelectedArea = selectPixelsInColorRange(pm); // Existing private method
            if (newlySelectedArea == null || newlySelectedArea.isEmpty()) {
                // if nothing new selected, return the old shape or null
                return oldShape;
            }

            if (oldShape instanceof Area area) {
                // add the newly selected area to the existing area
                area.add(newlySelectedArea);
                return area;
            } else {
                // this is the first click, return the newly selected area
                return newlySelectedArea;
            }
        }

        /**
         * Selects contiguous pixels within a color tolerance using a scanline fill algorithm.
         */
        private static Area selectPixelsInColorRange(PMouseEvent pm) {
            Area selectedArea = new Area();

            int imX = (int) pm.getImX();
            int imY = (int) pm.getImY();

            Composition comp = pm.getComp();
            Drawable dr = comp.getActiveDrawableOrThrow();
            BufferedImage image = dr.getImage();
            int imgHeight = image.getHeight();
            int imgWidth = image.getWidth();

            // adjust click coordinates to be relative to the drawable's image
            int x = imX - dr.getTx();
            int y = imY - dr.getTy();

            // check if the click is outside the drawable's bounds
            if (x < 0 || x >= imgWidth || y < 0 || y >= imgHeight) {
                return new Area();
            }

            int colorTolerance = Tools.MAGIC_WAND.getTolerance();

            boolean hasSelection = comp.hasSelection();
            boolean grayScale = isGrayscale(image);

            // TODO optionally we should use the composite image
            //   for color picking, as that's what the user sees
            BufferedImage workingImage;
            if (grayScale) {
                workingImage = ImageUtils.toSysCompatibleImage(image);
            } else if (hasSelection) {
                workingImage = ImageUtils.copyImage(image);
            } else {
                workingImage = image;
            }

            // tracks pixels already processed or added to the selection
            boolean [][] visited = new boolean[workingImage.getWidth()][workingImage.getHeight()];

            int targetColor = getColorAtPoint(new Point(x, y), pm);

            int finalX = x;
            int finalY = y;

            selectArea(workingImage, finalX, finalY, colorTolerance, targetColor, selectedArea, pm, visited, 1);
            selectArea(workingImage, finalX, finalY, colorTolerance, targetColor, selectedArea, pm, visited, -1);

            return selectedArea;
        }

        private static void selectArea(BufferedImage img, int x, int y, int tolerance,
                                       int rgbAtMouse, Area selectedArea, PMouseEvent pm,
                                       boolean[][] visited, int yOffset) {

            Stack<Point> pixelsToProcess = new Stack<>();
            pixelsToProcess.push(new Point(x, y));

            while (!pixelsToProcess.isEmpty()) {
                Point currentPixel = pixelsToProcess.pop();

                int startX = currentPixel.x;
                int startY = currentPixel.y;

                if (canBeSelected(currentPixel, img, rgbAtMouse, tolerance, visited, pm)) {

                    int leftX = walkDirection(currentPixel, img, rgbAtMouse, tolerance, visited, -1, pixelsToProcess, pm); // Walk left
                    int lineEndX = walkDirection(currentPixel, img, rgbAtMouse, tolerance, visited, 1, pixelsToProcess, pm); // Walk right

                    // Create an area based on the line segment
                    int lineStartX = leftX + 1;
                    int lineLength = lineEndX - lineStartX;

                    if (lineLength > 0) {
                        Area lineSegment = new Area(new Rectangle2D.Double(lineStartX, startY, lineLength, 1));
                        selectedArea.add(lineSegment);
                    }

                    pixelsToProcess.push(new Point(startX, startY + yOffset));
                }
            }
        }

        private static int walkDirection(Point currentPixel, BufferedImage img, int rgbAtMouse, int tolerance, boolean[][] visited,
                                         int xStep, Stack<Point> pixelsToProcess, PMouseEvent pm) {

            int startX = currentPixel.x;
            int startY = currentPixel.y;
            int currentX = startX + xStep;

            while (canBeSelected(new Point(currentX, startY), img, rgbAtMouse, tolerance, visited, pm)) {
                visited[currentX][startY] = true;

                Point pointAbove = new Point(currentX, startY - 1);
                boolean canBeSelectedAbove = canBeSelected(pointAbove, img, rgbAtMouse, tolerance, visited, pm);

                Point pointBelow = new Point(currentX, startY + 1);
                boolean canBeSelectedBelow = canBeSelected(pointBelow, img, rgbAtMouse, tolerance, visited, pm);

                selectPixelIfPossible(canBeSelectedAbove, pixelsToProcess, pointAbove);
                selectPixelIfPossible(canBeSelectedBelow, pixelsToProcess, pointBelow);

                currentX += xStep;
            }
            return currentX;
        }

        private static void selectPixelIfPossible(boolean canBeSelected, Stack<Point> pixelsToProcess, Point pointToSelect) {
            if (canBeSelected) {
                pixelsToProcess.push(pointToSelect);
            }
        }

        private static boolean canBeSelected(Point currentPixel, BufferedImage img,
                                             int rgbAtMouse, int tolerance, boolean[][] visited, PMouseEvent pm) {
            int imgHeight = img.getHeight();
            int imgWidth = img.getWidth();

            int currentX = currentPixel.x;
            int currentY = currentPixel.y;

            int targetColor = getColorAtPoint(currentPixel, pm);

            return currentX >= 0 && currentX < imgWidth && currentY >= 0 && currentY < imgHeight &&
                ImageUtils.isSimilar(rgbAtMouse, targetColor, tolerance) &&
                !visited[currentX][currentY];
        }

        private static int getColorAtPoint(Point p, PMouseEvent pm) {
            int x = p.x;
            int y = p.y;

            View view = pm.getView();
            BufferedImage img;
            Composition comp = view.getComp();
            img = comp.getCompositeImage();

            if (x >= 0 && x < img.getWidth() && y >= 0 && y < img.getHeight()) {
                return img.getRGB(x, y);
            } else {
                return Color.BLACK.getRGB();
            }
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
    public abstract Shape createShapeFromEvent(PMouseEvent event, Shape oldShape);

    @Override
    public String toString() {
        return displayName;
    }
}

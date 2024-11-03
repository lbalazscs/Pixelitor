/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.tools.SelectionTool;
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

/**
 * The type of a new selection created interactively by the user.
 * Corresponds to the "Type" combo box in the Selection Tool.
 */
public enum SelectionType {
    RECTANGLE("Rectangle", true) {
        @Override
        public Shape createShape(Object mouseInfo, Shape oldShape) {
            Drag drag = (Drag) mouseInfo;
            return drag.createPositiveImRect();
        }
    }, ELLIPSE("Ellipse", true) {
        @Override
        public Shape createShape(Object mouseInfo, Shape oldShape) {
            Drag drag = (Drag) mouseInfo;
            Rectangle2D dr = drag.createPositiveImRect();
            return new Ellipse2D.Double(dr.getX(), dr.getY(), dr.getWidth(), dr.getHeight());
        }
    }, LASSO("Freehand", false) {
        @Override
        public Shape createShape(Object mouseInfo, Shape oldShape) {
            Drag drag = (Drag) mouseInfo;

            if (createNewShape(oldShape)) {
                GeneralPath p = new GeneralPath();
                p.moveTo(drag.getStartX(), drag.getStartY());
                p.lineTo(drag.getEndX(), drag.getEndY());
                return p;
            } else {
                GeneralPath gp = (GeneralPath) oldShape;
                gp.lineTo(drag.getEndX(), drag.getEndY());

                return gp;
            }
        }
    }, POLYGONAL_LASSO("Polygonal", false) {
        @Override
        public Shape createShape(Object mouseInfo, Shape oldShape) {
            PMouseEvent pe = (PMouseEvent) mouseInfo;

            if (createNewShape(oldShape)) {
                GeneralPath p = new GeneralPath();
                p.moveTo(pe.getImX(), pe.getImY());
                return p;
            } else {
                GeneralPath gp = (GeneralPath) oldShape;
                gp.lineTo(pe.getImX(), pe.getImY());

                return gp;
            }
        }
    }, SELECTION_MAGIC_WAND("MagicWand", true){
        @Override
        public Shape createShape(Object mouseInfo, Shape oldShape) {
            PMouseEvent pm = (PMouseEvent) mouseInfo;
            Area newShape = selectPixelsInColorRange(pm);

            if(createNewShape(oldShape)){
                return newShape;
            } else {
                Area unitedArea = new Area(oldShape);
                unitedArea.add(newShape);
                return unitedArea;
            }
        }

        private Area selectPixelsInColorRange(PMouseEvent pm) {
            Area selectedArea = new Area();

            int x = (int) pm.getImX();
            int y = (int) pm.getImY();

            var comp = pm.getComp();
            Drawable dr = comp.getActiveDrawableOrThrow();

            int tx = dr.getTx();
            int ty = dr.getTy();

            x -= tx;
            y -= ty;

            int colorTolerance = SelectionTool.getTolerance();

            BufferedImage image = dr.getImage();
            int imgHeight = image.getHeight();
            int imgWidth = image.getWidth();
            if (x < 0 || x >= imgWidth || y < 0 || y >= imgHeight) {
                return null;
            }

            boolean thereIsSelection = comp.hasSelection();
            boolean grayScale = image.getType() == BufferedImage.TYPE_BYTE_GRAY;

            BufferedImage workingImage;
            if (grayScale) {
                workingImage = ImageUtils.toSysCompatibleImage(image);
            } else if (thereIsSelection) {
                workingImage = ImageUtils.copyImage(image);
            } else {
                workingImage = image;
            }

            boolean [][] visited = new boolean[workingImage.getWidth()][workingImage.getHeight()];
            int targetColor = getColorAtEvent(new Point(x, y), pm);

            int finalX = x;
            int finalY = y;

            selectArea(workingImage, finalX, finalY, colorTolerance, targetColor, selectedArea, pm, visited, 1);
            selectArea(workingImage, finalX, finalY, colorTolerance, targetColor, selectedArea, pm, visited, -1);

            return selectedArea;
        }

        private void selectArea(BufferedImage img, int x, int y, int tolerance,
                                int rgbAtMouse, Area selectedArea, PMouseEvent pm,
                                boolean[][] visited, int yOffset) {

            Stack<Point> pixelsToProcess = new Stack<>();
            pixelsToProcess.push(new Point(x, y));

            while (!pixelsToProcess.isEmpty()) {
                Point currentPixel = pixelsToProcess.pop();
                System.out.println("working");

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

        private int walkDirection(Point currentPixel, BufferedImage img, int rgbAtMouse, int tolerance, boolean[][] visited,
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

        private void selectPixelIfPossible(boolean canBeSelected, Stack<Point> pixelsToProcess, Point pointToSelect) {
            if (canBeSelected) {
                pixelsToProcess.push(pointToSelect);
            }
        }

        private boolean canBeSelected(Point currentPixel, BufferedImage img,
                                      int rgbAtMouse, int tolerance, boolean[][] visited, PMouseEvent pm) {
            int imgHeight = img.getHeight();
            int imgWidth = img.getWidth();

            int currentX = currentPixel.x;
            int currentY = currentPixel.y;

            int targetColor = getColorAtEvent(currentPixel, pm);

            return currentX >= 0 && currentX < imgWidth && currentY >= 0 && currentY < imgHeight &&
                colorWithinTolerance(new Color(rgbAtMouse), new Color(targetColor), tolerance) &&
                !visited[currentX][currentY];
        }

        private int getColorAtEvent(Point p, PMouseEvent pm) {
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

        private boolean colorWithinTolerance(Color c1, Color c2, int tolerance) {
            return Math.abs(c1.getRed() - c2.getRed()) <= tolerance &&
                    Math.abs(c1.getGreen() - c2.getGreen()) <= tolerance &&
                    Math.abs(c1.getBlue() - c2.getBlue()) <= tolerance;
        }
    };

    private final String displayName;

    // whether this selection type should display width and height info
    private final boolean displayWH;

    SelectionType(String displayName, boolean displayWH) {
        this.displayName = displayName;
        this.displayWH = displayWH;
    }

    public abstract Shape createShape(Object mouseInfo, Shape oldShape);

    public boolean displayWidthHeight() {
        return displayWH;
    }

    private static boolean createNewShape(Shape oldShape) {
        boolean createNew;
        if (oldShape == null) {
            createNew = true;
        } else if (oldShape instanceof GeneralPath) {
            createNew = false;
        } else { // it is an Area, meaning that a new shape has been started
            createNew = true;
        }
        return createNew;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.tools.PaintBucketTool;
import pixelitor.tools.util.Drag;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.ImageUtils;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.ParsePosition;

import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;
import static pixelitor.colors.FgBgColors.setBGColor;
import static pixelitor.colors.FgBgColors.setFGColor;

import java.util.*;

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
        /*public Shape createShape(Object mouseInfo, Shape oldShape) {
            Drag drag = (Drag) mouseInfo;

            if (createNewShape(oldShape)) {
                /*GeneralPath p = new GeneralPath();
                p.moveTo(drag.getStartX(), drag.getStartY());
                p.lineTo(drag.getEndX(), drag.getEndY());
                return p;
                Area magicWandArea = new Area(new Rectangle2D.Double(drag.getStartX(), drag.getStartY(), 1, 1));

                // Agregar a la región todos los píxeles que la varita mágica seleccionaría
                // (puedes ajustar el rango según sea necesario)
                for (int x = (int) drag.getStartX() - 1; x <= drag.getStartX() + 10; x++) {
                    for (int y = (int) drag.getStartY() - 1; y <= drag.getStartY() + 10; y++) {
                        magicWandArea.add(new Area(new Rectangle2D.Double(x, y, 1, 1)));
                    }
                }

                return magicWandArea;
            } else {
                GeneralPath gp = (GeneralPath) oldShape;
                gp.lineTo(drag.getEndX(), drag.getEndY());

                return gp;
            }
        }*/

        public Shape createShape(Object mouseInfo, Shape oldShape) {
            /*Drag drag = (Drag) mouseInfo;
            int rgb = getColorAt((int) drag.getStartX(), (int) drag.getStartY(), mouseInfo);

            if (createNewShape(oldShape)) {
                // Agregar a la región todos los píxeles que la varita mágica seleccionaría
                return selectPixelsInColorRange(drag.getStartX(), drag.getStartY(), new Color(rgb), (Object) mouseInfo);
            } else {
                GeneralPath gp = (GeneralPath) oldShape;
                gp.lineTo(drag.getEndX(), drag.getEndY());

                return gp;
            }*/

            if (mouseInfo instanceof PMouseEvent pm) {
                Area newShape = selectPixelsInColorRange(pm);

                if(createNewShape(oldShape)){
                    return newShape;
                } else {
                    Area unitedArea = new Area(oldShape);
                    unitedArea.add(newShape);
                    return unitedArea;
                }
            } else if (mouseInfo instanceof Drag) {
                if(createNewShape(oldShape)){
                    //a
                } else {
                    //a
                }
            }
            return oldShape;
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

            int colorTolerance = 30;

            BufferedImage image = dr.getImage();
            int imgHeight = image.getHeight();
            int imgWidth = image.getWidth();
            if (x < 0 || x >= imgWidth || y < 0 || y >= imgHeight) {
                return selectedArea;
            }

            BufferedImage backupForUndo = ImageUtils.copyImage(image);
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

            int rgbAtMouse = workingImage.getRGB(x, y);

            /*Area selectedArea1 = new Area();
            Area selectedArea2 = new Area();
            Area selectedArea3 = new Area();
            Area selectedArea4 = new Area();

            int x = (int) pm.getImX();
            int y = (int) pm.getImY();

            int colorTolerance = 30;
            int targetColor = getColorAtEvent(x, y, pm);*/

            /***for (int i = 0; i <= 70; i++) {
                for (int j = 0; j <= 70; j++) {
                    int pixelColor = getColorAtEvent(x + i, y + j, pm); // Método para obtener el color del píxel en la posición dada

                    // Compara el color con la tolerancia
                    if (colorWithinTolerance(new Color(pixelColor), new Color(targetColor), colorTolerance)) {
                        selectedArea.add(new Area(new Rectangle2D.Double(x + i, y + j, 1, 1)));
                    }
                }
            }*/

            /*
            XYSelector(pm, selectedArea1, colorTolerance, targetColor, x, y);
            nXYSelector(pm, selectedArea2, colorTolerance, targetColor, x, y);
            XnYSelector(pm, selectedArea3, colorTolerance, targetColor, x, y);
            nXnYSelector(pm, selectedArea4, colorTolerance, targetColor, x, y);

            selectedArea.add(selectedArea1);
            selectedArea.add(selectedArea2);
            selectedArea.add(selectedArea3);
            selectedArea.add(selectedArea4);
            */

            //return selectedArea;

            //Area newArea = scanlineFloodSelector(workingImage, x, y, colorTolerance, rgbAtMouse/*, 30*/);

            boolean [][] visited = new boolean[workingImage.getWidth()][workingImage.getHeight()];

            /*try {
                newArea(workingImage, x, y, colorTolerance, targetColor, targetColor, selectedArea, pm, visited);
            } catch (StackOverflowError st) {
                st.getMessage();
            }*/

            int targetColor = getColorAtEvent(x, y, pm);

            selectedArea(workingImage, x, y, colorTolerance, targetColor, selectedArea, pm, visited);

            return selectedArea;
        }

        private void selectedArea(BufferedImage img, int x, int y, int tolerance,
                                  int rgbAtMouse, Area selectedArea,
                                  PMouseEvent pm, boolean[][] visited) {
            int imgHeight = img.getHeight();
            int imgWidth = img.getWidth();

            ArrayList<Area> areasToProcess = new ArrayList<>();
            areasToProcess.add(new Area(new Rectangle2D.Double(x, y, 1, 1)));

            while (!areasToProcess.isEmpty()) {
                Area currentArea = areasToProcess.get(0);
                areasToProcess.remove(0);
                Rectangle2D bounds = currentArea.getBounds2D();
                int currentX = (int) bounds.getX();
                int currentY = (int) bounds.getY();

                int targetColor = getColorAtEvent(currentX, currentY, pm);

                if (currentX >= 0 && currentX < imgWidth && currentY >= 0 && currentY < imgHeight &&
                        colorWithinTolerance(new Color(rgbAtMouse), new Color(targetColor), tolerance) &&
                        !visited[currentX][currentY]) {

                    System.out.println("Se puede bro");
                    System.out.println("coords: " + currentX + " " + currentY);

                    selectedArea.add(currentArea);
                    visited[currentX][currentY] = true;

                    areasToProcess.add(new Area(new Rectangle2D.Double(currentX + 1, currentY, 1, 1)));
                    areasToProcess.add(new Area(new Rectangle2D.Double(currentX - 1, currentY, 1, 1)));
                    areasToProcess.add(new Area(new Rectangle2D.Double(currentX, currentY + 1, 1, 1)));
                    areasToProcess.add(new Area(new Rectangle2D.Double(currentX, currentY - 1, 1, 1)));
                } else {
                    System.out.println("no se puede bro");
                    System.out.println(areasToProcess.size());
                }
            }
        }


        /*private void selectedArea(BufferedImage img, int x, int y, int tolerance,
                                  int rgbAtMouse, Area selectedArea,
                                  PMouseEvent pm, boolean[][] visited) {
            int imgHeight = img.getHeight();
            int imgWidth = img.getWidth();

            Deque<Area> stack = new ArrayDeque<>();
            Area newArea = new Area(new Rectangle2D.Double(x, y, 1, 1));
            stack.push(newArea);

            while (!stack.isEmpty()) {
                Area a = stack.pop();

                int currentX = (int) a.getBounds2D().getX();
                int currentY = (int) a.getBounds2D().getY();

                int targetColor = getColorAtEvent(x, y, pm);

                if (currentX < 0 || currentX >= imgWidth || currentY < 0 || currentY >= imgHeight || !colorWithinTolerance(new Color(rgbAtMouse), new Color(targetColor), tolerance) || visited[currentX][currentY]) {
                    System.out.println("no se puede bro");
                    System.out.println(stack.size());
                    return;
                } else {
                    System.out.println("Se puede bro");

                    System.out.println("coords: " + currentX + " " + currentY);
                    newArea = new Area(new Rectangle2D.Double(currentX, currentY, 1, 1));

                    selectedArea.add(newArea);
                    visited[currentX][currentY] = true;

                    stack.push(new Area(new Rectangle2D.Double(currentX + 1, currentY, 1, 1)));
                    stack.push(new Area(new Rectangle2D.Double(currentX - 1, currentY, 1, 1)));
                    stack.push(new Area(new Rectangle2D.Double(currentX, currentY + 1, 1, 1)));
                    stack.push(new Area(new Rectangle2D.Double(currentX, currentY - 1, 1, 1)));

                    // newArea(img, x + 1, y, tolerance, getColorAtEvent(x + 1, y, pm), targetColor, selectedArea, pm, visited);
                    // newArea(img, x - 1, y, tolerance, getColorAtEvent(x - 1, y, pm), targetColor, selectedArea, pm, visited);
                    // newArea(img, x, y + 1, tolerance, getColorAtEvent(x, y + 1, pm), targetColor, selectedArea, pm, visited);
                    // newArea(img, x, y - 1, tolerance, getColorAtEvent(x, y - 1, pm), targetColor, selectedArea, pm, visited);
                }
            }

        }*/

        private void newArea(BufferedImage img, int x, int y, int tolerance,
                             int rgbAtMouse, int targetColor, Area selectedArea,
                             PMouseEvent pm, boolean[][] visited) {

            int imgHeight = img.getHeight();
            int imgWidth = img.getWidth();

            if (x < 0 || x >= imgWidth || y < 0 || y >= imgHeight || !colorWithinTolerance(new Color(rgbAtMouse), new Color(targetColor), tolerance) || visited[x][y]) {
                return;
            } else {
                selectedArea.add(new Area(new Rectangle2D.Double(x, y, 1, 1)));
                visited[x][y] = true;

                System.out.println("a");

                newArea(img, x + 1, y, tolerance, getColorAtEvent(x + 1, y, pm), targetColor, selectedArea, pm, visited);
                newArea(img, x - 1, y, tolerance, getColorAtEvent(x - 1, y, pm), targetColor, selectedArea, pm, visited);
                newArea(img, x, y + 1, tolerance, getColorAtEvent(x, y + 1, pm), targetColor, selectedArea, pm, visited);
                newArea(img, x, y - 1, tolerance, getColorAtEvent(x, y - 1, pm), targetColor, selectedArea, pm, visited);
            }

        }

        private static Area scanlineFloodSelector(BufferedImage img,
                                                   int x, int y, int tolerance,
                                                   int rgbAtMouse/*, int newRGB*/) {
            Area newArea = new Area();

            int minX = x;
            int maxX = x;
            int minY = y;
            int maxY = y;
            int imgHeight = img.getHeight();
            int imgWidth = img.getWidth();

            int[] pixels = ImageUtils.getPixelArray(img);

            // Needed because the tolerance: we cannot assume that
            // if the pixel is within the target range, it has been processed
            boolean[] checkedPixels = new boolean[pixels.length];

            // the double-ended queue is used as a simple LIFO stack
            Deque<Area> stack = new ArrayDeque<>();
            stack.push(new Area(new Rectangle2D.Double(x, y, 1, 1)));

            while (!stack.isEmpty()) {
                Area a = stack.pop();

                x = (int) a.getBounds2D().getX();
                y = (int) a.getBounds2D().getY();

                // find the last replaceable point to the left
                int scanlineMinX = x - 1;
                int offset = y * imgWidth;
                while (scanlineMinX >= 0
                        && isSimilar(pixels[scanlineMinX + offset], rgbAtMouse, tolerance)) {
                    scanlineMinX--;
                }
                scanlineMinX++;

                // find the last replaceable point to the right
                int scanlineMaxX = x + 1;
                while (scanlineMaxX < img.getWidth()
                        && isSimilar(pixels[scanlineMaxX + offset], rgbAtMouse, tolerance)) {
                    scanlineMaxX++;
                }
                scanlineMaxX--;

                // set the minX, maxX, minY, maxY variables
                // that will be used to calculate the affected area
                if (scanlineMinX < minX) {
                    minX = scanlineMinX;
                }
                if (scanlineMaxX > maxX) {
                    maxX = scanlineMaxX;
                }
                if (y > maxY) {
                    maxY = y;
                } else if (y < minY) {
                    minY = y;
                }

                // draw a line between (scanlineMinX, y) and (scanlineMaxX, y)
                for (int i = scanlineMinX; i <= scanlineMaxX; i++) {
                    int index = i + offset;
                    pixels[index] = 0x00_00_00_00;
                    System.out.println(pixels);
                    checkedPixels[index] = true;
                }

                // look upwards for new points to be inspected later
                if (y > 0) {
                    // if there are multiple pixels to be replaced
                    // that are horizontal neighbours,
                    // only one of them has to be inspected later
                    boolean pointsInLine = false;

                    int upOffset = (y - 1) * imgWidth;

                    for (int i = scanlineMinX; i <= scanlineMaxX; i++) {
                        int upIndex = i + upOffset;
                        boolean shouldBeReplaced = !checkedPixels[upIndex]
                                && isSimilar(pixels[upIndex], rgbAtMouse, tolerance);

                        if (!pointsInLine && shouldBeReplaced) {
                            //Point inspectLater = new Point(i, y - 1);
                            Area inspectLater = new Area(new Rectangle2D.Double(i, y - 1, 1, 1));
                            System.out.println("max: " + scanlineMinX);
                            System.out.println("min: " + scanlineMaxX);
                            System.out.println(i);
                            newArea.add(inspectLater);
                            stack.push(inspectLater);
                            pointsInLine = true;
                        } else if (pointsInLine && !shouldBeReplaced) {
                            pointsInLine = false;
                        }
                    }
                }

                // look downwards for new points to be inspected later
                if (y < imgHeight - 1) {
                    boolean pointsInLine = false;
                    int downOffset = (y + 1) * imgWidth;

                    for (int i = scanlineMinX; i <= scanlineMaxX; i++) {
                        int downIndex = i + downOffset;
                        boolean shouldBeReplaced = !checkedPixels[downIndex]
                                && isSimilar(pixels[downIndex], rgbAtMouse, tolerance);

                        if (!pointsInLine && shouldBeReplaced) {
                            //Point inspectLater = new Point(i, y + 1);
                            Area inspectLater = new Area(new Rectangle2D.Double(i, y + 1, 1, 1));
                            newArea.add(inspectLater);
                            stack.push(inspectLater);
                            pointsInLine = true;
                        } else if (pointsInLine && !shouldBeReplaced) {
                            pointsInLine = false;
                        }
                    }
                }

            }

            // return the affected area
            //return new Area(new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1));
            return newArea;
        }

        private static boolean isSimilar(int color1, int color2, int tolerance) {
            if (color1 == color2) {
                return true;
            }

            int a1 = (color1 >>> 24) & 0xFF;
            int r1 = (color1 >>> 16) & 0xFF;
            int g1 = (color1 >>> 8) & 0xFF;
            int b1 = color1 & 0xFF;

            int a2 = (color2 >>> 24) & 0xFF;
            int r2 = (color2 >>> 16) & 0xFF;
            int g2 = (color2 >>> 8) & 0xFF;
            int b2 = color2 & 0xFF;

            return (r2 <= r1 + tolerance) && (r2 >= r1 - tolerance) &&
                    (g2 <= g1 + tolerance) && (g2 >= g1 - tolerance) &&
                    (b2 <= b1 + tolerance) && (b2 >= b1 - tolerance) &&
                    (a2 <= a1 + tolerance) && (a2 >= a1 - tolerance);
        }

        private void magicWandSelectorComparator(
                PMouseEvent pm, Area selectedArea, int colorTolerance,
                int targetColor, int x, int y, int i, int j
        ) {
            int pixelColor = getColorAtEvent(x + i, y + j, pm); // Método para obtener el color del píxel en la posición dada

            // Compara el color con la tolerancia
            if (colorWithinTolerance(new Color(pixelColor), new Color(targetColor), colorTolerance)) {
                selectedArea.add(new Area(new Rectangle2D.Double(x + i, y + j, 1, 1)));
            }
        }

        private void XYSelector(
                PMouseEvent pm, Area selectedArea, int colorTolerance,
                int targetColor, int x, int y
        ) {
            for (int i = 0; i <= 70; i++) {
                outerLoop:
                for (int j = 0; j <= 70; j++) {
                    //magicWandSelectorComparator(pm, selectedArea, colorTolerance, targetColor, x, y, i, j);

                    int pixelColor = getColorAtEvent(x + i, y + j, pm); // Método para obtener el color del píxel en la posición dada

                    // Compara el color con la tolerancia
                    if (colorWithinTolerance(new Color(pixelColor), new Color(targetColor), colorTolerance)) {
                        selectedArea.add(new Area(new Rectangle2D.Double(x + i, y + j, 1, 1)));
                    } else {
                        break outerLoop;
                    }
                }
            }

            Area selectedArea2 = new Area();

            for (int j = 0; j <= 70; j++) {
                outerLoop:
                for (int i = 0; i <= 70; i++) {
                    //magicWandSelectorComparator(pm, selectedArea, colorTolerance, targetColor, x, y, i, j);

                    int pixelColor = getColorAtEvent(x + i, y + j, pm); // Método para obtener el color del píxel en la posición dada

                    // Compara el color con la tolerancia
                    if (colorWithinTolerance(new Color(pixelColor), new Color(targetColor), colorTolerance)) {
                        selectedArea2.add(new Area(new Rectangle2D.Double(x + i, y + j, 1, 1)));
                    } else {
                        break outerLoop;
                    }
                }
            }

            selectedArea.intersect(selectedArea2);
        }

        private void nXYSelector(
                PMouseEvent pm, Area selectedArea, int colorTolerance,
                int targetColor, int x, int y
        ) {
            for (int i = 0; i >= -70; i--) {
                outerLoop:
                for (int j = 0; j <= 70; j++) {
                    //magicWandSelectorComparator(pm, selectedArea, colorTolerance, targetColor, x, y, i, j);

                    int pixelColor = getColorAtEvent(x + i, y + j, pm); // Método para obtener el color del píxel en la posición dada

                    // Compara el color con la tolerancia
                    if (colorWithinTolerance(new Color(pixelColor), new Color(targetColor), colorTolerance)) {
                        selectedArea.add(new Area(new Rectangle2D.Double(x + i, y + j, 1, 1)));
                    } else {
                        break outerLoop;
                    }
                }
            }

            Area selectedArea2 = new Area();

            for (int j = 0; j <= 70; j++) {
                outerLoop:
                for (int i = 0; i >= -70; i--) {
                    //magicWandSelectorComparator(pm, selectedArea, colorTolerance, targetColor, x, y, i, j);

                    int pixelColor = getColorAtEvent(x + i, y + j, pm); // Método para obtener el color del píxel en la posición dada

                    // Compara el color con la tolerancia
                    if (colorWithinTolerance(new Color(pixelColor), new Color(targetColor), colorTolerance)) {
                        selectedArea2.add(new Area(new Rectangle2D.Double(x + i, y + j, 1, 1)));
                    } else {
                        break outerLoop;
                    }
                }
            }

            selectedArea.intersect(selectedArea2);
        }

        private void XnYSelector(
                PMouseEvent pm, Area selectedArea, int colorTolerance,
                int targetColor, int x, int y
        ) {
            for (int i = 0; i <= 70; i++) {
                outerLoop:
                for (int j = 0; j >= -70; j--) {
                    //magicWandSelectorComparator(pm, selectedArea, colorTolerance, targetColor, x, y, i, j);

                    int pixelColor = getColorAtEvent(x + i, y + j, pm); // Método para obtener el color del píxel en la posición dada

                    // Compara el color con la tolerancia
                    if (colorWithinTolerance(new Color(pixelColor), new Color(targetColor), colorTolerance)) {
                        selectedArea.add(new Area(new Rectangle2D.Double(x + i, y + j, 1, 1)));
                    } else {
                        break outerLoop;
                    }
                }
            }

            Area selectedArea2 = new Area();

            for (int j = 0; j >= -70; j--) {
                outerLoop:
                for (int i = 0; i <= 70; i++) {
                    //magicWandSelectorComparator(pm, selectedArea, colorTolerance, targetColor, x, y, i, j);

                    int pixelColor = getColorAtEvent(x + i, y + j, pm); // Método para obtener el color del píxel en la posición dada

                    // Compara el color con la tolerancia
                    if (colorWithinTolerance(new Color(pixelColor), new Color(targetColor), colorTolerance)) {
                        selectedArea2.add(new Area(new Rectangle2D.Double(x + i, y + j, 1, 1)));
                    } else {
                        break outerLoop;
                    }
                }
            }

            selectedArea.intersect(selectedArea2);
        }

        private void nXnYSelector(
                PMouseEvent pm, Area selectedArea, int colorTolerance,
                int targetColor, int x, int y
        ) {
            for (int i = 0; i >= -70; i--) {
                outerLoop:
                for (int j = 0; j >= -70; j--) {
                    //magicWandSelectorComparator(pm, selectedArea, colorTolerance, targetColor, x, y, i, j);

                    int pixelColor = getColorAtEvent(x + i, y + j, pm); // Método para obtener el color del píxel en la posición dada

                    // Compara el color con la tolerancia
                    if (colorWithinTolerance(new Color(pixelColor), new Color(targetColor), colorTolerance)) {
                        selectedArea.add(new Area(new Rectangle2D.Double(x + i, y + j, 1, 1)));
                    } else {
                        break outerLoop;
                    }
                }
            }

            Area selectedArea2 = new Area();

            for (int j = 0; j >= -70; j--) {
                outerLoop:
                for (int i = 0; i >= -70; i--) {
                    //magicWandSelectorComparator(pm, selectedArea, colorTolerance, targetColor, x, y, i, j);

                    int pixelColor = getColorAtEvent(x + i, y + j, pm); // Método para obtener el color del píxel en la posición dada

                    // Compara el color con la tolerancia
                    if (colorWithinTolerance(new Color(pixelColor), new Color(targetColor), colorTolerance)) {
                        selectedArea2.add(new Area(new Rectangle2D.Double(x + i, y + j, 1, 1)));
                    } else {
                        break outerLoop;
                    }
                }
            }

            selectedArea.intersect(selectedArea2);
        }

        private int getColorAtEvent(int x, int y, PMouseEvent pm) {
            View view = pm.getView();
            BufferedImage img;
            boolean isGray = false;
            Composition comp = view.getComp();
            img = comp.getCompositeImage();

            /*if (x < img.getWidth() && y < img.getHeight() && x >= 0 && y >= 0) {
                return img.getRGB(x, y);

                //Color sampledColor = new Color(rgb);
            }*/

            return img.getRGB(x, y);
        }

        /*private Area selectPixelsInColorRange(double x, double y, Color targetColor, Object obj) {
            Area selectedArea = new Area();

            // Especifica la tolerancia de color (ajusta según sea necesario)
            int colorTolerance = 30;

            // Selecciona píxeles en un área alrededor de la posición del mouse
            for (int i = -10; i <= 10; i++) {
                for (int j = -10; j <= 10; j++) {
                    int pixelColor = getColorAt((int) x + i, (int) y + j, (Drag) obj); // Método para obtener el color del píxel en la posición dada

                    // Compara el color con la tolerancia
                    if (colorWithinTolerance(new Color(pixelColor), targetColor, colorTolerance)) {
                        selectedArea.add(new Area(new Rectangle2D.Double(x + i, y + j, 1, 1)));
                    }
                }
            }

            return selectedArea;
        }

        private int getColorAt(int x, int y, Object mouseInfo) {
            BufferedImage img;

            Drag drag = (Drag) mouseInfo;
            PMouseEvent pm = (PMouseEvent) drag;

            boolean isGray = false;

            View view = pm.getView();
            Composition comp = view.getComp();
            img = comp.getCompositeImage();

            isGray = img.getType() == TYPE_BYTE_GRAY;

            if (x < img.getWidth() && y < img.getHeight() && x >= 0 && y >= 0) {
                return img.getRGB(x, y);
            }

            return 0; // Devuelve un valor ficticio para este ejemplo, debes reemplazarlo con la lógica real.
        }*/

        private boolean colorWithinTolerance(Color c1, Color c2, int tolerance) {
            // Comprueba si el color c1 está dentro de la tolerancia de color de c2
            return Math.abs(c1.getRed() - c2.getRed()) <= tolerance &&
                    Math.abs(c1.getGreen() - c2.getGreen()) <= tolerance &&
                    Math.abs(c1.getBlue() - c2.getBlue()) <= tolerance;
        }
    };

    private final String guiName;

    // whether this selection type should display width and height info
    private final boolean displayWH;

    SelectionType(String guiName, boolean displayWH) {
        this.guiName = guiName;
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
        return guiName;
    }
}

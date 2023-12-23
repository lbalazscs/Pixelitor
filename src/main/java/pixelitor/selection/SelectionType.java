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
import pixelitor.tools.SelectionTool;
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
        @Override
        public Shape createShape(Object mouseInfo, Shape oldShape) {

            if (mouseInfo instanceof PMouseEvent pm) {
                Area newShape = selectPixelsInColorRange(pm);

                if(createNewShape(oldShape)){
                    return newShape;
                } else {
                    Area unitedArea = new Area(oldShape);
                    unitedArea.add(newShape);
                    return unitedArea;
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

            int colorTolerance = SelectionTool.getTolerance();

            BufferedImage image = dr.getImage();
            int imgHeight = image.getHeight();
            int imgWidth = image.getWidth();
            if (x < 0 || x >= imgWidth || y < 0 || y >= imgHeight) {
                return null;
            }

            //BufferedImage backupForUndo = ImageUtils.copyImage(image);
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

            boolean [][] visited = new boolean[workingImage.getWidth()][workingImage.getHeight()];
            int targetColor = getColorAtEvent(x, y, pm);
            System.out.println(x + " " + y + " " + targetColor);

            selectedArea(workingImage, x, y, colorTolerance, targetColor, selectedArea, pm, visited);

            return selectedArea;
        }

        private void selectedArea(BufferedImage img, int x, int y, int tolerance,
                                  int rgbAtMouse, Area selectedArea,
                                  PMouseEvent pm, boolean[][] visited) {

            int imgHeight = img.getHeight();
            int imgWidth = img.getWidth();

            LinkedList<Area> areasToProcess = new LinkedList<>();
            areasToProcess.add(new Area(new Rectangle2D.Double(x, y, 1, 1)));

            while (!areasToProcess.isEmpty()) {

                    Area currentArea = areasToProcess.get(0);
                    areasToProcess.remove(0);
                    Rectangle2D bounds = currentArea.getBounds2D();
                    int currentX = (int) bounds.getX();
                    int currentY = (int) bounds.getY();

                    System.out.println(currentX + " " + currentY);

                    int targetColor = getColorAtEvent(currentX, currentY, pm);

                    if (currentX > 0 && currentX < imgWidth && currentY > 0 && currentY < imgHeight &&
                            colorWithinTolerance(new Color(rgbAtMouse), new Color(targetColor), tolerance) &&
                            !visited[currentX][currentY]) {

                        System.out.println(imgHeight);

                        selectedArea.add(currentArea);
                        visited[currentX][currentY] = true;

                        areasToProcess.add(new Area(new Rectangle2D.Double(currentX + 1, currentY, 1, 1)));
                        areasToProcess.add(new Area(new Rectangle2D.Double(currentX - 1, currentY, 1, 1)));
                        areasToProcess.add(new Area(new Rectangle2D.Double(currentX, currentY + 1, 1, 1)));
                        areasToProcess.add(new Area(new Rectangle2D.Double(currentX, currentY - 1, 1, 1)));
                    }

            }
        }

        private int getColorAtEvent(int x, int y, PMouseEvent pm) {
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

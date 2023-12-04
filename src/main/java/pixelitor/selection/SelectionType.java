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
import pixelitor.tools.util.Drag;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PPoint;

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

            int colorTolerance = 30;
            int targetColor = getColorAtEvent(x, y, pm);

            /***for (int i = 0; i <= 70; i++) {
                for (int j = 0; j <= 70; j++) {
                    int pixelColor = getColorAtEvent(x + i, y + j, pm); // Método para obtener el color del píxel en la posición dada

                    // Compara el color con la tolerancia
                    if (colorWithinTolerance(new Color(pixelColor), new Color(targetColor), colorTolerance)) {
                        selectedArea.add(new Area(new Rectangle2D.Double(x + i, y + j, 1, 1)));
                    }
                }
            }*/
            XYSelector(pm, selectedArea, colorTolerance, targetColor, x, y);
            nXYSelector(pm, selectedArea, colorTolerance, targetColor, x, y);
            XnYSelector(pm, selectedArea, colorTolerance, targetColor, x, y);
            nXnYSelector(pm, selectedArea, colorTolerance, targetColor, x, y);

            return selectedArea;
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
                for (int j = 0; j <= 70; j++) {
                    magicWandSelectorComparator(pm, selectedArea, colorTolerance, targetColor, x, y, i, j);
                }
            }
        }

        private void nXYSelector(
                PMouseEvent pm, Area selectedArea, int colorTolerance,
                int targetColor, int x, int y
        ) {
            for (int i = 0; i >= -70; i--) {
                for (int j = 0; j <= 70; j++) {
                    magicWandSelectorComparator(pm, selectedArea, colorTolerance, targetColor, x, y, i, j);
                }
            }
        }

        private void XnYSelector(
                PMouseEvent pm, Area selectedArea, int colorTolerance,
                int targetColor, int x, int y
        ) {
            for (int i = 0; i <= 70; i++) {
                for (int j = 0; j >= -70; j--) {
                    magicWandSelectorComparator(pm, selectedArea, colorTolerance, targetColor, x, y, i, j);
                }
            }
        }

        private void nXnYSelector(
                PMouseEvent pm, Area selectedArea, int colorTolerance,
                int targetColor, int x, int y
        ) {
            for (int i = 0; i >= -70; i--) {
                for (int j = 0; j >= -70; j--) {
                    magicWandSelectorComparator(pm, selectedArea, colorTolerance, targetColor, x, y, i, j);
                }
            }
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

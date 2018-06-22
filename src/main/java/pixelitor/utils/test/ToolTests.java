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

package pixelitor.utils.test;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.NewImage;
import pixelitor.gui.ImageComponent;
import pixelitor.layers.Drawable;
import pixelitor.tools.AbstractBrushTool;
import pixelitor.tools.ImDrag;
import pixelitor.tools.MoveTool;
import pixelitor.tools.PPoint;
import pixelitor.tools.ShapeType;
import pixelitor.tools.Tools;
import pixelitor.tools.shapestool.ShapesTool;

import static pixelitor.colors.FillType.WHITE;

/**
 *
 */
public class ToolTests {

    /**
     * Utility class with static methods
     */
    private ToolTests() {
    }

    public static void testTools(Drawable dr) {
        NewImage.addNewImage(WHITE, 400, 400, "Tool Tests");

        SplashImageCreator.addRadialBWGradientToActiveDrawable(dr, true);

        int xDistanceFormEdge = 20;
        int yDistanceFormEdge = 20;

        // erase diagonally
        paintDiagonals(Tools.ERASER, dr, xDistanceFormEdge, yDistanceFormEdge);

        // paint a frame
        paintImageFrame(Tools.BRUSH, dr, xDistanceFormEdge, yDistanceFormEdge);

        paintHeartShape(dr);

        Composition comp = dr.getComp();

        MoveTool.move(comp, 40, 40);

        comp.repaint();
    }

    private static void paintHeartShape(Drawable dr) {
        ShapesTool shapesTool = Tools.SHAPES;
        Canvas canvas = dr.getComp().getCanvas();
        int canvasWidth = canvas.getImWidth();
        int canvasHeight = canvas.getImHeight();

        ImDrag imDrag = new ImDrag(canvasWidth * 0.25, canvasHeight * 0.25,
                canvasWidth * 0.75, canvasHeight * 0.75);

        ShapeType shapeType = ShapeType.HEART;
        shapesTool.setShapeType(shapeType);
        shapesTool.paintShape(dr, shapeType.getShape(imDrag));
    }

    private static void paintDiagonals(AbstractBrushTool eraseTool, Drawable dr, int xDistanceFormEdge, int yDistanceFormEdge) {
        Composition comp = dr.getComp();
        ImageComponent ic = comp.getIC();
        Canvas canvas = comp.getCanvas();
        int canvasWidth = canvas.getImWidth();
        int canvasHeight = canvas.getImHeight();
        PPoint topLeft = new PPoint.Image(ic, xDistanceFormEdge, yDistanceFormEdge);
        PPoint topRight = new PPoint.Image(ic, canvasWidth - xDistanceFormEdge, yDistanceFormEdge);
        PPoint bottomRight = new PPoint.Image(ic, canvasWidth - xDistanceFormEdge, canvasHeight - yDistanceFormEdge);
        PPoint bottomLeft = new PPoint.Image(ic, xDistanceFormEdge, canvasHeight - yDistanceFormEdge);
        eraseTool.drawBrushStrokeProgrammatically(dr, topLeft, bottomRight);
        eraseTool.drawBrushStrokeProgrammatically(dr, topRight, bottomLeft);
    }

    private static void paintImageFrame(AbstractBrushTool brushTool, Drawable dr, int xDistanceFormEdge, int yDistanceFormEdge) {
        Composition comp = dr.getComp();
        ImageComponent ic = comp.getIC();
        Canvas canvas = comp.getCanvas();
        int canvasWidth = canvas.getImWidth();
        int canvasHeight = canvas.getImHeight();
        PPoint topLeft = new PPoint.Image(ic, xDistanceFormEdge, yDistanceFormEdge);
        PPoint topRight = new PPoint.Image(ic, canvasWidth - xDistanceFormEdge, yDistanceFormEdge);
        PPoint bottomRight = new PPoint.Image(ic, canvasWidth - xDistanceFormEdge, canvasHeight - yDistanceFormEdge);
        PPoint bottomLeft = new PPoint.Image(ic, xDistanceFormEdge, canvasHeight - yDistanceFormEdge);
        brushTool.drawBrushStrokeProgrammatically(dr, topLeft, topRight);
        brushTool.drawBrushStrokeProgrammatically(dr, topRight, bottomRight);
        brushTool.drawBrushStrokeProgrammatically(dr, bottomRight, bottomLeft);
        brushTool.drawBrushStrokeProgrammatically(dr, bottomLeft, topLeft);
    }
}

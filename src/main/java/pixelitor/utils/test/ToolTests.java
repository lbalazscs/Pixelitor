/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.layers.ImageLayer;
import pixelitor.tools.AbstractBrushTool;
import pixelitor.tools.MoveTool;
import pixelitor.tools.ShapeType;
import pixelitor.tools.Tools;
import pixelitor.tools.UserDrag;
import pixelitor.tools.shapestool.ShapesTool;

import java.awt.Point;

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

    public static void testTools(ImageLayer layer) {
        NewImage.addNewImage(WHITE, 400, 400, "Tool Tests");

        SplashImageCreator.addRadialBWGradientToActiveLayer(layer, true);

        int xDistanceFormEdge = 20;
        int yDistanceFormEdge = 20;

        // erase diagonally
        paintDiagonals(Tools.ERASER, layer, xDistanceFormEdge, yDistanceFormEdge);

        // paint a frame
        paintImageFrame(Tools.BRUSH, layer, xDistanceFormEdge, yDistanceFormEdge);

        paintHeartShape(layer);

        Composition comp = layer.getComp();

        MoveTool.move(comp, 40, 40);

        comp.repaint();
    }

    private static void paintHeartShape(ImageLayer layer) {
        ShapesTool shapesTool = Tools.SHAPES;
        Canvas canvas = layer.getComp().getCanvas();
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        UserDrag userDrag = new UserDrag((int) (canvasWidth * 0.25), (int) (canvasHeight * 0.25), (int) (canvasWidth * 0.75), (int) (canvasHeight * 0.75));

        shapesTool.setShapeType(ShapeType.HEART);
        shapesTool.paintShapeOnIC(layer, userDrag);
    }

    private static void paintDiagonals(AbstractBrushTool eraseTool, ImageLayer layer, int xDistanceFormEdge, int yDistanceFormEdge) {
        Canvas canvas = layer.getComp().getCanvas();
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        Point topLeft = new Point(xDistanceFormEdge, yDistanceFormEdge);
        Point topRight = new Point(canvasWidth - xDistanceFormEdge, yDistanceFormEdge);
        Point bottomRight = new Point(canvasWidth - xDistanceFormEdge, canvasHeight - yDistanceFormEdge);
        Point bottomLeft = new Point(xDistanceFormEdge, canvasHeight - yDistanceFormEdge);
        eraseTool.drawBrushStrokeProgrammatically(layer, topLeft, bottomRight);
        eraseTool.drawBrushStrokeProgrammatically(layer, topRight, bottomLeft);
    }

    private static void paintImageFrame(AbstractBrushTool brushTool, ImageLayer layer, int xDistanceFormEdge, int yDistanceFormEdge) {
        Canvas canvas = layer.getComp().getCanvas();
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        Point topLeft = new Point(xDistanceFormEdge, yDistanceFormEdge);
        Point topRight = new Point(canvasWidth - xDistanceFormEdge, yDistanceFormEdge);
        Point bottomRight = new Point(canvasWidth - xDistanceFormEdge, canvasHeight - yDistanceFormEdge);
        Point bottomLeft = new Point(xDistanceFormEdge, canvasHeight - yDistanceFormEdge);
        brushTool.drawBrushStrokeProgrammatically(layer, topLeft, topRight);
        brushTool.drawBrushStrokeProgrammatically(layer, topRight, bottomRight);
        brushTool.drawBrushStrokeProgrammatically(layer, bottomRight, bottomLeft);
        brushTool.drawBrushStrokeProgrammatically(layer, bottomLeft, topLeft);
    }
}

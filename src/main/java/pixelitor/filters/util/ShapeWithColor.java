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

package pixelitor.filters.util;

import pixelitor.Canvas;
import pixelitor.colors.Colors;
import pixelitor.utils.Distortion;
import pixelitor.utils.Shapes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.List;

/**
 * Associates a {@link Shape} with a {@link Color}.
 */
public record ShapeWithColor(Shape shape, Color color) {
    public void fill(Graphics2D g) {
        g.setColor(color());
        g.fill(shape());
    }

    public ShapeWithColor transform(AffineTransform at) {
        return new ShapeWithColor(at.createTransformedShape(shape), color);
    }

    // creates SVG content with no stroke
    public static String createSvgContent(List<ShapeWithColor> shapes, Canvas canvas, Color bgColor) {
        return createSvgContent(shapes, canvas, bgColor, 0, null);
    }

    public static String createSvgContent(List<ShapeWithColor> shapes, Canvas canvas, Color bgColor,
                                          int strokeWidth, Color strokeColor) {
        StringBuilder content = new StringBuilder()
            .append(canvas.createSVGElement())
            .append("\n");
        if (bgColor != null) {
            content.append(String.format("<rect width=\"100%%\" height=\"100%%\" fill=\"#%s\"/>\n",
                Colors.toHTMLHex(bgColor, true)));
        }
        appendSvgPaths(content, shapes, strokeWidth, strokeColor);
        content.append("</svg>");
        return content.toString();
    }

    private static void appendSvgPaths(StringBuilder sb, List<ShapeWithColor> shapes,
                                       int strokeWidth, Color strokeColor) {
        for (ShapeWithColor shape : shapes) {
            String pathData = Shapes.toSvgPath(shape.shape());
            sb.append("<path d=\"").append(pathData).append("\" ");

            String colorHex = Colors.toHTMLHex(shape.color(), false);
            sb.append("fill=\"#").append(colorHex).append("\" ");

            String svgFillRule = Shapes.getSvgFillRule(shape.shape());
            sb.append("fill-rule=\"").append(svgFillRule).append("\"");

            if (strokeWidth > 0 && strokeColor != null) {
                String strokeColorHex = Colors.toHTMLHex(strokeColor, false);
                sb.append(" stroke=\"#").append(strokeColorHex).append("\"");
                sb.append(" stroke-width=\"").append(strokeWidth).append("\"");
                sb.append(" stroke-linecap=\"butt\"");
                sb.append(" stroke-linejoin=\"bevel\"");
            }

            sb.append("/>\n");
        }
    }

    public ShapeWithColor distort(Distortion distortion) {
        return new ShapeWithColor(distortion.distort(shape), color);
    }
}

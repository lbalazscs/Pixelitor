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
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.List;

/**
 * Associates a {@link Shape} with a {@link Color}.
 */
public record ShapeWithColor(Shape shape, Color color) {
    public ShapeWithColor transform(AffineTransform at) {
        return new ShapeWithColor(at.createTransformedShape(shape), color);
    }

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
        appendSvgPaths(shapes, content, strokeWidth, strokeColor);
        content.append("</svg>");
        return content.toString();
    }

    private static void appendSvgPaths(List<ShapeWithColor> shapes, StringBuilder sb,
                                       int strokeWidth, Color strokeColor) {
        for (ShapeWithColor shape : shapes) {
            String pathData = Shapes.toSvgPath(shape.shape());
            String svgFillRule = Shapes.getSvgFillRule(shape.shape());
            String colorHex = Colors.toHTMLHex(shape.color(), false);

            StringBuilder pathAttrs = new StringBuilder();
            pathAttrs.append(String.format("d=\"%s\" ", pathData));
            pathAttrs.append(String.format("fill=\"#%s\" ", colorHex));
            pathAttrs.append(String.format("fill-rule=\"%s\"", svgFillRule));

            if (strokeWidth > 0 && strokeColor != null) {
                pathAttrs.append(String.format(" stroke=\"#%s\"",
                    Colors.toHTMLHex(strokeColor, false)));
                pathAttrs.append(String.format(" stroke-width=\"%d\"", strokeWidth));
                pathAttrs.append(" stroke-linecap=\"butt\"");
                pathAttrs.append(" stroke-linejoin=\"bevel\"");
            }

            sb.append(String.format("<path %s/>\n", pathAttrs));
        }
    }

    public ShapeWithColor distort(Distortion distortion) {
        return new ShapeWithColor(distortion.distort(shape), color);
    }
}
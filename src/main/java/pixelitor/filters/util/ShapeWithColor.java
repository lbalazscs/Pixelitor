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
import pixelitor.utils.Shapes;

import java.awt.Color;
import java.awt.Shape;
import java.util.List;

/**
 * Associates a {@link Shape} with a {@link Color}.
 */
public record ShapeWithColor(Shape shape, Color color) {
    public static void appendSvgPaths(List<ShapeWithColor> shapes, StringBuilder sb) {
        for (ShapeWithColor shape : shapes) {
            String pathData = Shapes.toSvgPath(shape.shape());
            String colorHex = Colors.toHTMLHex(shape.color(), false);
            sb.append("<path d=\"%s\" fill=\"#%s\"/>\n".formatted(pathData, colorHex));
        }
    }

    public static String createSvgContent(List<ShapeWithColor> shapes, Canvas canvas, Color bgColor) {
        StringBuilder content = new StringBuilder()
            .append(canvas.createSVGElement())
            .append("\n");
        if (bgColor != null) {
            content.append(String.format("<rect width=\"100%%\" height=\"100%%\" fill=\"#%s\"/>\n",
                Colors.toHTMLHex(bgColor, true)));
        }
        appendSvgPaths(shapes, content);
        content.append("</svg>");
        return content.toString();
    }
}

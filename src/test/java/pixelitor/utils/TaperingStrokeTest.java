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

package pixelitor.utils;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaperingStrokeTest {
    @Test
    void testingPerpendicularCalculation() {
        var A = new Point2D.Float();
        var B = new Point2D.Float(2, 2);

        var P = new Point2D.Float();
        var Q = new Point2D.Float();

        Geometry.perpendiculars(A, B, (float) Math.sqrt(2), P, Q);

//                   |
//                   |
//                   |       B
//               P   |
//                   |
//      _____________A____________
//                   |
//                   |   Q
//                   |
//                   |

        assertEquals(-1.0f, P.x, 0.1);
        assertEquals(1.0f, P.y, 0.1);
        assertEquals(1.0f, Q.x, 0.1);
        assertEquals(-1.0f, Q.y, 0.1);
    }

    public static void main(String[] args) {
        testingTheStroke();
    }

    static void testingTheStroke() {
        BufferedImage image = new BufferedImage(100, 100, 2);

        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 100, 100);

        GeneralPath path = new GeneralPath();
        path.moveTo(40, 30);
        path.lineTo(75, 50);
        path.lineTo(45, 70);
        path.lineTo(15, 50);
        path.closePath();

        path.moveTo(25, 80);
        path.lineTo(75, 70);
        path.lineTo(80, 20);

        Shapes.debugPathIterator(path);
        Shapes.debugPathIterator(new TaperingStroke(30).createStrokedShape(path));

        g.setColor(Color.WHITE);
        g.setStroke(new TaperingStroke(30));
        g.draw(path);

        g.setColor(Color.RED);
        g.setStroke(new BasicStroke());
        g.draw(path);

        new JFrame() {{
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            add(new JButton(new ImageIcon(image)));
            pack();
            setLocationRelativeTo(null);
            setVisible(true);
        }};
    }
}
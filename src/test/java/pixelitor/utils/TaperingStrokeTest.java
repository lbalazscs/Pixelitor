package pixelitor.utils;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
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

        System.out.println(P);
        System.out.println(Q);

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

        assertEquals(-1f, P.x, 0.1);
        assertEquals(1f, P.y, 0.1);
        assertEquals(1f, Q.x, 0.1);
        assertEquals(-1f, Q.y, 0.1);
    }

    public static void main(String[] args) {
        new TaperingStrokeTest().testingTheStroke();
    }

    @Test
    void testingTheStroke() {

        BufferedImage image = new BufferedImage(100, 100, 2);

        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0,0,100,100);

        GeneralPath path = new GeneralPath();
        path.moveTo(40, 30);
        path.lineTo(75, 50);
        path.lineTo(45, 70);
        path.lineTo(15, 50);
        path.closePath();

        path.moveTo(25, 80);
        path.lineTo(75, 70);
        path.lineTo(80, 20 );

        Shapes.debugPathIterator(path);
        Shapes.debugPathIterator(new TaperingStroke(30).createStrokedShape(path));

        g.setColor(Color.WHITE);
        g.setStroke(new TaperingStroke(30));
        g.draw(path);

        g.setColor(Color.RED);
        g.setStroke(new BasicStroke());
        g.draw(path);

        new JFrame(){{
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            add(new JButton(new ImageIcon(image)));
            pack();
            setLocationRelativeTo(null);
            setVisible(true);
        }};

    }
}
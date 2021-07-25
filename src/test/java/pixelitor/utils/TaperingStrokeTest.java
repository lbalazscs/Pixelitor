package pixelitor.utils;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaperingStrokeTest {

    @Test
    void testingPerpendicularCalculation() {

        float[] A = {0, 0};
        float[] B = {2, 2};

        float[] P = new float[2];
        float[] Q = new float[2];

        FloatVectorMath.perpendiculars(A, B, (float) Math.sqrt(2), P, Q);

        System.out.println(Arrays.toString(P));
        System.out.println(Arrays.toString(Q));

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

        assertEquals(-1f, P[0], 0.1);
        assertEquals(1f, P[1], 0.1);
        assertEquals(1f, Q[0], 0.1);
        assertEquals(-1f, Q[1], 0.1);
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
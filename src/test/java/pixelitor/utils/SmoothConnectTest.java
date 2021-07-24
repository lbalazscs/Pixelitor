/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.colors.Colors;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.List;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

public class SmoothConnectTest extends JPanel {
    private final Dimension size = new Dimension(300, 300);

    public SmoothConnectTest() {
    }

    @Override
    public Dimension getPreferredSize() {
        return size;
    }

    @Override
    public Dimension getMinimumSize() {
        return size;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        Colors.fillWith(Color.BLACK, g2, 300, 300);
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        List<Point2D> points = List.of(
            new Point2D.Double(30, 30),
            new Point2D.Double(100, 50),
            new Point2D.Double(110, 100),
            new Point2D.Double(90, 200),
            new Point2D.Double(220, 250),
            new Point2D.Double(270, 230));
        Path2D path = Shapes.smoothConnect(points);

        g2.setColor(Color.WHITE);
        g2.draw(path);

        g2.setColor(Color.RED);
        for (Point2D point : points) {
            Shape c = Shapes.createCircle(point, 5);
            g2.fill(c);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SmoothConnectTest::buildGUI);
    }

    private static void buildGUI() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        JFrame f = new JFrame("Smooth Connect Test");
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        f.add(new SmoothConnectTest());

        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}

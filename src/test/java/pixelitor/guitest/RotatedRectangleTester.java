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

package pixelitor.guitest;

import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.painters.RotatedRectangle;

import javax.swing.*;
import java.awt.*;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.SOUTH;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * A small test app for the {@link RotatedRectangle} class.
 * Not a unit test.
 */
class RotatedRectangleTester extends JPanel {
    private double rotation;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RotatedRectangleTester::createGUI);
    }

    private static void createGUI() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }

        JFrame f = new JFrame("Test");
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        var p = new JPanel();
        p.setLayout(new BorderLayout());

        var testPanel = new RotatedRectangleTester();
        testPanel.setPreferredSize(new Dimension(200, 200));
        p.add(testPanel, CENTER);

        var angleParam = new AngleParam("", 0);
        angleParam.setAdjustmentListener(() -> testPanel.setRotation(angleParam.getValueInRadians()));
        p.add(angleParam.createGUI(), SOUTH);
        f.add(p);

        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        // paint the original rectangle in blue
        Rectangle rect = new Rectangle(80, 60, 140, 80);
        g2.setColor(Color.BLUE);
        g2.draw(rect);

        // paint the rotated rectangle in red
        var rotatedRect = new RotatedRectangle(rect, rotation);
        Shape rotatedShape = rotatedRect.asShape();
        g2.setColor(Color.RED);
        g2.draw(rotatedShape);

        // paint the bounding box of the rotated rectangle in black
        if (rotation != 0) {
            g2.setColor(Color.BLACK);
            g2.draw(rotatedRect.getBoundingBox());
        }

        rotatedRect.paintCorners(g2);
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
        repaint();
    }
}

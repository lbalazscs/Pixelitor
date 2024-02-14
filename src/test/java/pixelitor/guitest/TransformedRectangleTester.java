/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.painters.TransformedRectangle;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.SliderSpinner;

import javax.swing.*;
import java.awt.*;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.SOUTH;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * A small test app for the {@link TransformedRectangle} class.
 * Not a unit test.
 */
class TransformedRectangleTester extends JPanel {
    private double rotation;
    private double sx;
    private double sy;
    private double shx;
    private double shy;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TransformedRectangleTester::createGUI);
    }

    private static void createGUI() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }

        JFrame f = new JFrame("Transformed Rectangle Tester");
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        var p = new JPanel();
        p.setLayout(new BorderLayout());

        var testPanel = new TransformedRectangleTester();
        testPanel.setPreferredSize(new Dimension(200, 200));
        p.add(testPanel, CENTER);

        var angleParam = new AngleParam("Angle", 0);
        var sxParam = new RangeParam("Sx", -200, 100, 200, true, SliderSpinner.TextPosition.NONE_TICKS);
        var syParam = new RangeParam("Sy", -200, 100, 200, true, SliderSpinner.TextPosition.NONE_TICKS);
        var shxParam = new RangeParam("Shx", -100, 0, 100, true, SliderSpinner.TextPosition.NONE_TICKS);
        var shyParam = new RangeParam("Shy", -100, 0, 100, true, SliderSpinner.TextPosition.NONE_TICKS);
        var growParam = new RangeParam("Grow", 0, 0, 100, true, SliderSpinner.TextPosition.NONE_TICKS);
        ParamAdjustmentListener adjustmentListener = () ->
            testPanel.updateTransfrom(
                angleParam.getValueInRadians(), growParam.getValueAsDouble(),
                sxParam.getPercentage(), syParam.getPercentage(),
                shxParam.getPercentage(), shyParam.getPercentage());

        angleParam.setAdjustmentListener(adjustmentListener);
        sxParam.setAdjustmentListener(adjustmentListener);
        syParam.setAdjustmentListener(adjustmentListener);
        shxParam.setAdjustmentListener(adjustmentListener);
        shyParam.setAdjustmentListener(adjustmentListener);
        growParam.setAdjustmentListener(adjustmentListener);

        JPanel controlsPanel = new JPanel(new GridBagLayout());
        GridBagHelper gbh = new GridBagHelper(controlsPanel);
        gbh.addParam(angleParam);
        gbh.addParam(sxParam);
        gbh.addParam(syParam);
        gbh.addParam(shxParam);
        gbh.addParam(shyParam);
        gbh.addParam(growParam);
        p.add(controlsPanel, SOUTH);
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

        // paint the transformed rectangle in red
        var transformedRect = new TransformedRectangle(rect, rotation, sx, sy, shx, shy);
        Shape transformedShape = transformedRect.asShape();
        g2.setColor(Color.RED);
        g2.draw(transformedShape);

        // paint the bounding box of the transformed rectangle in black
        if (rotation != 0 || sx != 1.0 || sy != 1.0 || shx != 0 || shy != 0) {
            g2.setColor(Color.BLACK);
            g2.draw(transformedRect.getBoundingBox());
        }

        transformedRect.paintCorners(g2);
    }

    private void updateTransfrom(double rotation, double growth,
                                 double sx, double sy,
                                 double shx, double shy) {
        this.rotation = rotation;
        this.sx = sx;
        this.sy = sy;
        this.shx = shx;
        this.shy = shy;
        repaint();
    }
}

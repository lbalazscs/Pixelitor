/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import pixelitor.Composition;
import pixelitor.ImageComponent;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

public class HistogramsPanel extends JPanel implements ImageSwitchListener {
    public static final HistogramsPanel INSTANCE = new HistogramsPanel();

    private final HistogramPainter red;
    private final HistogramPainter green;
    private final HistogramPainter blue;

    private HistogramsPanel() {
        setLayout(new BorderLayout());

        red = new HistogramPainter(Color.red);
        green = new HistogramPainter(Color.green);
        blue = new HistogramPainter(Color.blue);

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setPreferredSize(new Dimension(256, 300));
        box.setMinimumSize(new Dimension(256, 300));

        box.add(red);
        box.add(green);
        box.add(blue);
        setBorder(BorderFactory.createTitledBorder("Histograms"));

        box.setBorder(BorderFactory.createLineBorder(Color.BLUE));
        JScrollPane scrollPane = new JScrollPane(box);
        add(scrollPane, BorderLayout.CENTER);
    }

    public boolean areHistogramsShown() {
        return (getParent() != null);
    }

    @Override
    public void noOpenImageAnymore() {
        red.updateWithNothing();
        green.updateWithNothing();
        blue.updateWithNothing();
        repaint();
    }

    @Override
    public void newImageOpened() {
    }

    @Override
    public void activeImageHasChanged(ImageComponent oldIC, ImageComponent newIC) {
        updateFromCompIfShown(newIC.getComp());
    }

    public void updateFromCompIfShown(Composition comp) {
        if (comp == null) {
            throw new IllegalArgumentException("trying to update with null comp");
        }
        if (!areHistogramsShown()) {
            return;
        }
        BufferedImage image = comp.getCompositeImage();

        int histogramResolution = 256;
        int[] redValues = new int[histogramResolution];
        int[] blueValues = new int[histogramResolution];
        int[] greenValues = new int[histogramResolution];

        int[] data = ImageUtils.getPixelsAsArray(image);
        for (int rgb : data) {
            //                int a = (rgb >>> 24) & 0xFF;
            int r = (rgb >>> 16) & 0xFF;
            int g = (rgb >>> 8) & 0xFF;
            int b = (rgb) & 0xFF;

            redValues[r]++;
            greenValues[g]++;
            blueValues[b]++;
        }

        boolean useLogarithm = false;
        if (useLogarithm) {
            for (int i = 0; i < histogramResolution; i++) {
                redValues[i] = (int) (1000.0 * Math.log(redValues[i]));
                greenValues[i] = (int) (1000.0 * Math.log(greenValues[i]));
                blueValues[i] = (int) (1000.0 * Math.log(blueValues[i]));
            }
        }

        red.updateData(redValues);
        green.updateData(greenValues);
        blue.updateData(blueValues);
        repaint();
    }
}

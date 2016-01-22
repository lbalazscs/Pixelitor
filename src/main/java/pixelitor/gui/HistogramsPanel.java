/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

package pixelitor.gui;

import pixelitor.Composition;
import pixelitor.utils.ImageSwitchListener;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Objects;

import static java.awt.Color.BLUE;
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;

public class HistogramsPanel extends JPanel implements ImageSwitchListener {
    public static final HistogramsPanel INSTANCE = new HistogramsPanel();

    private final HistogramPainter red;
    private final HistogramPainter green;
    private final HistogramPainter blue;
    private static final int HISTOGRAM_RESOLUTION = 256;

    private HistogramsPanel() {
        setLayout(new BorderLayout());

        red = new HistogramPainter(RED);
        green = new HistogramPainter(GREEN);
        blue = new HistogramPainter(BLUE);

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setPreferredSize(new Dimension(256, 300));
        box.setMinimumSize(new Dimension(256, 300));

        box.add(red);
        box.add(green);
        box.add(blue);
        setBorder(BorderFactory.createTitledBorder("Histograms"));

        box.setBorder(BorderFactory.createLineBorder(BLUE));
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
    public void newImageOpened(Composition comp) {
    }

    @Override
    public void activeImageHasChanged(ImageComponent oldIC, ImageComponent newIC) {
        updateFromCompIfShown(newIC.getComp());
    }

    public void updateFromCompIfShown(Composition comp) {
        Objects.requireNonNull(comp);
        if (!areHistogramsShown()) {
            return;
        }
        BufferedImage image = comp.getCompositeImage();

        int[] redValues = new int[HISTOGRAM_RESOLUTION];
        int[] blueValues = new int[HISTOGRAM_RESOLUTION];
        int[] greenValues = new int[HISTOGRAM_RESOLUTION];

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
            for (int i = 0; i < HISTOGRAM_RESOLUTION; i++) {
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

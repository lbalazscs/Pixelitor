/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.ActiveImageChangeListener;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.Objects;

import static java.awt.Color.BLUE;
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static javax.swing.BorderFactory.createTitledBorder;

/**
 * The panel that shows the histograms
 */
public class HistogramsPanel extends JPanel implements ActiveImageChangeListener {
    public static final HistogramsPanel INSTANCE = new HistogramsPanel();
    private static final String TYPE_LOGARITHMIC = "Logarithmic";
    private static final String TYPE_LINEAR = "Linear";

    private final HistogramPainter red;
    private final HistogramPainter green;
    private final HistogramPainter blue;
    private static final int HISTOGRAM_RESOLUTION = 256;

    private boolean logarithmic;

    private HistogramsPanel() {
        setLayout(new BorderLayout());

        red = new HistogramPainter(RED);
        green = new HistogramPainter(GREEN);
        blue = new HistogramPainter(BLUE);

        JPanel painters = new JPanel();
        painters.setLayout(new GridLayout(3, 1, 0, 0));

        Dimension size = new Dimension(258, 306);
        painters.setPreferredSize(size);
        painters.setMinimumSize(size);

        painters.add(red);
        painters.add(green);
        painters.add(blue);

        JComboBox<String> typeChooser = new JComboBox<>(
                new String[]{TYPE_LINEAR, TYPE_LOGARITHMIC});
        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        northPanel.add(new JLabel("Type:"));
        northPanel.add(typeChooser);
        add(northPanel, BorderLayout.NORTH);
        typeChooser.addActionListener(e ->
                typeChanged((String) typeChooser.getSelectedItem()));

        setBorder(createTitledBorder("Histograms"));
        JScrollPane scrollPane = new JScrollPane(painters);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void typeChanged(String selected) {
        boolean isLogarithmicNow = selected.equals(TYPE_LOGARITHMIC);
        if (isLogarithmicNow != logarithmic) {
            logarithmic = isLogarithmicNow;
            ImageComponents.getActiveComp().ifPresent(
                    this::updateFromCompIfShown);
        }
    }

    public boolean isShown() {
        return (getParent() != null);
    }

    @Override
    public void noOpenImageAnymore() {
        red.noOpenImageAnymore();
        green.noOpenImageAnymore();
        blue.noOpenImageAnymore();
        repaint();
    }

    @Override
    public void activeImageChanged(ImageComponent oldIC, ImageComponent newIC) {
        updateFromCompIfShown(newIC.getComp());
    }

    public void updateFromCompIfShown(Composition comp) {
        Objects.requireNonNull(comp);
        if (!isShown()) {
            return;
        }
        BufferedImage image = comp.getCompositeImage();

        int[] reds = new int[HISTOGRAM_RESOLUTION];
        int[] blues = new int[HISTOGRAM_RESOLUTION];
        int[] greens = new int[HISTOGRAM_RESOLUTION];

        int[] data = ImageUtils.getPixelsAsArray(image);
        for (int rgb : data) {
            int a = (rgb >>> 24) & 0xFF;
            if (a > 0) {
                int r = (rgb >>> 16) & 0xFF;
                int g = (rgb >>> 8) & 0xFF;
                int b = (rgb) & 0xFF;

                reds[r]++;
                greens[g]++;
                blues[b]++;
            }
        }

        if (logarithmic) {
            for (int i = 0; i < HISTOGRAM_RESOLUTION; i++) {
                // Add one before taking the logarithm to avoid calculating log(0)
                // Note that log(1) = 0, which is just perfect
                // Also multiply with a big number to avoid rounding errors
                reds[i] = (int) (1000.0 * (Math.log(reds[i] + 1)));
                greens[i] = (int) (1000.0 * (Math.log(greens[i] + 1)));
                blues[i] = (int) (1000.0 * (Math.log(blues[i] + 1)));
            }
        }

        red.updateData(reds);
        green.updateData(greens);
        blue.updateData(blues);
        repaint();
    }
}

/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.Views;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.ViewActivationListener;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.Objects;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static java.awt.Color.*;
import static java.awt.FlowLayout.LEFT;
import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.utils.Texts.i18n;

/**
 * The panel that shows the histograms
 */
public class HistogramsPanel extends JPanel implements ViewActivationListener {
    private static final HistogramsPanel INSTANCE = new HistogramsPanel();

    public static final int HISTOGRAM_RESOLUTION = 256;

    private static final String TYPE_LOGARITHMIC = "Logarithmic";
    private static final String TYPE_LINEAR = "Linear";
    private final JComboBox<String> typeChooser;

    private final HistogramPainter red;
    private final HistogramPainter green;
    private final HistogramPainter blue;

    private boolean logarithmic;

    private HistogramsPanel() {
        super(new BorderLayout());

        red = new HistogramPainter(RED);
        green = new HistogramPainter(GREEN);
        blue = new HistogramPainter(BLUE);

        JPanel painters = new JPanel();
        painters.setLayout(new GridLayout(3, 1, 0, 0));

        var size = new Dimension(
            HISTOGRAM_RESOLUTION + 2,
            3 * HistogramPainter.PREFERRED_HEIGHT);
        painters.setPreferredSize(size);
        painters.setMinimumSize(size);

        painters.add(red);
        painters.add(green);
        painters.add(blue);

        typeChooser = new JComboBox<>(new String[]{TYPE_LINEAR, TYPE_LOGARITHMIC});
        typeChooser.addActionListener(e -> typeChanged());

        JPanel northPanel = new JPanel(new FlowLayout(LEFT));
        northPanel.add(new JLabel(GUIText.TYPE + ":"));
        northPanel.add(typeChooser);
        add(northPanel, NORTH);

        setBorder(createTitledBorder(i18n("histograms")));
        var scrollPane = new JScrollPane(painters);
        add(scrollPane, CENTER);
    }

    private void typeChanged() {
        String newType = (String) typeChooser.getSelectedItem();
        boolean isLogarithmicNow = newType.equals(TYPE_LOGARITHMIC);
        if (isLogarithmicNow != logarithmic) {
            logarithmic = isLogarithmicNow;
            Views.onActiveComp(this::update);
        }
    }

    @Override
    public void allViewsClosed() {
        red.allViewsClosed();
        green.allViewsClosed();
        blue.allViewsClosed();
        repaint();
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        update(newView.getComp());
    }

    public static void updateFromActiveComp() {
        Views.onActiveComp(INSTANCE::update);
    }

    public static void updateFrom(Composition comp) {
        INSTANCE.update(comp);
    }

    private void update(Composition comp) {
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
                int b = rgb & 0xFF;

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
                reds[i] = (int) (1000.0 * Math.log(reds[i] + 1));
                greens[i] = (int) (1000.0 * Math.log(greens[i] + 1));
                blues[i] = (int) (1000.0 * Math.log(blues[i] + 1));
            }
        }

        red.updateData(reds);
        green.updateData(greens);
        blue.updateData(blues);
        repaint();
    }

    public static HistogramsPanel get() {
        return INSTANCE;
    }

    public static boolean isShown() {
        return INSTANCE.getParent() != null;
    }
}

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
import static java.awt.Color.BLUE;
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static java.awt.FlowLayout.LEFT;
import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.utils.Texts.i18n;

/**
 * The panel that shows the histograms
 */
public class HistogramsPanel extends JPanel implements ViewActivationListener {
    private static final HistogramsPanel INSTANCE = new HistogramsPanel();

    public static final int NUM_BINS = 256;

    private static final String TYPE_LOGARITHMIC = "Logarithmic";
    private static final String TYPE_LINEAR = "Linear";
    private JComboBox<String> typeChooser;

    private final HistogramPainter redPainter;
    private final HistogramPainter greenPainter;
    private final HistogramPainter bluePainter;

    private boolean logarithmic;

    private HistogramsPanel() {
        super(new BorderLayout());

        redPainter = new HistogramPainter(RED);
        greenPainter = new HistogramPainter(GREEN);
        bluePainter = new HistogramPainter(BLUE);

        JPanel northPanel = initNorthPanel();
        add(northPanel, NORTH);

        JPanel painters = initPaintersPanel();
        setBorder(createTitledBorder(i18n("histograms")));
        add(new JScrollPane(painters), CENTER);
    }

    private JPanel initNorthPanel() {
        typeChooser = new JComboBox<>(new String[]{TYPE_LINEAR, TYPE_LOGARITHMIC});
        typeChooser.addActionListener(e -> typeChanged());

        JPanel northPanel = new JPanel(new FlowLayout(LEFT));
        northPanel.add(new JLabel(GUIText.TYPE + ":"));
        northPanel.add(typeChooser);
        return northPanel;
    }

    private JPanel initPaintersPanel() {
        JPanel painters = new JPanel();
        painters.setLayout(new GridLayout(3, 1, 0, 0));

        var size = new Dimension(
            NUM_BINS + 2,
            3 * HistogramPainter.PREFERRED_HEIGHT);
        painters.setPreferredSize(size);
        painters.setMinimumSize(size);

        painters.add(redPainter);
        painters.add(greenPainter);
        painters.add(bluePainter);

        return painters;
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
        redPainter.allViewsClosed();
        greenPainter.allViewsClosed();
        bluePainter.allViewsClosed();
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

        int[] reds = new int[NUM_BINS];
        int[] blues = new int[NUM_BINS];
        int[] greens = new int[NUM_BINS];

        int[] data = ImageUtils.getPixelArray(image);
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
            for (int i = 0; i < NUM_BINS; i++) {
                // Add one before taking the logarithm to avoid calculating log(0)
                // Note that log(1) = 0, which is just perfect
                // Also multiply with a big number to avoid rounding errors
                reds[i] = (int) (1000.0 * Math.log(reds[i] + 1));
                greens[i] = (int) (1000.0 * Math.log(greens[i] + 1));
                blues[i] = (int) (1000.0 * Math.log(blues[i] + 1));
            }
        }

        redPainter.updateData(reds);
        greenPainter.updateData(greens);
        bluePainter.updateData(blues);
        repaint();
    }

    public static HistogramsPanel get() {
        return INSTANCE;
    }

    public static boolean isShown() {
        return INSTANCE.getParent() != null;
    }
}

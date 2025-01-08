/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
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

    private static final String SCALE_LOGARITHMIC = "Logarithmic";
    private static final String SCALE_LINEAR = "Linear";
    private JScrollPane paintersPanel;
    private JComboBox<String> scaleSelector;

    private static final String HISTOGRAM_TYPE_RGB = "RGB";
    private static final String HISTOGRAM_TYPE_LUMINANCE = "Luminance";
    private JComboBox<String> histogramTypeSelector;

    private final HistogramPainter redPainter;
    private final HistogramPainter greenPainter;
    private final HistogramPainter bluePainter;
    private final HistogramPainter luminancePainter;

    private int[] reds;
    private int[] greens;
    private int[] blues;
    private int[] luminances;

    private int[] logReds;
    private int[] logGreens;
    private int[] logBlues;
    private int[] logLuminances;

    private boolean isLogarithmic;
    private boolean isLuminance;

    private HistogramsPanel() {
        super(new BorderLayout());

        redPainter = new HistogramPainter(RED, false);
        greenPainter = new HistogramPainter(GREEN, false);
        bluePainter = new HistogramPainter(BLUE, false);
        luminancePainter = new HistogramPainter(Color.WHITE, true);

        add(initControlPanel(), NORTH);
        paintersPanel = new JScrollPane(initPaintersPanel());
        add(paintersPanel, CENTER);

        setBorder(createTitledBorder(i18n("histograms")));
    }

    private JPanel initControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(LEFT));

        scaleSelector = new JComboBox<>(new String[]{SCALE_LINEAR, SCALE_LOGARITHMIC});
        scaleSelector.addActionListener(e -> scaleChanged());
        controlPanel.add(new JLabel("Scale:"));
        controlPanel.add(scaleSelector);

        histogramTypeSelector = new JComboBox<>(new String[]{HISTOGRAM_TYPE_RGB, HISTOGRAM_TYPE_LUMINANCE});
        histogramTypeSelector.addActionListener(e -> typeChanged());
        controlPanel.add(new JLabel(GUIText.TYPE + ":"));
        controlPanel.add(histogramTypeSelector);

        return controlPanel;
    }

    private JPanel initPaintersPanel() {
        JPanel painters = new JPanel();

        int numPainters = isLuminance ? 1 : 3;
        painters.setLayout(new GridLayout(numPainters, 1, 0, 0));

        if (isLuminance) {
            painters.add(luminancePainter);
        } else {
            painters.add(redPainter);
            painters.add(greenPainter);
            painters.add(bluePainter);
        }

        Dimension size = new Dimension(
            NUM_BINS + 2,
            numPainters * HistogramPainter.PREFERRED_HEIGHT);
        painters.setPreferredSize(size);
        painters.setMinimumSize(size);

        return painters;
    }

    private void scaleChanged() {
        String newScale = (String) scaleSelector.getSelectedItem();
        boolean newScaleIsLogarithmic = newScale.equals(SCALE_LOGARITHMIC);
        if (newScaleIsLogarithmic != isLogarithmic) {
            isLogarithmic = newScaleIsLogarithmic;

            calcLazyData();
            updatePainterData();
            repaint();
        }
    }

    private void typeChanged() {
        String newLuminance = (String) histogramTypeSelector.getSelectedItem();
        boolean isNewLuminance = HISTOGRAM_TYPE_LUMINANCE.equals(newLuminance);

        if (isNewLuminance != isLuminance) {
            isLuminance = isNewLuminance;

            remove(paintersPanel);
            paintersPanel = new JScrollPane(initPaintersPanel());
            add(paintersPanel, CENTER);

            revalidate();

            calcLazyData();
            updatePainterData();

            repaint();
        }
    }

    @Override
    public void allViewsClosed() {
        redPainter.clearData();
        greenPainter.clearData();
        bluePainter.clearData();

        luminancePainter.clearData();

        repaint();
    }

    @Override
    public void viewActivated(View oldView, View newView) {
        updateHistograms(newView.getComp());
    }

    public static void updateFromActiveComp() {
        Views.onActiveComp(INSTANCE::updateHistograms);
    }

    public static void updateFrom(Composition comp) {
        INSTANCE.updateHistograms(comp);
    }

    // extracts the essential information from the image
    private void calcBaseArrays(BufferedImage image) {
        if (reds != null) {
            Arrays.fill(reds, 0);
            Arrays.fill(greens, 0);
            Arrays.fill(blues, 0);
            Arrays.fill(luminances, 0);
        } else {
            reds = new int[NUM_BINS];
            greens = new int[NUM_BINS];
            blues = new int[NUM_BINS];
            luminances = new int[NUM_BINS];
        }

        int[] pixels = ImageUtils.getPixels(image);
        for (int rgb : pixels) {
            int a = (rgb >>> 24) & 0xFF;
            if (a > 0) {
                int r = (rgb >>> 16) & 0xFF;
                int g = (rgb >>> 8) & 0xFF;
                int b = rgb & 0xFF;

                reds[r]++;
                greens[g]++;
                blues[b]++;

                int lum = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                luminances[lum]++;
            }
        }
    }

    private void calcRGBLogs() {
        if (logReds != null) {
            Arrays.fill(logReds, 0);
            Arrays.fill(logGreens, 0);
            Arrays.fill(logBlues, 0);
        } else {
            logReds = new int[NUM_BINS];
            logGreens = new int[NUM_BINS];
            logBlues = new int[NUM_BINS];
        }

        calcLog(reds, logReds);
        calcLog(greens, logGreens);
        calcLog(blues, logBlues);
    }

    private void calcLumLogs() {
        if (logLuminances != null) {
            Arrays.fill(logLuminances, 0);
        } else {
            logLuminances = new int[NUM_BINS];
        }

        calcLog(luminances, logLuminances);
    }

    // called when the image is first added or when the image is changed
    private void changeImage(BufferedImage image) {
        calcBaseArrays(image);
        calcLazyData();

        updatePainterData();
    }

    private void calcLazyData() {
        if (isLogarithmic) {
            if (isLuminance) {
                calcLumLogs();
            } else {
                calcRGBLogs();
            }
        }
    }

    private void updatePainterData() {
        if (isLuminance) {
            if (isLogarithmic) {
                calcLumLogs();
                luminancePainter.updateData(logLuminances);
            } else {
                luminancePainter.updateData(luminances);
            }
        } else { // RGB mode
            if (isLogarithmic) {
                calcRGBLogs();
                redPainter.updateData(logReds);
                greenPainter.updateData(logGreens);
                bluePainter.updateData(logBlues);
            } else {
                redPainter.updateData(reds);
                greenPainter.updateData(greens);
                bluePainter.updateData(blues);
            }
        }
    }

    private void updateHistograms(Composition comp) {
        Objects.requireNonNull(comp);
        if (!isShown()) {
            return;
        }

        changeImage(comp.getCompositeImage());
        repaint();
    }

    private static void calcLog(int[] input, int[] output) {
        for (int i = 0; i < NUM_BINS; i++) {
            // Add one before taking the logarithm to avoid calculating log(0)
            // Note that log(1) = 0, which is just perfect.
            // Also multiply by a large number to mitigate rounding errors.
            output[i] = (int) (1000.0 * Math.log(input[i] + 1));
        }
    }

    public static HistogramsPanel getInstance() {
        return INSTANCE;
    }

    public static boolean isShown() {
        return INSTANCE.getParent() != null;
    }
}

/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import java.awt.event.HierarchyEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static java.awt.Color.*;
import static java.awt.FlowLayout.LEFT;
import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.utils.Texts.i18n;

/**
 * The panel that shows the histograms.
 */
public class HistogramsPanel extends JPanel implements ViewActivationListener {
    public static final int NUM_BINS = 256;

    private static final String SCALE_LOGARITHMIC = "Logarithmic";
    private static final String SCALE_LINEAR = "Linear";
    private final JScrollPane scrollPane;
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

    private boolean rgbLogsDirty = true;
    private boolean lumLogsDirty = true;

    private boolean logMode;
    private boolean luminanceMode;

    // true if recalculation is necessary when the panel is shown
    private boolean dirty = Views.getActive() != null;

    public HistogramsPanel() {
        super(new BorderLayout());

        redPainter = new HistogramPainter(RED, false);
        greenPainter = new HistogramPainter(GREEN, false);
        bluePainter = new HistogramPainter(BLUE, false);
        luminancePainter = new HistogramPainter(Color.WHITE, true);

        add(createControlPanel(), NORTH);
        scrollPane = new JScrollPane(createPaintersPanel());
        add(scrollPane, CENTER);

        setBorder(createTitledBorder(i18n("histograms")));

        setupVisibilityListener();
        Views.addActivationListener(this);
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(LEFT));

        // no labels are added, because they would take up too much horizontal space
        scaleSelector = new JComboBox<>(new String[]{SCALE_LINEAR, SCALE_LOGARITHMIC});
        scaleSelector.addActionListener(_ -> scaleChanged());
        controlPanel.add(scaleSelector);

        histogramTypeSelector = new JComboBox<>(new String[]{HISTOGRAM_TYPE_RGB, HISTOGRAM_TYPE_LUMINANCE});
        histogramTypeSelector.addActionListener(_ -> typeChanged());
        controlPanel.add(histogramTypeSelector);

        return controlPanel;
    }

    private JPanel createPaintersPanel() {
        JPanel p = new JPanel();

        int numPainters = luminanceMode ? 1 : 3;
        p.setLayout(new GridLayout(numPainters, 1, 0, 0));

        if (luminanceMode) {
            p.add(luminancePainter);
        } else {
            p.add(redPainter);
            p.add(greenPainter);
            p.add(bluePainter);
        }

        Dimension size = new Dimension(
            NUM_BINS + 2,
            numPainters * HistogramPainter.PREFERRED_HEIGHT);
        p.setPreferredSize(size);
        p.setMinimumSize(size);

        return p;
    }

    private void setupVisibilityListener() {
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                panelShown();
            }
        });
    }

    private void panelShown() {
        if (dirty) {
            Composition comp = Views.getActiveComp();
            if (comp != null) {
                updateHistograms(comp);
            } else {
                dirty = false;
            }
        }
    }

    // called when the user toggles between Linear and Logarithmic
    private void scaleChanged() {
        String newScale = (String) scaleSelector.getSelectedItem();
        boolean newScaleIsLogarithmic = newScale.equals(SCALE_LOGARITHMIC);
        if (newScaleIsLogarithmic != logMode) {
            logMode = newScaleIsLogarithmic;

            updatePainterData();
            repaint();
        }
    }

    // called when the user toggles between RGB and Luminance
    private void typeChanged() {
        String newType = (String) histogramTypeSelector.getSelectedItem();
        boolean newTypeIsLuminance = HISTOGRAM_TYPE_LUMINANCE.equals(newType);

        if (newTypeIsLuminance != luminanceMode) {
            luminanceMode = newTypeIsLuminance;

            scrollPane.setViewportView(createPaintersPanel());
            revalidate();

            updatePainterData();

            repaint();
        }
    }

    @Override
    public void allViewsClosed() {
        dirty = false;

        // clear cached histogram arrays to prevent resurrecting stale data
        reds = null;
        greens = null;
        blues = null;
        luminances = null;
        logReds = null;
        logGreens = null;
        logBlues = null;
        logLuminances = null;
        rgbLogsDirty = true;
        lumLogsDirty = true;

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

    public static void updateFrom(Composition comp) {
        HistogramsPanel panel = AppPanel.HISTOGRAMS.getComponent();
        panel.updateHistograms(comp);
    }

    // computes linear histogram bin counts from the image pixels
    private void calcLinearHistograms(BufferedImage image) {
        reds = resetOrCreate(reds);
        greens = resetOrCreate(greens);
        blues = resetOrCreate(blues);
        luminances = resetOrCreate(luminances);

        int[] pixels = ImageUtils.getPixels(image);
        for (int rgb : pixels) {
            int a = rgb >>> 24;
            if (a > 0) {
                int r = (rgb >>> 16) & 0xFF;
                int g = (rgb >>> 8) & 0xFF;
                int b = rgb & 0xFF;

                reds[r]++;
                greens[g]++;
                blues[b]++;

                // the extra 128 avoids under-representing the last bin
                int lum = (77 * r + 150 * g + 29 * b + 128) >> 8;

                luminances[lum]++;
            }
        }

        // invalidate cached logarithmic arrays
        rgbLogsDirty = true;
        lumLogsDirty = true;
    }

    private void calcRGBLogs() {
        logReds = resetOrCreate(logReds);
        logGreens = resetOrCreate(logGreens);
        logBlues = resetOrCreate(logBlues);

        calcLog(reds, logReds);
        calcLog(greens, logGreens);
        calcLog(blues, logBlues);
        rgbLogsDirty = false;
    }

    private void calcLumLogs() {
        logLuminances = resetOrCreate(logLuminances);
        calcLog(luminances, logLuminances);
        lumLogsDirty = false;
    }

    private static int[] resetOrCreate(int[] array) {
        if (array == null) {
            return new int[NUM_BINS];
        } else {
            Arrays.fill(array, 0);
            return array;
        }
    }

    private void updatePainterData() {
        if (!hasData()) {
            return;
        }

        if (luminanceMode) {
            if (logMode) {
                if (lumLogsDirty) {
                    calcLumLogs();
                }
                luminancePainter.updateData(logLuminances);
            } else {
                luminancePainter.updateData(luminances);
            }
        } else { // RGB mode
            if (logMode) {
                if (rgbLogsDirty) {
                    calcRGBLogs();
                }
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

    // returns false if there are no open compositions yet
    private boolean hasData() {
        return reds != null;
    }

    // called when a new composition is loaded/updated
    private void updateHistograms(Composition comp) {
        if (comp == null) {
            return;
        }

        // if not currently visible on screen, defer computation
        if (!isShowing()) {
            dirty = true;
            return;
        }

        dirty = false;
        BufferedImage newImage = comp.getCompositeImage();
        calcLinearHistograms(newImage);
        updatePainterData();
        repaint();
    }

    private static void calcLog(int[] input, int[] output) {
        for (int i = 0; i < NUM_BINS; i++) {
            // Add one before taking the logarithm to avoid calculating log(0).
            // Zero-count bins remain zero because log(1) == 0.
            // Also multiply by a large number to prevent precision loss from integer truncation.
            output[i] = (int) (1000.0 * Math.log(input[i] + 1));
        }
    }
}

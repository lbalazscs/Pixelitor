/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.io;

import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.ImagePanel;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.io.JpegOutput.ImageWithSize;
import pixelitor.tools.HandToolSupport;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.JProgressBarTracker;
import pixelitor.utils.Messages;
import pixelitor.utils.ProgressPanel;
import pixelitor.utils.ProgressTracker;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.SOUTH;
import static java.awt.FlowLayout.LEFT;
import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.WEST;

/**
 * The panel shown in the "Export Optimized JPEG..." dialog
 */
public class OptimizedJpegSavePanel extends JPanel {
    private static final int GRID_HOR_GAP = 10;
    private static final int GRID_VER_GAP = 10;
    private final BufferedImage image;
    private ImagePanel optimized;
    private RangeParam qualityParam;
    private JLabel sizeLabel;
    private ImagePanel original;
    private JCheckBox progressiveCB;
    private ProgressPanel progressPanel;

    private OptimizedJpegSavePanel(BufferedImage image) {
        this.image = image;

        JPanel controlsPanel = createControlsPanel();
        JPanel comparePanel = createComparePanel(image);

        setLayout(new BorderLayout(3, 3));
        add(comparePanel, CENTER);
        add(controlsPanel, SOUTH);

        updatePreview();
    }

    private JPanel createComparePanel(BufferedImage image) {
        JPanel comparePanel = new JPanel();
        comparePanel.setLayout(new GridLayout(1, 2, GRID_HOR_GAP, GRID_VER_GAP));
        Dimension imageSize = new Dimension(image.getWidth(), image.getHeight());

        original = createViewPanel(imageSize);
        original.setImage(image);

        optimized = createViewPanel(imageSize);

        setupScrollPanes(comparePanel);

        return comparePanel;
    }

    private void setupScrollPanes(JPanel comparePanel) {
        JScrollPane originalSP = createScrollPane(original, "Original");
        JScrollPane optimizedSP = createScrollPane(optimized, "Optimized");

        comparePanel.add(originalSP);
        comparePanel.add(optimizedSP);

        GUIUtils.setupSharedScrollModels(originalSP, optimizedSP);
    }

    private static JScrollPane createScrollPane(ImagePanel original, String borderTitle) {
        JScrollPane sp = new JScrollPane(original);
        HandToolSupport.addBehavior(sp);
        sp.setBorder(createTitledBorder(borderTitle));

        return sp;
    }

    private static ImagePanel createViewPanel(Dimension imageSize) {
        // no checkerboard, because here it is a black image
        ImagePanel view = new ImagePanel(false);

        view.setPreferredSize(imageSize);

        return view;
    }

    private JPanel createControlsPanel() {
        var p = new JPanel(new FlowLayout(LEFT));

        p.add(new JLabel("Progressive:"));
        progressiveCB = new JCheckBox("", false);
        progressiveCB.addActionListener(e -> updatePreview());
        p.add(progressiveCB);

        qualityParam = new RangeParam("  JPEG Quality", 1, 60, 100);
        qualityParam.setAdjustmentListener(this::updatePreview);
        p.add(new SliderSpinner(qualityParam, WEST, false));

        sizeLabel = new JLabel();
        p.add(sizeLabel);

        p.add(Box.createRigidArea(new Dimension(40, 10)));

        progressPanel = new ProgressPanel();
        p.add(progressPanel);

        return p;
    }

    private void updatePreview() {
        JpegSettings settings = getSelectedSettings();

        CompletableFuture
            .supplyAsync(
                () -> createPreview(settings),
                IOThread.getExecutor())
            .thenAcceptAsync(
                this::setPreview,
                EventQueue::invokeLater)
            .exceptionally(Messages::showExceptionOnEDT);
    }

    private ImageWithSize createPreview(JpegSettings settings) {
        ProgressTracker pt = new JProgressBarTracker(progressPanel);
        return JpegOutput.writeJPGtoPreviewImage(image, settings, pt);
    }

    private void setPreview(ImageWithSize imageWithSize) {
        BufferedImage newPreview = imageWithSize.getImage();
        optimized.changeImage(newPreview);

        int numBytes = imageWithSize.getSize();
        sizeLabel.setText("  Size: " + Utils.bytesToString(numBytes));
    }

    private JpegSettings getSelectedSettings() {
        return new JpegSettings(qualityParam.getPercentageValF(),
                progressiveCB.isSelected());
    }

    public static void showInDialog(BufferedImage image, JFrame frame) {
        BufferedImage rgbImage = ImageUtils.convertToRGB(image, false);
        OptimizedJpegSavePanel p = new OptimizedJpegSavePanel(rgbImage);

        new DialogBuilder()
            .content(p)
            .owner(frame)
            .title("Save Optimized JPEG")
            .okText("Save")
            .okAction(() -> {
                JpegSettings settings = p.getSelectedSettings();
                OpenSave.saveJpegWithQuality(settings);
            })
            .show();
    }
}


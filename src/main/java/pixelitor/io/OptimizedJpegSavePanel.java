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

package pixelitor.io;

import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.ImagePanel;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.io.JpegOutput.ImageWithSize;
import pixelitor.tools.HandToolSupport;
import pixelitor.utils.*;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.SOUTH;
import static java.awt.FlowLayout.LEFT;
import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.WEST;
import static pixelitor.utils.Threads.onEDT;
import static pixelitor.utils.Threads.onPool;

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
        super(new BorderLayout(3, 3));

        this.image = image;

        JPanel controlsPanel = createControlsPanel();
        JPanel comparePanel = createComparePanel(image);

        add(comparePanel, CENTER);
        add(controlsPanel, SOUTH);

        updatePreviewAsync();
    }

    private JPanel createComparePanel(BufferedImage image) {
        JPanel comparePanel = new JPanel(new GridLayout(1, 2, GRID_HOR_GAP, GRID_VER_GAP));
        var imageSize = new Dimension(image.getWidth(), image.getHeight());

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

        GUIUtils.shareScrollModels(originalSP, optimizedSP);
    }

    private static JScrollPane createScrollPane(ImagePanel original, String borderTitle) {
        var scrollPane = new JScrollPane(original);
        HandToolSupport.addBehavior(scrollPane);
        scrollPane.setBorder(createTitledBorder(borderTitle));

        return scrollPane;
    }

    private static ImagePanel createViewPanel(Dimension imageSize) {
        // no checkerboard, because here transparency is shown as black
        var viewPanel = new ImagePanel(false);

        viewPanel.setPreferredSize(imageSize);

        return viewPanel;
    }

    private JPanel createControlsPanel() {
        JPanel p = new JPanel(new FlowLayout(LEFT));

        p.add(new JLabel("Progressive:"));
        progressiveCB = new JCheckBox("", false);
        progressiveCB.addActionListener(e -> updatePreviewAsync());
        p.add(progressiveCB);

        qualityParam = new RangeParam("  JPEG Quality", 1, 60, 100);
        qualityParam.setAdjustmentListener(this::updatePreviewAsync);
        p.add(new SliderSpinner(qualityParam, WEST, false));

        sizeLabel = new JLabel();
        p.add(sizeLabel);

        p.add(Box.createRigidArea(new Dimension(40, 10)));

        progressPanel = new ProgressPanel();
        p.add(progressPanel);

        return p;
    }

    private void updatePreviewAsync() {
        CompletableFuture
            .supplyAsync(() -> createPreview(getSettings()), onPool)
            .thenAcceptAsync(this::setPreview, onEDT)
            .exceptionally(Messages::showExceptionOnEDT);
    }

    private ImageWithSize createPreview(JpegInfo config) {
        var tracker = new JProgressBarTracker(progressPanel);
        return JpegOutput.writeJPGtoPreviewImage(image, config, tracker);
    }

    private void setPreview(ImageWithSize imageWithSize) {
        BufferedImage newPreview = imageWithSize.image();
        optimized.changeImage(newPreview);

        int numBytes = imageWithSize.size();
        sizeLabel.setText("  Size: " + Utils.bytesToString(numBytes));
    }

    private JpegInfo getSettings() {
        return new JpegInfo((float) qualityParam.getPercentage(),
            progressiveCB.isSelected());
    }

    public static void showInDialog(BufferedImage image) {
        var rgbImage = ImageUtils.convertToRGB(image, false);
        var savePanel = new OptimizedJpegSavePanel(rgbImage);

        new DialogBuilder()
            .content(savePanel)
            .title("Save Optimized JPEG")
            .okText("Save")
            .okAction(() -> IO.saveJpegWithQuality(savePanel.getSettings()))
            .show();
    }
}


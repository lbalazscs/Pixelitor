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

package pixelitor.io;

import pixelitor.Composition;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.ImagePanel;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.tools.HandToolSupport;
import pixelitor.utils.*;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.SOUTH;
import static java.awt.FlowLayout.LEFT;
import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.gui.utils.SliderSpinner.LabelPosition.WEST;
import static pixelitor.utils.Texts.i18n;
import static pixelitor.utils.Threads.onEDT;
import static pixelitor.utils.Threads.onPool;

/**
 * The panel shown in the "Export Optimized JPEG..." dialog
 */
public class OptimizedJpegExportPanel extends JPanel {
    private static final int GRID_GAP = 10;

    private final BufferedImage sourceImage;

    private ImagePanel original;
    private ImagePanel optimized;

    private RangeParam qualityParam;
    private JLabel sizeLabel;
    private JCheckBox progressiveCB;
    private ProgressPanel progressPanel;

    private OptimizedJpegExportPanel(BufferedImage image) {
        super(new BorderLayout(3, 3));

        this.sourceImage = image;

        add(createComparePanel(image), CENTER);
        add(createSettingsPanel(), SOUTH);

        updatePreviewAsync();
    }

    private JPanel createComparePanel(BufferedImage image) {
        JPanel comparePanel = new JPanel(new GridLayout(1, 2, GRID_GAP, GRID_GAP));
        var imageSize = new Dimension(image.getWidth(), image.getHeight());

        original = createImagePanel(imageSize);
        original.replaceImage(image);

        optimized = createImagePanel(imageSize);

        setupScrollPanes(comparePanel);

        return comparePanel;
    }

    private void setupScrollPanes(JPanel comparePanel) {
        JScrollPane originalSP = createScrollPane(original, "Original");
        JScrollPane optimizedSP = createScrollPane(optimized, "Optimized");

        comparePanel.add(originalSP);
        comparePanel.add(optimizedSP);

        GUIUtils.synchronizeScrollPanes(originalSP, optimizedSP);
    }

    private static JScrollPane createScrollPane(ImagePanel original, String borderTitle) {
        JScrollPane scrollPane = new JScrollPane(original);
        HandToolSupport.addBehavior(scrollPane);
        scrollPane.setBorder(createTitledBorder(borderTitle));

        return scrollPane;
    }

    private static ImagePanel createImagePanel(Dimension imageSize) {
        // no checkerboard, because here transparency is shown as black
        var viewPanel = new ImagePanel(false);

        viewPanel.setPreferredSize(imageSize);

        return viewPanel;
    }

    private JPanel createSettingsPanel() {
        JPanel p = new JPanel(new FlowLayout(LEFT));

        // progressive encoding
        p.add(new JLabel("Progressive:"));
        progressiveCB = new JCheckBox("", false);
        progressiveCB.addActionListener(e -> updatePreviewAsync());
        p.add(progressiveCB);

        // quality slider
        qualityParam = new RangeParam("  JPEG Quality", 1, 60, 100);
        qualityParam.setAdjustmentListener(this::updatePreviewAsync);
        p.add(new SliderSpinner(qualityParam, WEST, false));

        // file size preview
        sizeLabel = new JLabel();
        p.add(sizeLabel);

        // spacing
        p.add(Box.createRigidArea(new Dimension(40, 10)));

        // progress indicator
        progressPanel = new ProgressPanel();
        p.add(progressPanel);

        return p;
    }

    private void updatePreviewAsync() {
        CompletableFuture
            .supplyAsync(() -> generatePreview(getQuality(), isProgressive()), onPool)
            .thenAcceptAsync(this::updatePreview, onEDT)
            .exceptionally(Messages::showExceptionOnEDT);
    }

    private PreviewInfo generatePreview(float quality, boolean progressive) {
        return writeJPGtoPreviewImage(sourceImage,
            JpegSettings.createJpegCustomizer(quality, progressive),
            new JProgressBarTracker(progressPanel));
    }

    private void updatePreview(PreviewInfo previewInfo) {
        optimized.refreshImage(previewInfo.image());
        sizeLabel.setText("  Size: "
            + MemoryInfo.bytesToString(previewInfo.sizeInBytes()));
    }

    private float getQuality() {
        return (float) qualityParam.getPercentage();
    }

    private boolean isProgressive() {
        return progressiveCB.isSelected();
    }

    public static void showInDialog(Composition comp, String title) {
        var image = comp.getCompositeImage();
        var rgbImage = ImageUtils.convertToRGB(image, false);
        var exportPanel = new OptimizedJpegExportPanel(rgbImage);

        new DialogBuilder()
            .content(exportPanel)
            .title(title)
            .okText(i18n("save"))
            .okAction(() -> FileIO.saveJpegWithCustomSettings(comp,
                exportPanel.getQuality(), exportPanel.isProgressive()))
            .show();
    }

    private static PreviewInfo writeJPGtoPreviewImage(BufferedImage image,
                                                      Consumer<ImageWriteParam> customizer,
                                                      ProgressTracker pt) {
        var bos = new ByteArrayOutputStream(32768);
        BufferedImage previewImage = null;
        byte[] bytes = null;
        try {
            // Step 1: write JPEG with the given settings to memory.
            // Approximately 70% of the total time is spent here.
            ImageOutputStream ios = ImageIO.createImageOutputStream(bos);
            var pt1 = new SubtaskProgressTracker(0.7, pt);
            TrackedIO.writeToIOS(image, ios, "jpg", pt1, customizer);

            // Step 2: Read it back into the preview image.
            // Approximately 30% of the total time is spent here.
            bytes = bos.toByteArray();
            var in = new ByteArrayInputStream(bytes);
            var pt2 = new SubtaskProgressTracker(0.3, pt);
            try (ImageInputStream iis = ImageIO.createImageInputStream(in)) {
                previewImage = TrackedIO.readFromIIS(iis, pt2);
            }

            pt.finished();
        } catch (IOException e) {
            Messages.showException(e);
        }

        return new PreviewInfo(previewImage, bytes.length);
    }

    /**
     * An image and its estimated (jpeg-compressed) disk size.
     */
    private record PreviewInfo(BufferedImage image, int sizeInBytes) {
    }
}


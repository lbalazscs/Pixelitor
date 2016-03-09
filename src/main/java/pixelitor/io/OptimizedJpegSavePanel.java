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

package pixelitor.io;

import pixelitor.filters.gui.AddDefaultButton;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.ImagePanel;
import pixelitor.gui.utils.OKCancelDialog;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.tools.HandToolSupport;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;

import static pixelitor.gui.utils.SliderSpinner.TextPosition.WEST;

/**
 * The panel shown in the "Export Optimized JPEG..." dialog
 */
public class OptimizedJpegSavePanel extends JPanel {
    private final BufferedImage image;
    private ImagePanel optimized;
    private RangeParam qualityParam;
    private JLabel sizeLabel;
    private ImagePanel original;

    private OptimizedJpegSavePanel(BufferedImage image) {
        this.image = image;

        JPanel controlsPanel = createControlsPanel(); // must be constructed before the comparePanel
        JPanel comparePanel = createComparePanel(image);

        setLayout(new BorderLayout(3, 3));
        add(comparePanel, BorderLayout.CENTER);
        add(controlsPanel, BorderLayout.SOUTH);
    }

    private JPanel createComparePanel(BufferedImage image) {
        JPanel comparePanel = new JPanel();
        comparePanel.setLayout(new GridLayout(1, 2, 10, 10));
        Dimension imageSize = new Dimension(image.getWidth(), image.getHeight());

        original = createViewPanel(imageSize);
        original.setImage(image);

        optimized = createViewPanel(imageSize);
        updateAfterPreview(); // to set a first preview image

        setupScrollPanes(comparePanel);

        return comparePanel;
    }

    private void setupScrollPanes(JPanel comparePanel) {
        JScrollPane originalSP = createScrollPane(original, "Original");
        JScrollPane optimizedSP = createScrollPane(optimized, "Optimized");

        comparePanel.add(originalSP);
        comparePanel.add(optimizedSP);

        optimizedSP.getVerticalScrollBar().setModel(originalSP.getVerticalScrollBar().getModel());
        optimizedSP.getHorizontalScrollBar().setModel(originalSP.getHorizontalScrollBar().getModel());
    }

    private static JScrollPane createScrollPane(ImagePanel original, String borderTitle) {
        JScrollPane sp = new JScrollPane(original);
        HandToolSupport.addBehavior(sp);
        sp.setBorder(BorderFactory.createTitledBorder(borderTitle));

        return sp;
    }

    private static ImagePanel createViewPanel(Dimension imageSize) {
        // no checkerboard, because here it is a black image
        ImagePanel view = new ImagePanel(false);

        view.setPreferredSize(imageSize);

        return view;
    }

    private JPanel createControlsPanel() {
        qualityParam = new RangeParam("JPEG Quality", 1, 60, 100);
        qualityParam.setAdjustmentListener(this::updateAfterPreview);
        SliderSpinner qualitySpinner = new SliderSpinner(qualityParam, WEST, AddDefaultButton.NO);
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        southPanel.add(qualitySpinner);
        sizeLabel = new JLabel();
        southPanel.add(sizeLabel);

        return southPanel;
    }

    private void updateAfterPreview() {
        float quality = getSelectedQuality();

        JpegOutput.ImageWithSize[] imageWithSize = new JpegOutput.ImageWithSize[1];
        Runnable task = () -> imageWithSize[0] = JpegOutput.writeJPGtoPreviewImage(this.image, quality);
        Utils.executeWithBusyCursor(this, task);

        BufferedImage newPreview = imageWithSize[0].getImage();
        optimized.updateImage(newPreview);

        int numBytes = imageWithSize[0].getSize();
        sizeLabel.setText("Size: " + Utils.bytesToString(numBytes));
    }

    private float getSelectedQuality() {
        return qualityParam.getValueAsPercentage();
    }

    public static void showInDialog(BufferedImage image, JFrame frame) {
        BufferedImage rgbImage = ImageUtils.convertToRGB(image, false);

        OptimizedJpegSavePanel p = new OptimizedJpegSavePanel(rgbImage);
        OKCancelDialog d = new OKCancelDialog(p, frame, "Save Optimized JPEG", "Save", "Cancel", false) {
            @Override
            protected void dialogAccepted() {
                close();
                float quality = p.getSelectedQuality();
                OpenSaveManager.saveJpegWithQuality(quality);
            }
        };
        d.setVisible(true);
    }

}


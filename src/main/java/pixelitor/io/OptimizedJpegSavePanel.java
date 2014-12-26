/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.io;

import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.RangeParam;
import pixelitor.tools.HandToolSupport;
import pixelitor.utils.Dialogs;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.OKCancelDialog;
import pixelitor.utils.SliderSpinner;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;

/**
 *
 */
public class OptimizedJpegSavePanel extends JPanel {
    private final BufferedImage image;
    private final ImagePanel optimized;
    private final RangeParam qualityParam;
    private final JLabel sizeLabel;

    public OptimizedJpegSavePanel(BufferedImage image) {
        this.image = image;
        qualityParam = new RangeParam("JPEG Quality", 1, 100, 60);
        sizeLabel = new JLabel();

        JPanel comparePanel = new JPanel();
        comparePanel.setLayout(new GridLayout(1, 2, 10, 10));

        ImagePanel original = new ImagePanel(image);
        optimized = new ImagePanel();

        Dimension imageSize = new Dimension(image.getWidth(), image.getHeight());
        original.setPreferredSize(imageSize);
        optimized.setPreferredSize(imageSize);

        updateAfterPreview();


        JScrollPane originalSP = new JScrollPane(original);
        comparePanel.add(originalSP);
        HandToolSupport.addBehavior(originalSP);

        JScrollPane optimizedSP = new JScrollPane(optimized);
        comparePanel.add(optimizedSP);
        HandToolSupport.addBehavior(optimizedSP);

        originalSP.setBorder(BorderFactory.createTitledBorder("Original"));
        optimizedSP.setBorder(BorderFactory.createTitledBorder("Optimized"));


        optimizedSP.getVerticalScrollBar().setModel(originalSP.getVerticalScrollBar().getModel());
        optimizedSP.getHorizontalScrollBar().setModel(originalSP.getHorizontalScrollBar().getModel());


        setLayout(new BorderLayout(3, 3));
        add(comparePanel, BorderLayout.CENTER);

        qualityParam.setAdjustmentListener(new ParamAdjustmentListener() {
            @Override
            public void paramAdjusted() {
                updateAfterPreview();
            }
        });

        SliderSpinner qualitySpinner = new SliderSpinner(qualityParam, false, SliderSpinner.TextPosition.WEST);
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        southPanel.add(qualitySpinner);

        southPanel.add(sizeLabel);

        add(southPanel, BorderLayout.SOUTH);
    }

    private void updateAfterPreview() {
        final float quality = getSelectedQuality();

        final JpegOutput.ImageWithSize[] imageWithSize = new JpegOutput.ImageWithSize[1];
        Runnable task = new Runnable() {
            @Override
            public void run() {
                imageWithSize[0] = JpegOutput.writeJPGtoPreviewImage(OptimizedJpegSavePanel.this.image, quality);
            }
        };
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

        final OptimizedJpegSavePanel p = new OptimizedJpegSavePanel(rgbImage);
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

class ImagePanel extends JPanel {
    private BufferedImage image;

    ImagePanel() {
        init();
    }

    ImagePanel(BufferedImage image) {
        this.image = image;

        init();
    }

    private void init() {

    }

    public void updateImage(BufferedImage newImage) {
        if (image != null) {
            image.flush();
        }

        image = newImage;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        try {
            g.drawImage(image, 0, 0, null);
        } catch (OutOfMemoryError e) {
            Dialogs.showOutOfMemoryDialog();
        }
    }


}
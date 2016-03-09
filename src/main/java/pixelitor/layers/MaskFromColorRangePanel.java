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

package pixelitor.layers;

import pixelitor.filters.gui.AddDefaultButton;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.ColorPickerThumbnailPanel;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.ImagePanel;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;

import static java.awt.AlphaComposite.DstIn;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.WEST;
import static pixelitor.layers.LayerMask.RUBYLITH_COLOR_MODEL;
import static pixelitor.layers.LayerMask.RUBYLITH_COMPOSITE;
import static pixelitor.layers.LayerMask.TRANSPARENCY_COLOR_MODEL;

/**
 * Mask from Color Range
 */
public class MaskFromColorRangePanel extends JPanel {
    public static final String NAME = "Mask from Color Range";
    private static final int DEFAULT_THUMB_SIZE = 512;
    private static final String HELP_TEXT = "Select a color by clicking or dragging on the image";
    private static final String PREVIEW_MODE_MASK = "Mask";
    private static final String PREVIEW_MODE_BLACK_MATTE = "Black Matte";
    private static final String PREVIEW_MODE_WHITE_MATTE = "White Matte";
    private static final String PREVIEW_MODE_RUBYLITH = "Rubylith";

    private final JComboBox<String> colorSpaceCB = new JComboBox(
            new IntChoiceParam.Value[]{
                    new IntChoiceParam.Value("HSB", MaskFromColorRangeFilter.MODE_HSB),
                    new IntChoiceParam.Value("RGB", MaskFromColorRangeFilter.MODE_RGB),
            });
    private final RangeParam tolerance = new RangeParam("Tolerance", 0, 10, 150);
    private final RangeParam softness = new RangeParam("   Softness", 0, 10, 100);
    private final JCheckBox invertCB = new JCheckBox();

    private int lastPickerWidth;
    private int lastPickerHeight;

    private final ImagePanel previewPanel = new ImagePanel(false);
    private final BufferedImage image;
    private BufferedImage thumb;
    private Color lastColor;

    private final JComboBox<String> previewModeCB = new JComboBox(
            new String[]{PREVIEW_MODE_MASK,
                    PREVIEW_MODE_BLACK_MATTE,
                    PREVIEW_MODE_WHITE_MATTE,
                    PREVIEW_MODE_RUBYLITH});


    private MaskFromColorRangePanel(BufferedImage image) {
        super(new BorderLayout());
        this.image = image;

        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        northPanel.add(new JLabel("Preview Mode:"));
        northPanel.add(previewModeCB);
        add(northPanel, BorderLayout.NORTH);

        JPanel imagesPanel = new JPanel(new GridLayout(1, 2, 5, 5));

        Dimension thumbDim = ImageUtils.calcThumbDimensions(image, DEFAULT_THUMB_SIZE);

        ColorPickerThumbnailPanel colorPickerPanel =
                new ColorPickerThumbnailPanel(thumb, (c) -> {
                    lastColor = c;
                    updatePreview(c);
                });

        colorPickerPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int newWidth = colorPickerPanel.getWidth();
                int newHeight = colorPickerPanel.getHeight();

                if (newWidth == lastPickerWidth && newHeight == lastPickerHeight) {
                    return;
                }
                lastPickerWidth = newWidth;
                lastPickerHeight = newHeight;

                thumb = ImageUtils.createThumbnail(image, newWidth, newHeight, null);

                colorPickerPanel.setImage(thumb);
                colorPickerPanel.repaint();
                updatePreview(lastColor);
            }
        });

        colorPickerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        colorPickerPanel.setPreferredSize(thumbDim);
        previewPanel.setPreferredSize(thumbDim);

        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createTitledBorder(HELP_TEXT));
        left.add(colorPickerPanel, BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(BorderFactory.createTitledBorder("Preview"));
        right.add(previewPanel, BorderLayout.CENTER);

        imagesPanel.add(left);
        imagesPanel.add(right);

        add(imagesPanel, BorderLayout.CENTER);
        JPanel southPanel = new JPanel(new BorderLayout());

        JPanel southCenterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JPanel southEastPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        southPanel.add(southCenterPanel, BorderLayout.CENTER);
        southPanel.add(southEastPanel, BorderLayout.EAST);

        SliderSpinner toleranceSlider = new SliderSpinner(tolerance, WEST, AddDefaultButton.NO);
        SliderSpinner softnessSlider = new SliderSpinner(softness, WEST, AddDefaultButton.NO);
        southCenterPanel.add(toleranceSlider);
        southCenterPanel.add(softnessSlider);
        southCenterPanel.add(new JLabel("   Invert:"));
        southCenterPanel.add(invertCB);

        southEastPanel.add(new JLabel("   Color Space:"));
        southEastPanel.add(colorSpaceCB);

        ChangeListener changeListener = e -> updatePreview(lastColor);
        toleranceSlider.addChangeListener(changeListener);
        softnessSlider.addChangeListener(changeListener);
        ActionListener actionListener = e -> updatePreview(lastColor);
        invertCB.addActionListener(actionListener);
        previewModeCB.addActionListener(actionListener);
        colorSpaceCB.addActionListener(actionListener);

        add(southPanel, BorderLayout.SOUTH);
    }

    private MaskFromColorRangeFilter createFilterFromSettings(Color c) {
        MaskFromColorRangeFilter filter = new MaskFromColorRangeFilter(NAME);

        IntChoiceParam.Value colorSpace = (IntChoiceParam.Value) colorSpaceCB.getSelectedItem();
        filter.setMode(colorSpace.getIntValue());
        filter.setColor(c);
        filter.setTolerance(tolerance.getValue(), softness.getValueAsPercentage());
        filter.setInvert(invertCB.isSelected());
        return filter;
    }

    private void updatePreview(Color c) {
        if (c == null) {
            return; // the color was not set yet
        }
        MaskFromColorRangeFilter filter = createFilterFromSettings(c);
        BufferedImage rgbMask = filter.filter(thumb, null);

        String previewMode = (String) previewModeCB.getSelectedItem();

        if (previewMode.equals(PREVIEW_MODE_MASK)) {
            previewPanel.updateImage(rgbMask);
        } else if (previewMode.equals(PREVIEW_MODE_RUBYLITH)) {
            BufferedImage grayMask = ImageUtils.convertToGrayScaleImage(rgbMask);
            BufferedImage ruby = new BufferedImage(RUBYLITH_COLOR_MODEL, grayMask.getRaster(), false, null);
            BufferedImage thumbWithRuby = ImageUtils.copyImage(thumb);
            Graphics2D g = thumbWithRuby.createGraphics();
            g.setComposite(RUBYLITH_COMPOSITE);
            g.drawImage(ruby, 0, 0, null);
            g.dispose();
            previewPanel.updateImage(thumbWithRuby);
        } else {
            BufferedImage grayMask = ImageUtils.convertToGrayScaleImage(rgbMask);
            BufferedImage transparencyImage = new BufferedImage(TRANSPARENCY_COLOR_MODEL, grayMask.getRaster(), false, null);

            BufferedImage thumbWithTransparency = ImageUtils.copyImage(thumb);
            Graphics2D g = thumbWithTransparency.createGraphics();
            g.setComposite(DstIn);
            g.drawImage(transparencyImage, 0, 0, null);
            g.dispose();

            BufferedImage preview = ImageUtils.createSysCompatibleImage(rgbMask.getWidth(), rgbMask.getHeight());
            Graphics2D previewG = preview.createGraphics();
            switch (previewMode) {
                case PREVIEW_MODE_BLACK_MATTE:
                    previewG.setColor(Color.BLACK);
                    break;
                case PREVIEW_MODE_WHITE_MATTE:
                    previewG.setColor(Color.WHITE);
                    break;
                default:
                    throw new IllegalStateException("previewMode = " + previewMode);
            }
            previewG.fillRect(0, 0, preview.getWidth(), preview.getHeight());
            previewG.drawImage(thumbWithTransparency, 0, 0, null);
            previewG.dispose();

            previewPanel.updateImage(preview);
        }
    }

    private BufferedImage getMaskImage() {
        MaskFromColorRangeFilter filter = createFilterFromSettings(lastColor);

        BufferedImage rgbMask = filter.filter(image, null);
        BufferedImage grayMask = ImageUtils.convertToGrayScaleImage(rgbMask);

        return grayMask;
    }

    private Color getLastColor() {
        return lastColor;
    }

    public static void showInDialog(Layer layer, BufferedImage image) {
        MaskFromColorRangePanel form = new MaskFromColorRangePanel(image);

        String okText = layer.hasMask() ? "Replace Mask" : "Add Mask";

        new DialogBuilder()
                .title(NAME)
                .form(form)
                .okText(okText)
                .okAction(() -> {
                    BufferedImage maskImage = form.getMaskImage();
                    layer.addOrReplaceMaskImage(maskImage, NAME);
                })
                .validator(d -> {
                    if (form.getLastColor() == null) {
                        Dialogs.showInfoDialog(d, "No color selected", HELP_TEXT);
                        return false;
                    }
                    return true;
                })
                .show();
    }
}

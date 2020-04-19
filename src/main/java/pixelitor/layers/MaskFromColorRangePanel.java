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

package pixelitor.layers;

import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.filters.gui.IntChoiceParam.Value;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.ColorPickerThumbnailPanel;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.ImagePanel;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.utils.Cursors;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;

import static java.awt.AlphaComposite.DstIn;
import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.EAST;
import static java.awt.BorderLayout.NORTH;
import static java.awt.BorderLayout.SOUTH;
import static java.awt.BorderLayout.WEST;
import static java.awt.FlowLayout.LEFT;
import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.NONE;
import static pixelitor.layers.LayerMask.RUBYLITH_COLOR_MODEL;
import static pixelitor.layers.LayerMask.RUBYLITH_COMPOSITE;
import static pixelitor.layers.LayerMask.TRANSPARENCY_COLOR_MODEL;
import static pixelitor.utils.ImageUtils.calcThumbDimensions;
import static pixelitor.utils.ImageUtils.convertToGrayScaleImage;
import static pixelitor.utils.ImageUtils.copyImage;
import static pixelitor.utils.ImageUtils.createSysCompatibleImage;
import static pixelitor.utils.ImageUtils.createThumbnail;

/**
 * The GUI for "Mask from Color Range"
 */
public class MaskFromColorRangePanel extends JPanel {
    public static final String NAME = "Mask from Color Range";
    private static final int DEFAULT_THUMB_SIZE = 512;
    private static final String HELP_TEXT = "Select a color by clicking or dragging on the image";

    private static final String PREVIEW_MODE_MASK = "Mask";
    private static final String PREVIEW_MODE_BLACK_MATTE = "Black Matte";
    private static final String PREVIEW_MODE_WHITE_MATTE = "White Matte";
    private static final String PREVIEW_MODE_RUBYLITH = "Rubylith";
    private final JComboBox<String> previewModeCB = new JComboBox<>(
            new String[]{PREVIEW_MODE_MASK,
                    PREVIEW_MODE_BLACK_MATTE,
                    PREVIEW_MODE_WHITE_MATTE,
                    PREVIEW_MODE_RUBYLITH});

    private static final String MASK_BASE_LAYER = "Current Layer";
    private static final String MASK_BASE_COMP = "Composite Image";
    private final JComboBox<String> maskBaseCB = new JComboBox<>(
            new String[]{MASK_BASE_LAYER, MASK_BASE_COMP});

    private JComboBox<Value> distTypeCombo;
    private final RangeParam tolerance = new RangeParam("Tolerance", 0, 10, 150);
    private final RangeParam softness = new RangeParam("   Softness", 0, 10, 100);
    private JCheckBox invertCheckBox;

    private int lastPickerWidth;
    private int lastPickerHeight;

    private final ImagePanel previewPanel = new ImagePanel(false);

    private BufferedImage srcImage;
    private boolean srcBasedOnLayer = true;

    private BufferedImage thumb;
    private Color lastColor;
    private final Layer layer;
    private ColorPickerThumbnailPanel colorPickerPanel;

    private MaskFromColorRangePanel(Composition comp, Layer layer) {
        super(new BorderLayout());
        this.layer = layer;

        // by default use the layer image
        srcImage = layer.getRepresentingImage();
        srcBasedOnLayer = true;
        maskBaseCB.addActionListener(e -> maskBaseChanged(comp));

        createInvertCheckBox();
        createColorSpaceComboBox();

        add(createNorthPanel(), NORTH);
        add(createImagesPanel(srcImage), CENTER);
        add(createSouthPanel(), SOUTH);
    }

    private void maskBaseChanged(Composition comp) {
        Object selectedMaskBase = maskBaseCB.getSelectedItem();
        if (selectedMaskBase.equals(MASK_BASE_LAYER) && !srcBasedOnLayer) {
            // change to layer-based
            srcImage = layer.getRepresentingImage();
            srcBasedOnLayer = true;
            changeColorPickerImage(true);
        } else if (srcBasedOnLayer) {
            // change to composite-based
            srcImage = comp.getCompositeImage();
            srcBasedOnLayer = false;
            changeColorPickerImage(true);
        }
    }

    private void createInvertCheckBox() {
        invertCheckBox = new JCheckBox();
        invertCheckBox.setName("invertCheckBox");
    }

    private void createColorSpaceComboBox() {
        distTypeCombo = new JComboBox<>(new Value[]{
                new Value("HSB", MaskFromColorRangeFilter.HSB),
                new Value("Hue", MaskFromColorRangeFilter.HUE),
                new Value("Sat", MaskFromColorRangeFilter.SAT),
                new Value("RGB", MaskFromColorRangeFilter.RGB),
        });
        distTypeCombo.setName("distTypeCombo");
    }

    private JPanel createNorthPanel() {
        JPanel northPanel = new JPanel(new BorderLayout());

        JPanel leftPanel = new JPanel();
        leftPanel.add(new JLabel("Image Source:"));
        leftPanel.add(maskBaseCB);

        JPanel rightPanel = new JPanel();
        rightPanel.add(new JLabel("Preview Mode:"));
        rightPanel.add(previewModeCB);

        northPanel.add(leftPanel, WEST);
        northPanel.add(rightPanel, EAST);

        return northPanel;
    }

    private JPanel createImagesPanel(BufferedImage image) {
        Dimension thumbDim = calcThumbDimensions(image.getWidth(), image.getHeight(), DEFAULT_THUMB_SIZE);

        createColorPickerPanel(image, thumbDim);
        previewPanel.setPreferredSize(thumbDim);

        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(createTitledBorder(HELP_TEXT));
        left.add(colorPickerPanel, CENTER);

        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(createTitledBorder("Preview"));
        right.add(previewPanel, CENTER);

        JPanel imagesPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        imagesPanel.add(left);
        imagesPanel.add(right);
        return imagesPanel;
    }

    private void createColorPickerPanel(BufferedImage image, Dimension thumbDim) {
        colorPickerPanel = new ColorPickerThumbnailPanel(thumb, c -> {
            lastColor = c;
            updatePreview(c);
        });

        colorPickerPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                changeColorPickerImage(false);
            }
        });

        colorPickerPanel.setCursor(Cursors.CROSSHAIR);
        colorPickerPanel.setPreferredSize(thumbDim);
    }

    private void changeColorPickerImage(boolean force) {
        int newWidth = colorPickerPanel.getWidth();
        int newHeight = colorPickerPanel.getHeight();

        if (!force && newWidth == lastPickerWidth && newHeight == lastPickerHeight) {
            return;
        }
        lastPickerWidth = newWidth;
        lastPickerHeight = newHeight;

        thumb = createThumbnail(srcImage, newWidth, newHeight, null);

        colorPickerPanel.changeImage(thumb);
        updatePreview(lastColor);
    }

    private JPanel createSouthPanel() {
        JPanel southPanel = new JPanel(new BorderLayout());

        JPanel southCenterPanel = new JPanel(new GridBagLayout());
        JPanel southEastPanel = new JPanel(new FlowLayout(LEFT, 5, 5));
        southPanel.add(southCenterPanel, CENTER);
        southPanel.add(southEastPanel, EAST);

        SliderSpinner toleranceSlider = new SliderSpinner(tolerance, NONE, false);
        toleranceSlider.setName("toleranceSlider");

        SliderSpinner softnessSlider = new SliderSpinner(softness, NONE, false);
        softnessSlider.setName("softnessSlider");

        var gbh = new GridBagHelper(southCenterPanel);
        gbh.addLabelAndControlNoStretch(tolerance.getName(), toleranceSlider);
        gbh.addLabelAndControlNoStretch(softness.getName(), softnessSlider);
        gbh.addLabelAndControl("Invert:", invertCheckBox);

//        southCenterPanel.add(toleranceSlider);
//        southCenterPanel.add(softnessSlider);
//        southCenterPanel.add(new JLabel("   Invert:"));
//        southCenterPanel.add(invertCheckBox);

        southEastPanel.add(new JLabel("   Distance:"));
        southEastPanel.add(distTypeCombo);

        ChangeListener changeListener = e -> updatePreview(lastColor);
        toleranceSlider.addChangeListener(changeListener);
        softnessSlider.addChangeListener(changeListener);
        ActionListener actionListener = e -> updatePreview(lastColor);
        invertCheckBox.addActionListener(actionListener);
        previewModeCB.addActionListener(actionListener);
        distTypeCombo.addActionListener(actionListener);

        return southPanel;
    }

    private boolean validate(JDialog d) {
        if (getLastColor() == null) {
            Dialogs.showInfoDialog(d, "No color selected", HELP_TEXT);
            return false;
        }
        return true;
    }

    private MaskFromColorRangeFilter createFilterFromSettings(Color c) {
        MaskFromColorRangeFilter filter = new MaskFromColorRangeFilter(NAME);

        int distType = ((Value) distTypeCombo.getSelectedItem()).getValue();
        filter.setDistType(distType);
        filter.setColor(c);
        filter.setTolerance(tolerance.getValue(), softness.getPercentageValF());
        filter.setInvert(invertCheckBox.isSelected());
        return filter;
    }

    private void updatePreview(Color c) {
        if (c == null) {
            return; // the color was not set yet
        }
        MaskFromColorRangeFilter filter = createFilterFromSettings(c);
        BufferedImage rgbMask = filter.filter(thumb, null);

        String previewMode = (String) previewModeCB.getSelectedItem();

        switch (previewMode) {
            case PREVIEW_MODE_MASK:
                previewPanel.changeImage(rgbMask);
                break;
            case PREVIEW_MODE_RUBYLITH:
                updateRubyPreview(rgbMask);
                break;
            case PREVIEW_MODE_BLACK_MATTE:
                updateMattePreview(rgbMask, Color.BLACK);
                break;
            case PREVIEW_MODE_WHITE_MATTE:
                updateMattePreview(rgbMask, Color.WHITE);
                break;
            default:
                throw new IllegalStateException("previewMode = " + previewMode);
        }
    }

    private void updateRubyPreview(BufferedImage rgbMask) {
        BufferedImage grayMask = convertToGrayScaleImage(rgbMask);
        BufferedImage ruby = new BufferedImage(RUBYLITH_COLOR_MODEL,
                grayMask.getRaster(), false, null);
        BufferedImage thumbWithRuby = copyImage(thumb);
        Graphics2D g = thumbWithRuby.createGraphics();
        g.setComposite(RUBYLITH_COMPOSITE);
        g.drawImage(ruby, 0, 0, null);
        g.dispose();
        previewPanel.changeImage(thumbWithRuby);
    }

    private void updateMattePreview(BufferedImage rgbMask, Color matteColor) {
        BufferedImage grayMask = convertToGrayScaleImage(rgbMask);
        BufferedImage transparencyImage = new BufferedImage(
                TRANSPARENCY_COLOR_MODEL, grayMask.getRaster(),
                false, null);

        BufferedImage thumbWithTransparency = copyImage(thumb);
        Graphics2D g = thumbWithTransparency.createGraphics();
        g.setComposite(DstIn);
        g.drawImage(transparencyImage, 0, 0, null);
        g.dispose();

        BufferedImage preview = createSysCompatibleImage(
                rgbMask.getWidth(), rgbMask.getHeight());
        Graphics2D previewG = preview.createGraphics();
        previewG.setColor(matteColor);
        previewG.fillRect(0, 0, preview.getWidth(), preview.getHeight());
        previewG.drawImage(thumbWithTransparency, 0, 0, null);
        previewG.dispose();

        previewPanel.changeImage(preview);
    }

    private BufferedImage getMaskImage() {
        MaskFromColorRangeFilter filter = createFilterFromSettings(lastColor);

        BufferedImage rgbMask = filter.filter(srcImage, null);
        BufferedImage grayMask = convertToGrayScaleImage(rgbMask);

        return grayMask;
    }

    private Color getLastColor() {
        return lastColor;
    }

    public static void showInDialog() {
        var comp = OpenImages.getActiveComp();
        Layer layer = comp.getActiveLayer();

        MaskFromColorRangePanel panel = new MaskFromColorRangePanel(comp, layer);

        String okText = layer.hasMask() ? "Replace Mask" : "Add Mask";

        new DialogBuilder()
                .title(NAME)
                .content(panel)
                .okText(okText)
                .okAction(() -> {
                    BufferedImage maskImage = panel.getMaskImage();
                    layer.addOrReplaceMaskImage(maskImage, NAME);
                })
                .validator(panel::validate)
                .show();
    }
}

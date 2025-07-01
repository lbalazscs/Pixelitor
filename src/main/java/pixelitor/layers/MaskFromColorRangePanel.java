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

package pixelitor.layers;

import pixelitor.Composition;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.*;
import pixelitor.utils.Cursors;
import pixelitor.utils.Texts;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
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
import static pixelitor.gui.utils.SliderSpinner.LabelPosition.NONE;
import static pixelitor.layers.LayerMask.RUBYLITH_COLOR_MODEL;
import static pixelitor.layers.LayerMask.RUBYLITH_COMPOSITE;
import static pixelitor.layers.LayerMask.TRANSPARENCY_COLOR_MODEL;
import static pixelitor.utils.ImageUtils.calcThumbDimensions;
import static pixelitor.utils.ImageUtils.convertToGrayscaleImage;
import static pixelitor.utils.ImageUtils.copyImage;
import static pixelitor.utils.ImageUtils.copySubImage;
import static pixelitor.utils.ImageUtils.createSysCompatibleImage;
import static pixelitor.utils.ImageUtils.createThumbnail;
import static pixelitor.utils.ImageUtils.isSubImage;

/**
 * GUI panel for creating layer masks based on color similarity.
 */
public class MaskFromColorRangePanel extends JPanel {
    public static final String NAME = Texts.i18n("lm_add_from_range");
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

    private static final String IMG_SRC_LAYER = "Current Layer";
    private static final String IMG_SRC_COMP = "Composite Image";
    private final JComboBox<String> imageSourceCB = new JComboBox<>(
        new String[]{IMG_SRC_LAYER, IMG_SRC_COMP});

    private JComboBox<Item> distMetricCombo;
    private final RangeParam tolerance = new RangeParam("Tolerance", 0, 10, 150);
    private final RangeParam softness = new RangeParam("   Softness", 0, 10, 100);
    private JCheckBox invertMaskCheckBox;

    private int colorPickerWidth;
    private int colorPickerHeight;

    private final ImagePanel previewPanel = new ImagePanel(false);

    // either the layer image or the composite image
    private BufferedImage srcImage;

    private boolean srcIsLayer;

    // a thumbnail-sized version of the source image
    // used for picking a color
    private BufferedImage colorPickerImg;

    private Color referenceColor;
    private final Layer layer;
    private ColorPickerPanel colorPickerPanel;

    private MaskFromColorRangePanel(Composition comp, Layer layer) {
        super(new BorderLayout());
        this.layer = layer;

        // by default use the layer image
        createLayerBasedSourceImage();
        srcIsLayer = true;
        imageSourceCB.addActionListener(e -> updateImageSource(comp));

        createInvertCheckBox();
        createColorSpaceComboBox();

        add(createNorthPanel(), NORTH);
        add(createImagesPanel(srcImage), CENTER);
        add(createSouthPanel(), SOUTH);
    }

    private void updateImageSource(Composition comp) {
        Object selectedSource = imageSourceCB.getSelectedItem();
        if (selectedSource.equals(IMG_SRC_LAYER) && !srcIsLayer) {
            // change to layer-based
            createLayerBasedSourceImage();
            srcIsLayer = true;
            updateColorPickerImage(true);
        } else if (srcIsLayer) {
            // change to composite-based
            srcImage = comp.getCompositeImage();
            srcIsLayer = false;
            updateColorPickerImage(true);
        }
    }

    private void createLayerBasedSourceImage() {
        srcImage = layer.toImage(false, false);
        if (isSubImage(srcImage)) { // can happen for big image layers
            srcImage = copySubImage(srcImage);
        }
    }

    private void createInvertCheckBox() {
        invertMaskCheckBox = new JCheckBox();
        invertMaskCheckBox.setName("invertMaskCheckBox");
    }

    private void createColorSpaceComboBox() {
        distMetricCombo = new JComboBox<>(new Item[]{
            new Item("HSB", MaskFromColorRangeFilter.HSB),
            new Item("Hue", MaskFromColorRangeFilter.HUE),
            new Item("Sat", MaskFromColorRangeFilter.SAT),
            new Item("RGB", MaskFromColorRangeFilter.RGB),
        });
        distMetricCombo.setName("distMetricCombo");
    }

    private JPanel createNorthPanel() {
        JPanel northPanel = new JPanel(new BorderLayout());

        JPanel leftPanel = new JPanel();
        leftPanel.add(new JLabel("Image Source:"));
        leftPanel.add(imageSourceCB);

        JPanel rightPanel = new JPanel();
        rightPanel.add(new JLabel("Preview Mode:"));
        rightPanel.add(previewModeCB);

        northPanel.add(leftPanel, WEST);
        northPanel.add(rightPanel, EAST);

        return northPanel;
    }

    private JPanel createImagesPanel(BufferedImage image) {
        Dimension thumbDim = calcThumbDimensions(
            image.getWidth(), image.getHeight(), DEFAULT_THUMB_SIZE, true);

        createColorPickerPanel(thumbDim);
        previewPanel.setPreferredSize(thumbDim);

        JPanel pickerContainer = new JPanel(new BorderLayout());
        pickerContainer.setBorder(createTitledBorder(HELP_TEXT));
        pickerContainer.add(colorPickerPanel, CENTER);

        JPanel previewContainer = new JPanel(new BorderLayout());
        previewContainer.setBorder(createTitledBorder("Preview"));
        previewContainer.add(previewPanel, CENTER);

        JPanel imagesPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        imagesPanel.add(pickerContainer);
        imagesPanel.add(previewContainer);
        return imagesPanel;
    }

    private void createColorPickerPanel(Dimension size) {
        colorPickerPanel = new ColorPickerPanel(colorPickerImg, this::handleColorSelection);

        colorPickerPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateColorPickerImage(false);
            }
        });

        colorPickerPanel.setCursor(Cursors.CROSSHAIR);
        colorPickerPanel.setPreferredSize(size);
    }

    private void handleColorSelection(Color selectedColor) {
        referenceColor = selectedColor;
        updatePreview(selectedColor);
    }

    private void updateColorPickerImage(boolean forceUpdate) {
        int newWidth = colorPickerPanel.getWidth();
        int newHeight = colorPickerPanel.getHeight();

        if (!forceUpdate && newWidth == colorPickerWidth && newHeight == colorPickerHeight) {
            return;
        }

        colorPickerWidth = newWidth;
        colorPickerHeight = newHeight;

        colorPickerImg = createThumbnail(srcImage, newWidth, newHeight, null);
        colorPickerPanel.refreshImage(colorPickerImg);
        updatePreview(referenceColor);
    }

    private JPanel createSouthPanel() {
        JPanel southPanel = new JPanel(new BorderLayout());

        JPanel southCenterPanel = new JPanel(new GridBagLayout());
        JPanel southEastPanel = new JPanel(new FlowLayout(LEFT, 5, 5));
        southPanel.add(southCenterPanel, CENTER);
        southPanel.add(southEastPanel, EAST);

        var toleranceSlider = new SliderSpinner(tolerance, NONE, false);
        toleranceSlider.setName("toleranceSlider");

        var softnessSlider = new SliderSpinner(softness, NONE, false);
        softnessSlider.setName("softnessSlider");

        var gbh = new GridBagHelper(southCenterPanel);
        gbh.addLabelAndControlNoStretch(tolerance.getName(), toleranceSlider);
        gbh.addLabelAndControlNoStretch(softness.getName(), softnessSlider);
        gbh.addLabelAndControl("Invert:", invertMaskCheckBox);

        southEastPanel.add(new JLabel("   Distance:"));
        southEastPanel.add(distMetricCombo);

        ChangeListener changeListener = e -> updatePreview(referenceColor);
        toleranceSlider.addChangeListener(changeListener);
        softnessSlider.addChangeListener(changeListener);

        ActionListener actionListener = e -> updatePreview(referenceColor);
        invertMaskCheckBox.addActionListener(actionListener);
        previewModeCB.addActionListener(actionListener);
        distMetricCombo.addActionListener(actionListener);

        return southPanel;
    }

    private boolean validate(JDialog d) {
        if (getReferenceColor() == null) {
            Dialogs.showInfoDialog(d, "No color selected", HELP_TEXT);
            return false;
        }
        return true;
    }

    private MaskFromColorRangeFilter createFilterFromSettings(Color c) {
        MaskFromColorRangeFilter filter = new MaskFromColorRangeFilter(NAME);

        int distMetric = ((Item) distMetricCombo.getSelectedItem()).value();
        filter.setDistanceMetric(distMetric);
        filter.setReferenceColor(c);
        filter.setTolerance(tolerance.getValue(), softness.getPercentage());
        filter.setInvertMask(invertMaskCheckBox.isSelected());

        return filter;
    }

    private void updatePreview(Color c) {
        if (c == null) {
            return; // the color was not set yet
        }
        MaskFromColorRangeFilter filter = createFilterFromSettings(c);
        BufferedImage rgbMask = filter.filter(colorPickerImg, null);

        String previewMode = (String) previewModeCB.getSelectedItem();

        switch (previewMode) {
            case PREVIEW_MODE_MASK -> previewPanel.refreshImage(rgbMask);
            case PREVIEW_MODE_RUBYLITH -> updateRubyPreview(rgbMask);
            case PREVIEW_MODE_BLACK_MATTE -> updateMattePreview(rgbMask, Color.BLACK);
            case PREVIEW_MODE_WHITE_MATTE -> updateMattePreview(rgbMask, Color.WHITE);
            default -> throw new IllegalStateException("previewMode = " + previewMode);
        }
    }

    private void updateRubyPreview(BufferedImage rgbMask) {
        BufferedImage grayMask = convertToGrayscaleImage(rgbMask);
        BufferedImage ruby = new BufferedImage(RUBYLITH_COLOR_MODEL,
            grayMask.getRaster(), false, null);
        BufferedImage rubyPreview = copyImage(colorPickerImg);
        Graphics2D g = rubyPreview.createGraphics();
        g.setComposite(RUBYLITH_COMPOSITE);
        g.drawImage(ruby, 0, 0, null);
        g.dispose();
        previewPanel.refreshImage(rubyPreview);
    }

    private void updateMattePreview(BufferedImage rgbMask, Color matteColor) {
        BufferedImage grayMask = convertToGrayscaleImage(rgbMask);
        BufferedImage transparencyImage = new BufferedImage(
            TRANSPARENCY_COLOR_MODEL, grayMask.getRaster(),
            false, null);

        BufferedImage thumbWithTransparency = copyImage(colorPickerImg);
        Graphics2D g = thumbWithTransparency.createGraphics();
        g.setComposite(DstIn);
        g.drawImage(transparencyImage, 0, 0, null);
        g.dispose();

        BufferedImage preview = createSysCompatibleImage(
            rgbMask.getWidth(), rgbMask.getHeight());
        Graphics2D previewG = preview.createGraphics();
        Colors.fillWith(matteColor, previewG, preview.getWidth(), preview.getHeight());
        previewG.drawImage(thumbWithTransparency, 0, 0, null);
        previewG.dispose();

        previewPanel.refreshImage(preview);
    }

    private BufferedImage getMaskImage() {
        MaskFromColorRangeFilter filter = createFilterFromSettings(referenceColor);

        BufferedImage rgbMask = filter.filter(srcImage, null);

        return convertToGrayscaleImage(rgbMask);
    }

    private Color getReferenceColor() {
        return referenceColor;
    }

    public static void showInDialog(Composition comp) {
        var layer = comp.getActiveLayer();
        var panel = new MaskFromColorRangePanel(comp, layer);

        new DialogBuilder()
            .title(NAME)
            .content(panel)
            .okText(layer.hasMask() ? "Replace Mask" : "Add Mask")
            .okAction(() -> {
                BufferedImage maskImage = panel.getMaskImage();
                layer.addOrReplaceMaskImage(maskImage, NAME);
            })
            .validator(panel::validate)
            .show();
    }
}

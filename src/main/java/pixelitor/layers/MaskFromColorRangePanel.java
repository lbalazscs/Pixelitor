/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.colors.Colors;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.*;
import pixelitor.utils.Cursors;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;

import static java.awt.AlphaComposite.DstIn;
import static java.awt.BorderLayout.*;
import static java.awt.FlowLayout.LEFT;
import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.NONE;
import static pixelitor.layers.LayerMask.*;
import static pixelitor.utils.ImageUtils.*;

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

    private static final String IMG_SRC_LAYER = "Current Layer";
    private static final String IMG_SRC_COMP = "Composite Image";
    private final JComboBox<String> imageSourceCB = new JComboBox<>(
        new String[]{IMG_SRC_LAYER, IMG_SRC_COMP});

    private JComboBox<Item> distTypeCombo;
    private final RangeParam tolerance = new RangeParam("Tolerance", 0, 10, 150);
    private final RangeParam softness = new RangeParam("   Softness", 0, 10, 100);
    private JCheckBox invertCheckBox;

    private int lastPickerWidth;
    private int lastPickerHeight;

    private final ImagePanel previewPanel = new ImagePanel(false);

    // either the layer image or the composite image
    private BufferedImage srcImage;

    private boolean srcIsLayer;

    // a thumbnail-sized version of the source image
    // used for picking a color
    private BufferedImage colorPickerImg;

    private Color lastColor;
    private final Layer layer;
    private ColorPickerThumbnailPanel colorPickerPanel;

    private MaskFromColorRangePanel(Composition comp, Layer layer) {
        super(new BorderLayout());
        this.layer = layer;

        // by default use the layer image
        srcImage = layer.asImage(false, false);
        srcIsLayer = true;
        imageSourceCB.addActionListener(e -> imageSourceChanged(comp));

        createInvertCheckBox();
        createColorSpaceComboBox();

        add(createNorthPanel(), NORTH);
        add(createImagesPanel(srcImage), CENTER);
        add(createSouthPanel(), SOUTH);
    }

    private void imageSourceChanged(Composition comp) {
        Object selectedSource = imageSourceCB.getSelectedItem();
        if (selectedSource.equals(IMG_SRC_LAYER) && !srcIsLayer) {
            // change to layer-based
            srcImage = layer.asImage(false, false);
            srcIsLayer = true;
            refreshColorPickerImage(true);
        } else if (srcIsLayer) {
            // change to composite-based
            srcImage = comp.getCompositeImage();
            srcIsLayer = false;
            refreshColorPickerImage(true);
        }
    }

    private void createInvertCheckBox() {
        invertCheckBox = new JCheckBox();
        invertCheckBox.setName("invertCheckBox");
    }

    private void createColorSpaceComboBox() {
        distTypeCombo = new JComboBox<>(new Item[]{
            new Item("HSB", MaskFromColorRangeFilter.HSB),
            new Item("Hue", MaskFromColorRangeFilter.HUE),
            new Item("Sat", MaskFromColorRangeFilter.SAT),
            new Item("RGB", MaskFromColorRangeFilter.RGB),
        });
        distTypeCombo.setName("distTypeCombo");
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
            image.getWidth(), image.getHeight(), DEFAULT_THUMB_SIZE);

        createColorPickerPanel(thumbDim);
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

    private void createColorPickerPanel(Dimension thumbDim) {
        colorPickerPanel = new ColorPickerThumbnailPanel(colorPickerImg, c -> {
            lastColor = c;
            updatePreview(c);
        });

        colorPickerPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                refreshColorPickerImage(false);
            }
        });

        colorPickerPanel.setCursor(Cursors.CROSSHAIR);
        colorPickerPanel.setPreferredSize(thumbDim);
    }

    private void refreshColorPickerImage(boolean force) {
        int newWidth = colorPickerPanel.getWidth();
        int newHeight = colorPickerPanel.getHeight();
        if (!force && newWidth == lastPickerWidth && newHeight == lastPickerHeight) {
            return;
        }
        lastPickerWidth = newWidth;
        lastPickerHeight = newHeight;

        colorPickerImg = createThumbnail(srcImage, newWidth, newHeight, null);
        colorPickerPanel.changeImage(colorPickerImg);
        updatePreview(lastColor);
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
        gbh.addLabelAndControl("Invert:", invertCheckBox);

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

        int distType = ((Item) distTypeCombo.getSelectedItem()).getValue();
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
        BufferedImage rgbMask = filter.filter(colorPickerImg, null);

        String previewMode = (String) previewModeCB.getSelectedItem();

        switch (previewMode) {
            case PREVIEW_MODE_MASK -> previewPanel.changeImage(rgbMask);
            case PREVIEW_MODE_RUBYLITH -> updateRubyPreview(rgbMask);
            case PREVIEW_MODE_BLACK_MATTE -> updateMattePreview(rgbMask, Color.BLACK);
            case PREVIEW_MODE_WHITE_MATTE -> updateMattePreview(rgbMask, Color.WHITE);
            default -> throw new IllegalStateException("previewMode = " + previewMode);
        }
    }

    private void updateRubyPreview(BufferedImage rgbMask) {
        BufferedImage grayMask = convertToGrayScaleImage(rgbMask);
        BufferedImage ruby = new BufferedImage(RUBYLITH_COLOR_MODEL,
            grayMask.getRaster(), false, null);
        BufferedImage rubyPreview = copyImage(colorPickerImg);
        Graphics2D g = rubyPreview.createGraphics();
        g.setComposite(RUBYLITH_COMPOSITE);
        g.drawImage(ruby, 0, 0, null);
        g.dispose();
        previewPanel.changeImage(rubyPreview);
    }

    private void updateMattePreview(BufferedImage rgbMask, Color matteColor) {
        BufferedImage grayMask = convertToGrayScaleImage(rgbMask);
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

        previewPanel.changeImage(preview);
    }

    private BufferedImage getMaskImage() {
        MaskFromColorRangeFilter filter = createFilterFromSettings(lastColor);

        BufferedImage rgbMask = filter.filter(srcImage, null);

        return convertToGrayScaleImage(rgbMask);
    }

    private Color getLastColor() {
        return lastColor;
    }

    public static void showInDialog() {
        var comp = OpenImages.getActiveComp();
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

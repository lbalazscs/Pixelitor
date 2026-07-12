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

import pixelitor.colors.FillType;
import pixelitor.filters.gui.DialogMenuOwner;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.utils.DimensionHelper;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.Validated;
import pixelitor.gui.utils.ValidationResult;
import pixelitor.utils.ResizeUnit;

import javax.swing.*;
import java.awt.GridBagLayout;

import static java.lang.String.format;
import static javax.swing.BorderFactory.createEmptyBorder;
import static pixelitor.utils.MemoryInfo.BYTES_PER_MEGABYTE;

/**
 * The panel in the "New Image" dialog.
 */
public class NewImagePanel extends JPanel implements Validated, DialogMenuOwner, DimensionHelper.DimensionChangeCallback {
    private static final int PANEL_PADDING = 7;

    private final JTextField nameTF;
    private final JComboBox<FillType> fillSelector;

    private final DimensionHelper dimensions;

    public record Settings(int width, int height, int dpi, ResizeUnit unit, FillType fillType, String title) {
    }

    public NewImagePanel(Settings settings) {
        super(new GridBagLayout());
        var gbh = new GridBagHelper(this);

        setBorder(createEmptyBorder(PANEL_PADDING, PANEL_PADDING, PANEL_PADDING, PANEL_PADDING));

        nameTF = new JTextField(settings.title());
        nameTF.setName("nameTF");
        gbh.addLabelAndLastControl("Name", nameTF);

        // uses 1 as a dummy value for the original width/height (they are used only by PERCENTAGE)
        dimensions = new DimensionHelper(this, ResizeUnit.ABSOLUTE_UNITS,
            settings.width(), settings.height(), settings.dpi(), 1, 1);

        gbh.addVerticalSpace(PANEL_PADDING);

        var widthLayer = dimensions.createWidthTextField();
        var widthUnitChooser = dimensions.createUnitChooser();
        gbh.addLabelAndTwoControls("Width:", widthLayer, widthUnitChooser);

        var heightLayer = dimensions.createHeightTextField();
        var heightUnitChooser = dimensions.createUnitChooser();
        gbh.addLabelAndTwoControls("Height:", heightLayer, heightUnitChooser);

        var dpiChooser = dimensions.getDpiChooser();
        gbh.addLabelAndLastControl("DPI:", dpiChooser);
        gbh.addVerticalSpace(PANEL_PADDING);

        fillSelector = new JComboBox<>(FillType.values());
        fillSelector.setSelectedItem(settings.fillType());
        gbh.addLabelAndLastControl("Fill:", fillSelector);

        dimensions.getUnitSelectorModel().setSelectedItem(settings.unit());
        dimensions.setInitialValues();
    }

    @Override
    public void dpiChanged() {
        // when DPI changes, physical size is preserved, so both pixel values change
        dimensions.updateWidthText();
        dimensions.updateHeightText();
    }

    @Override
    public void dimensionChanged(boolean isWidthChanged) {
        // no-op, no proportions to keep or border to update
    }

    @Override
    public ValidationResult validateSettings() {
        ValidationResult result = dimensions.checkWidthAndHeight();

        if (!result.isValid()) {
            return result;
        }

        // we know parsing will succeed here because the validation passed
        return validateMemoryFootprint(dimensions.getTargetWidth(), dimensions.getTargetHeight());
    }

    private static ValidationResult validateMemoryFootprint(long width, long height) {
        // issue #49: check approximately whether the image would fit into available memory
        long numPixels = width * height;

        if (numPixels > Integer.MAX_VALUE) {
            // theoretical limit, as the pixels ultimately will be stored in an array
            return ValidationResult.invalid(format(
                "Pixelitor doesn't support images with more than %d pixels." +
                    "<br>%dx%d would be %d pixels.",
                Integer.MAX_VALUE, width, height, numPixels));
        }

        if (numPixels > 1_000_000) { // don't check for smaller images
            Runtime rt = Runtime.getRuntime();
            long allocatedMemory = rt.totalMemory() - rt.freeMemory();
            long availableMemory = rt.maxMemory() - allocatedMemory;
            long requiredMemory = numPixels * 4;

            if (requiredMemory > availableMemory) {
                return ValidationResult.invalid(format(
                    "The image would not fit into memory." +
                        "<br>An image of %dx%d pixels needs at least %d megabytes." +
                        "<br>Available memory is at most %d megabytes.",
                    width, height,
                    requiredMemory / BYTES_PER_MEGABYTE,
                    availableMemory / BYTES_PER_MEGABYTE));
            }
        }

        return ValidationResult.valid();
    }

    /**
     * Returns the new image settings from the panel.
     */
    public Settings getSettings() {
        int width = dimensions.getTargetWidth();
        int height = dimensions.getTargetHeight();
        int dpi = dimensions.getDpi();
        ResizeUnit unit = dimensions.getUnit();
        FillType fillType = getSelectedFill();
        String title = getTitle();

        return new Settings(width, height, dpi, unit, fillType, title);
    }

    private String getTitle() {
        return nameTF.getText().trim();
    }

    private FillType getSelectedFill() {
        return (FillType) fillSelector.getSelectedItem();
    }

    @Override
    public boolean supportsUserPresets() {
        return true;
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        dimensions.saveStateTo(preset);

        preset.put("Fill", getSelectedFill().name());
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        fillSelector.setSelectedItem(preset.getEnum("Fill", FillType.class));

        dimensions.loadUserPreset(preset, false);
    }

    @Override
    public String getPresetDirName() {
        return "New Image";
    }
}

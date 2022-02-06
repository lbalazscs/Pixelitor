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

package pixelitor.gui;

import pixelitor.NewImage;
import pixelitor.colors.FillType;
import pixelitor.filters.gui.DialogMenuOwner;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.TextFieldValidator;
import pixelitor.gui.utils.ValidatedPanel;
import pixelitor.gui.utils.ValidationResult;

import javax.swing.*;
import java.awt.GridBagLayout;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static javax.swing.BorderFactory.createEmptyBorder;
import static pixelitor.utils.Utils.NUM_BYTES_IN_MEGABYTE;

/**
 * The GUI of the "New Image" dialog
 */
public class NewImagePanel extends ValidatedPanel implements DialogMenuOwner {
    private final JTextField widthTF;
    private final JTextField heightTF;

    private static final int BORDER_WIDTH = 5;
    private final JComboBox<FillType> fillSelector;
    private final JTextField nameTF;

    public NewImagePanel() {
        setLayout(new GridBagLayout());
        var gbh = new GridBagHelper(this);

        //noinspection SuspiciousNameCombination
        setBorder(createEmptyBorder(BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH));

        nameTF = new JTextField(NewImage.generateTitle());
        nameTF.setName("nameTF");
        gbh.addLabelAndControl("Name", nameTF);

        widthTF = addTextField("widthTF", "Width:", NewImage.lastSize.width, gbh);
        heightTF = addTextField("heightTF", "Height:", NewImage.lastSize.height, gbh);

        fillSelector = new JComboBox<>(FillType.values());
        gbh.addLabelAndLastControl("Fill:", fillSelector);
    }

    private static JTextField addTextField(String name, String label,
                                           int value, GridBagHelper gbh) {
        var tf = new JTextField(String.valueOf(value));
        tf.setName(name);
        gbh.addLabelAndTwoControls(label,
            TextFieldValidator.createPositiveIntLayer(label, tf, false),
            new JLabel("pixels"));
        return tf;
    }

    @Override
    public ValidationResult checkValidity() {
        var retVal = ValidationResult.ok();
        int width = 0;
        try {
            width = getSelectedWidth();
            retVal = retVal.addErrorIfZero(width, "Width");
            retVal = retVal.addErrorIfNegative(width, "Width");
        } catch (NumberFormatException e) {
            retVal = retVal.addError("The width must be an integer.");
        }
        int height = 0;
        try {
            height = getSelectedHeight();
            retVal = retVal.addErrorIfZero(height, "Height");
            retVal = retVal.addErrorIfNegative(height, "Height");
        } catch (NumberFormatException e) {
            retVal = retVal.addError("The height must be an integer.");
        }

        if (retVal.isOK()) {
            // issue #49: check approximately whether the image
            // would even fit into the available memory
            long numPixels = ((long) width) * height;
            if (numPixels > Integer.MAX_VALUE) {
                // theoretical limit, as the pixels ultimately will be stored in an array
                return retVal.addError(format(
                    "Pixelitor doesn't support images with more than %d pixels." +
                    "<br>%dx%d would be %d pixels.",
                    Integer.MAX_VALUE, width, height, numPixels));
            } else if (numPixels > 1_000_000) { // don't check for smaller images
                Runtime rt = Runtime.getRuntime();
                long allocatedMemory = rt.totalMemory() - rt.freeMemory();
                long availableMemory = rt.maxMemory() - allocatedMemory;
                if (numPixels * 4 > availableMemory) {
                    return retVal.addError(format(
                        "The image would not fit into memory." +
                        "<br>An image of %dx%d pixels needs at least %d megabytes." +
                        "<br>Available memory is at most %d megabytes.",
                        width, height,
                        numPixels * 4 / NUM_BYTES_IN_MEGABYTE,
                        availableMemory / NUM_BYTES_IN_MEGABYTE));
                }
            }
        }

        return retVal;
    }

    public void okPressedInDialog() {
        int selectedWidth = getSelectedWidth();
        int selectedHeight = getSelectedHeight();
        FillType bg = getSelectedFill();

        NewImage.addNewImage(bg, selectedWidth, selectedHeight, getTitle());

        NewImage.lastSize.width = selectedWidth;
        NewImage.lastSize.height = selectedHeight;
    }

    private String getTitle() {
        return nameTF.getText().trim();
    }

    private int getSelectedWidth() {
        return parseInt(widthTF.getText().trim());
    }

    private int getSelectedHeight() {
        return parseInt(heightTF.getText().trim());
    }

    private FillType getSelectedFill() {
        return (FillType) fillSelector.getSelectedItem();
    }

    @Override
    public boolean canHaveUserPresets() {
        return true;
    }

    @Override
    public UserPreset createUserPreset(String presetName) {
        UserPreset preset = new UserPreset(presetName, getPresetDirName());

        preset.putInt("Width", getSelectedWidth());
        preset.putInt("Height", getSelectedHeight());
        preset.put("Fill", getSelectedFill().toString());

        return preset;
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        widthTF.setText(String.valueOf(preset.getInt("Width")));
        heightTF.setText(String.valueOf(preset.getInt("Height")));
        fillSelector.setSelectedItem(preset.getEnum("Fill", FillType.class));
    }

    @Override
    public String getPresetDirName() {
        return "New Image";
    }
}

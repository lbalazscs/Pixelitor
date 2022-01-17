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

package pixelitor;

import pixelitor.colors.FillType;
import pixelitor.filters.Fill;
import pixelitor.gui.utils.*;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static javax.swing.BorderFactory.createEmptyBorder;
import static pixelitor.colors.FillType.TRANSPARENT;
import static pixelitor.utils.Texts.i18n;
import static pixelitor.utils.Utils.BYTES_IN_1_MEGABYTE;

/**
 * Static methods for creating new images
 */
public final class NewImage {
    private static final String NEW_IMAGE_STRING = i18n("new_image");
    private static int untitledCount = 1;

    private static final Dimension lastSize = AppPreferences.getNewImageSize();

    private NewImage() {
    }

    public static Composition addNewImage(FillType bg, int width, int height, String title) {
        var comp = createNewComposition(bg, width, height, title);
        Views.addAsNewComp(comp);
        return comp;
    }

    public static Composition createNewComposition(FillType bg, int width, int height, String title) {
        BufferedImage newImage = ImageUtils.createSysCompatibleImage(width, height);
        fillImage(newImage, bg);
        return Composition.fromImage(newImage, null, title);
    }

    private static void fillImage(BufferedImage img, FillType bg) {
        if (bg == TRANSPARENT) {
            return;
        }
        Color c = bg.getColor();
        Fill.fillImage(img, c);
    }

    private static void showInDialog() {
        var panel = new NewImagePanel();
        new DialogBuilder()
            .title(NEW_IMAGE_STRING)
            .validatedContent(panel)
            .okAction(panel::okPressedInDialog)
            .show();
    }

    public static Action getAction() {
        return new PAction(NEW_IMAGE_STRING + "...") {
            @Override
            public void onClick() {
                showInDialog();
            }
        };
    }

    public static Dimension getLastSize() {
        return lastSize;
    }

    /**
     * The GUI of the "New Image" dialog
     */
    private static class NewImagePanel extends ValidatedPanel {
        private final JTextField widthTF;
        private final JTextField heightTF;

        private static final int BORDER_WIDTH = 5;
        private final JComboBox<FillType> backgroundSelector;
        private final JTextField nameTF;

        private NewImagePanel() {
            setLayout(new GridBagLayout());
            var gbh = new GridBagHelper(this);

            //noinspection SuspiciousNameCombination
            setBorder(createEmptyBorder(BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH));

            nameTF = new JTextField(generateTitle());
            nameTF.setName("nameTF");
            gbh.addLabelAndControl("Name", nameTF);

            widthTF = addTextField("widthTF", "Width:", lastSize.width, gbh);
            heightTF = addTextField("heightTF", "Height:", lastSize.height, gbh);

            backgroundSelector = new JComboBox<>(FillType.values());
            gbh.addLabelAndLastControl("Fill:", backgroundSelector);
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
                            numPixels * 4 / BYTES_IN_1_MEGABYTE,
                            availableMemory / BYTES_IN_1_MEGABYTE));
                    }
                }
            }

            return retVal;
        }

        private void okPressedInDialog() {
            int selectedWidth = getSelectedWidth();
            int selectedHeight = getSelectedHeight();
            FillType bg = getSelectedBackground();

            addNewImage(bg, selectedWidth, selectedHeight, getTitle());

            lastSize.width = selectedWidth;
            lastSize.height = selectedHeight;
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

        private FillType getSelectedBackground() {
            return (FillType) backgroundSelector.getSelectedItem();
        }
    }

    private static String generateTitle() {
        return "Untitled " + untitledCount++;
    }
}


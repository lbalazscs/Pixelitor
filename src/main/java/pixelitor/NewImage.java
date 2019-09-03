/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.OpenComps;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.TextFieldValidator;
import pixelitor.gui.utils.ValidatedPanel;
import pixelitor.gui.utils.ValidationResult;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

import static java.lang.Integer.parseInt;
import static javax.swing.BorderFactory.createEmptyBorder;
import static pixelitor.colors.FillType.TRANSPARENT;

/**
 * Static methods for creating new images
 */
public final class NewImage {
    private static int untitledCount = 1;

    private static final Dimension lastSize = AppPreferences.getNewImageSize();

    private NewImage() {
    }

    public static Composition addNewImage(FillType bg, int width, int height, String title) {
        Composition comp = createNewComposition(bg, width, height, title);
        OpenComps.addAsNewImage(comp);
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
        NewImagePanel panel = new NewImagePanel();
        new DialogBuilder()
                .title("New Image")
                .validatedContent(panel)
                .okAction(panel::okPressedInDialog)
                .show();
    }

    public static Action getAction() {
        return new AbstractAction("New Image...") {
            @Override
            public void actionPerformed(ActionEvent e) {
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

        private NewImagePanel() {
            setLayout(new GridBagLayout());
            GridBagHelper gbh = new GridBagHelper(this);

            //noinspection SuspiciousNameCombination
            setBorder(createEmptyBorder(BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH));

            widthTF = addTextField("widthTF", "Width:",
                    lastSize.width, gbh);
            heightTF = addTextField("heightTF", "Height:",
                    lastSize.height, gbh);

            backgroundSelector = new JComboBox(FillType.values());
            gbh.addLabelWithLastControl("Fill:", backgroundSelector);
        }

        private static JTextField addTextField(String name, String labelText,
                                               int value, GridBagHelper gbh) {
            JTextField tf = new JTextField(String.valueOf(value));
            tf.setName(name);
            gbh.addLabelWithTwoControls(labelText,
                TextFieldValidator.createPositiveIntLayerFor(tf, false),
                    new JLabel("pixels"));
            return tf;
        }

        @Override
        public ValidationResult checkValidity() {
            ValidationResult retVal = ValidationResult.ok();
            try {
                int width = getSelectedWidth();
                retVal = retVal.addErrorIfZero(width, "Width");
                retVal = retVal.addErrorIfNegative(width, "Width");
            } catch (NumberFormatException e) {
                retVal = retVal.addError("The width must be an integer.");
            }
            try {
                int height = getSelectedHeight();
                retVal = retVal.addErrorIfZero(height, "Height");
                retVal = retVal.addErrorIfNegative(height, "Height");
            } catch (NumberFormatException e) {
                retVal = retVal.addError("The height must be an integer.");
            }
            return retVal;
        }

        private void okPressedInDialog() {
            int selectedWidth = getSelectedWidth();
            int selectedHeight = getSelectedHeight();
            FillType bg = getSelectedBackground();

            String title = "Untitled" + untitledCount;
            addNewImage(bg, selectedWidth, selectedHeight, title);
            untitledCount++;

            lastSize.width = selectedWidth;
            lastSize.height = selectedHeight;
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
}


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

package pixelitor.compactions;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.filters.gui.DialogMenuBar;
import pixelitor.filters.gui.DialogMenuOwner;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.utils.*;
import pixelitor.utils.ResizeUnit;
import pixelitor.utils.Utils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

import static java.awt.FlowLayout.LEFT;
import static java.lang.Integer.parseInt;
import static pixelitor.gui.utils.TFValidationLayerUI.wrapWithValidation;

/**
 * The GUI for the resize settings.
 */
@SuppressWarnings("SuspiciousNameCombination")
public class ResizePanel extends ValidatedPanel implements KeyListener, ItemListener, DialogMenuOwner {
    private static final NumberFormat PERCENT_FORMAT = new DecimalFormat("#0.00");
    private static final int INPUT_FIELD_COLUMNS = 5;

    // shared model for the two comboboxes
    private final DefaultComboBoxModel<ResizeUnit> unitSelectorModel;

    private final JCheckBox keepProportionsCB;
    private final JTextField heightTF;
    private final JTextField widthTF;

    private final double origAspectRatio;
    private final int origWidth;
    private final int origHeight;

    private int targetWidth;
    private int targetHeight;
    private double targetWidthPercent;
    private double targetHeightPercent;

    private final Validator widthdValidator = new Validator("Width");
    private final Validator heightValidator = new Validator("Height");
    private final TitledBorder border;

    private ResizePanel(Canvas canvas) {
        origWidth = canvas.getWidth();
        origHeight = canvas.getHeight();
        origAspectRatio = ((double) origWidth) / origHeight;

        targetWidth = origWidth;
        targetHeight = origHeight;
        targetWidthPercent = 100.0;
        targetHeightPercent = 100.0;

        unitSelectorModel = new DefaultComboBoxModel<>(ResizeUnit.values());
        var inputPanel = new JPanel(new GridBagLayout());
        var gbh = new GridBagHelper(inputPanel);

        widthTF = new JTextField(INPUT_FIELD_COLUMNS);
        widthTF.setName("widthTF");
        widthTF.addKeyListener(this);
        updateWidthText(ResizeUnit.PIXELS);
        var unitChooser1 = new JComboBox<>(unitSelectorModel);
        var widthLayer = wrapWithValidation(widthTF, widthdValidator);
        gbh.addLabelAndTwoControls("Width:", widthLayer, unitChooser1);

        heightTF = new JTextField(INPUT_FIELD_COLUMNS);
        heightTF.setName("heightTF");
        updateHeightText(ResizeUnit.PIXELS);
        heightTF.addKeyListener(this);
        var unitChooser2 = new JComboBox<>(unitSelectorModel);
        var heightLayer = wrapWithValidation(heightTF, heightValidator);
        gbh.addLabelAndTwoControls("Height:", heightLayer, unitChooser2);

        unitChooser1.addItemListener(this);
        unitChooser2.addItemListener(this);

        border = BorderFactory.createTitledBorder("");
        updateBorderText();
        inputPanel.setBorder(border);
        Box verticalBox = Box.createVerticalBox();
        verticalBox.add(inputPanel);

        JPanel optionsPanel = new JPanel(new FlowLayout(LEFT));
        keepProportionsCB = new JCheckBox("Keep Proportions");
        keepProportionsCB.setSelected(true);
        keepProportionsCB.addItemListener(this);
        optionsPanel.add(keepProportionsCB);
        verticalBox.add(optionsPanel);

        add(verticalBox);
    }

    private ResizeUnit getUnit() {
        return (ResizeUnit) unitSelectorModel.getSelectedItem();
    }

    private boolean usePixelUnit() {
        return unitSelectorModel.getSelectedItem() == ResizeUnit.PIXELS;
    }

    private boolean keepProportions() {
        return keepProportionsCB.isSelected();
    }

    private static double parseDouble(String s) throws ParseException {
        if (s.isEmpty()) {
            return 0;
        }
        return Utils.parseLocalizedDouble(s);
    }

    // a combo box or a checkbox was used
    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == keepProportionsCB) {
            if (keepProportions()) {
                keepProportionsByAdjustingHeight();
            }
        } else { // one of the combo boxes was selected
            unitChanged();
        }
    }

    private void keepProportionsByAdjustingHeight() {
        targetHeight = (int) (targetWidth / origAspectRatio);
        targetHeightPercent = targetWidthPercent;
        updateHeightText(getUnit());
    }

    private void unitChanged() {
        ResizeUnit newUnit = getUnit();
        widthdValidator.setUnit(newUnit);
        heightValidator.setUnit(newUnit);
        updateWidthText(newUnit);
        updateHeightText(newUnit);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getSource() == widthTF) {
            keyReleasedInWidthTF();
            updateBorderText();
        } else if (e.getSource() == heightTF) {
            keyReleasedInHeightTF();
            updateBorderText();
        }
    }

    private void keyReleasedInWidthTF() {
        if (usePixelUnit()) {
            try {
                targetWidth = parseInt(getWidthText());
                targetWidthPercent = ((double) targetWidth) * 100 / origWidth;
                if (keepProportions()) {
                    targetHeight = (int) (targetWidth / origAspectRatio);
                    if (targetHeight == 0) {
                        targetHeight = 1;
                    }
                    updateHeightText(ResizeUnit.PIXELS);
                    targetHeightPercent = targetWidthPercent;
                }
            } catch (NumberFormatException ex) {
                resetWidth(ResizeUnit.PIXELS);
            }
        } else { // unit is percentage
            try {
                targetWidthPercent = parseDouble(getWidthText());
                targetWidth = (int) (origWidth * targetWidthPercent / 100);
                if (keepProportions()) {
                    targetHeight = (int) (targetWidth / origAspectRatio);
                    targetHeightPercent = targetWidthPercent;
                    updateHeightText(ResizeUnit.PERCENTAGE);
                }
            } catch (ParseException e) {
                resetWidth(ResizeUnit.PERCENTAGE);
            }
        }
    }

    private void resetWidth(ResizeUnit unit) {
        if (isWidthFieldEmpty()) {
            targetWidth = -1;
        } else if (targetWidth > 0) {
            updateWidthText(unit);
        }
    }

    private void keyReleasedInHeightTF() {
        if (usePixelUnit()) {
            try {
                targetHeight = parseInt(getHeightText());
                targetHeightPercent = ((double) targetHeight) * 100 / origHeight;
                if (keepProportions()) {
                    targetWidth = (int) (targetHeight * origAspectRatio);
                    if (targetWidth == 0) {
                        targetWidth = 1;
                    }
                    updateWidthText(ResizeUnit.PIXELS);
                    targetWidthPercent = targetHeightPercent;
                }
            } catch (NumberFormatException ex) {
                resetHeight(ResizeUnit.PIXELS);
            }
        } else {  // unit is percent
            try {
                targetHeightPercent = parseDouble(getHeightText());
                targetHeight = (int) (origHeight * targetHeightPercent / 100);
                if (keepProportions()) {
                    targetWidth = (int) (targetHeight * origAspectRatio);
                    targetWidthPercent = targetHeightPercent;
                    updateWidthText(ResizeUnit.PERCENTAGE);
                }
            } catch (ParseException e) {
                resetHeight(ResizeUnit.PERCENTAGE);
            }
        }
    }

    private void resetHeight(ResizeUnit unit) {
        if (isHeightFieldEmpty()) {
            targetHeight = -1;
        } else if (targetHeight > 0) {
            updateHeightText(unit);
        }
    }

    private void updateWidthText(ResizeUnit unit) {
        widthTF.setText(switch (unit) {
            case PIXELS -> String.valueOf(targetWidth);
            case PERCENTAGE -> PERCENT_FORMAT.format(targetWidthPercent);
        });
    }

    private void updateHeightText(ResizeUnit unit) {
        heightTF.setText(switch (unit) {
            case PIXELS -> String.valueOf(targetHeight);
            case PERCENTAGE -> PERCENT_FORMAT.format(targetHeightPercent);
        });
    }

    private void updateBorderText() {
        String origSize = origWidth + "x" + origHeight + " to ";
        if (targetWidth > 0 && targetHeight > 0) {
            border.setTitle(origSize + targetWidth + "x" + targetHeight);
        } else {
            border.setTitle(origSize + "??");
        }

        repaint();
    }

    @Override
    public ValidationResult validateSettings() {
        return widthdValidator.check(widthTF)
            .and(heightValidator.check(heightTF));
    }

    private String getWidthText() {
        return widthTF.getText().trim();
    }

    private String getHeightText() {
        return heightTF.getText().trim();
    }

    private boolean isWidthFieldEmpty() {
        return getWidthText().isEmpty();
    }

    private boolean isHeightFieldEmpty() {
        return getHeightText().isEmpty();
    }

    private int getTargetWidth() {
        return targetWidth;
    }

    private int getTargetHeight() {
        return targetHeight;
    }

    public static void showInDialog(Composition comp, String dialogTitle) {
        ResizePanel p = new ResizePanel(comp.getCanvas());
        new DialogBuilder()
            .validatedContent(p)
            .title(dialogTitle)
            .menuBar(new DialogMenuBar(p))
            .okAction(() -> new Resize(p.getTargetWidth(), p.getTargetHeight()).process(comp))
            .show();
    }

    @Override
    public boolean supportsUserPresets() {
        return true;
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        preset.putBoolean("Pixels", usePixelUnit());
        preset.putBoolean("Constrain", keepProportions());
        preset.put("Width", getWidthText());
        preset.put("Height", getHeightText());
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        boolean pixels = preset.getBoolean("Pixels");
        unitSelectorModel.setSelectedItem(pixels
            ? ResizeUnit.PIXELS
            : ResizeUnit.PERCENTAGE);

        boolean keep = preset.getBoolean("Constrain");
        keepProportionsCB.setSelected(keep);

        heightTF.setText(preset.get("Height"));
        widthTF.setText(preset.get("Width"));

        if (keep) {
            // Ensure that the values are consistent with the current image size.
            // The height will be adjusted to match the width.
            keyReleasedInWidthTF();
        }
        updateBorderText();
    }

    @Override
    public String getPresetDirName() {
        return "Resize";
    }

    /**
     * A textfield validator that can validate
     * either int or double textfields.
     */
    static class Validator implements TextFieldValidator {
        private boolean pixelMode = true;
        private final String label;

        public Validator(String label) {
            this.label = label;
        }

        public void setUnit(ResizeUnit unit) {
            this.pixelMode = (unit == ResizeUnit.PIXELS);
        }

        @Override
        public ValidationResult check(JTextField textField) {
            if (pixelMode) {
                return TextFieldValidator.hasPositiveInt(textField, label, false);
            } else {
                return TextFieldValidator.hasPositiveDouble(textField, label);
            }
        }
    }
}
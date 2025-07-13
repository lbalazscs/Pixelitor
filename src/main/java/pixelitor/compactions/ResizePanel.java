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

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.ParseException;

import static java.awt.FlowLayout.LEFT;
import static pixelitor.gui.utils.TFValidationLayerUI.wrapWithValidation;

/**
 * The GUI for the resize settings.
 */
@SuppressWarnings("SuspiciousNameCombination")
public class ResizePanel extends ValidatedPanel implements KeyListener, ItemListener, DialogMenuOwner {
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

    private final Validator widthValidator = new Validator("Width");
    private final Validator heightValidator = new Validator("Height");
    private final TitledBorder border;

    private ResizePanel(Canvas canvas) {
        origWidth = canvas.getWidth();
        origHeight = canvas.getHeight();
        origAspectRatio = ((double) origWidth) / origHeight;

        targetWidth = origWidth;
        targetHeight = origHeight;

        unitSelectorModel = new DefaultComboBoxModel<>(ResizeUnit.values());
        var inputPanel = new JPanel(new GridBagLayout());
        var gbh = new GridBagHelper(inputPanel);

        widthTF = new JTextField(INPUT_FIELD_COLUMNS);
        widthTF.setName("widthTF");
        widthTF.addKeyListener(this);
        updateWidthText(ResizeUnit.PIXELS);
        var unitChooser1 = new JComboBox<>(unitSelectorModel);
        var widthLayer = wrapWithValidation(widthTF, widthValidator);
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

    private boolean keepProportions() {
        return keepProportionsCB.isSelected();
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
        targetHeight = (int) Math.round(targetWidth / origAspectRatio);
        if (targetHeight == 0) {
            targetHeight = 1;
        }
        updateHeightText(getUnit());
    }

    private void unitChanged() {
        ResizeUnit newUnit = getUnit();
        widthValidator.setUnit(newUnit);
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
        Object source = e.getSource();
        if (source == widthTF || source == heightTF) {
            textFieldChanged((JTextField) source);
            updateBorderText();
        }
    }

    /**
     * Handles a change in one of the text fields.
     */
    private void textFieldChanged(JTextField source) {
        boolean isWidth = source == widthTF;
        try {
            updateModelFromText(source.getText().trim(), isWidth);
            if (keepProportions()) {
                recalculateOtherDimension(isWidth);
            }
        } catch (NumberFormatException | ParseException e) {
            // mark the value as invalid, which causes the border text to show "??"
            if (isWidth) {
                targetWidth = -1;
            } else {
                targetHeight = -1;
            }
        }
    }

    /**
     * Updates the model from the text in the source text field.
     */
    private void updateModelFromText(String text, boolean isWidthField) throws ParseException, NumberFormatException {
        ResizeUnit unit = getUnit();
        double value = unit.parse(text);

        if (isWidthField) {
            targetWidth = unit.toPixels(value, origWidth);
        } else { // height field
            targetHeight = unit.toPixels(value, origHeight);
        }
    }

    /**
     * Recalculates the other dimension to keep proportions.
     */
    private void recalculateOtherDimension(boolean changedWidth) {
        if (changedWidth) {
            targetHeight = (int) Math.round(targetWidth / origAspectRatio);
            if (targetHeight == 0) {
                targetHeight = 1;
            }
            updateHeightText(getUnit());
        } else { // changed height
            targetWidth = (int) Math.round(targetHeight * origAspectRatio);
            if (targetWidth == 0) {
                targetWidth = 1;
            }
            updateWidthText(getUnit());
        }
    }

    private void updateWidthText(ResizeUnit unit) {
        double valueInUnit = unit.fromPixels(targetWidth, origWidth);
        widthTF.setText(unit.format(valueInUnit));
    }

    private void updateHeightText(ResizeUnit unit) {
        double valueInUnit = unit.fromPixels(targetHeight, origHeight);
        heightTF.setText(unit.format(valueInUnit));
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
        return widthValidator.check(widthTF)
            .and(heightValidator.check(heightTF));
    }

    private String getWidthText() {
        return widthTF.getText().trim();
    }


    private String getHeightText() {
        return heightTF.getText().trim();
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
        preset.putBoolean("Pixels", getUnit() == ResizeUnit.PIXELS);
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

        // update model from text fields
        textFieldChanged(widthTF);
        if (!keep) {
            textFieldChanged(heightTF);
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
                return TextFieldValidator.hasPositiveInt(textField, label);
            } else {
                return TextFieldValidator.hasPositiveDouble(textField, label);
            }
        }
    }
}
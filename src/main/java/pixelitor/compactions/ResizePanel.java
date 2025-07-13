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
public class ResizePanel extends ValidatedPanel implements KeyListener, ItemListener, DialogMenuOwner {
    private static final int INPUT_FIELD_COLUMNS = 5;

    private static final Integer DEFAULT_DPI = 300;
    private static final Integer[] DPI_VALUES = {150, DEFAULT_DPI, 600};
    private int currentDpi = DEFAULT_DPI;

    // shared model for the two comboboxes
    private final DefaultComboBoxModel<ResizeUnit> unitSelectorModel;

    private final JCheckBox keepProportionsCB;
    private final JTextField heightTF;
    private final JTextField widthTF;
    private final JComboBox<Integer> dpiChooser;

    private final double origAspectRatio;
    private final int origWidth;
    private final int origHeight;

    private int targetWidth;
    private int targetHeight;

    private final DimensionValidator widthValidator = new DimensionValidator("Width");
    private final DimensionValidator heightValidator = new DimensionValidator("Height");
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

        widthTF = createTextField("Width:", "widthTF", widthValidator, gbh);
        heightTF = createTextField("Height:", "heightTF", heightValidator, gbh);

        border = BorderFactory.createTitledBorder("");
        updateBorderText();
        inputPanel.setBorder(border);

        JPanel optionsPanel = new JPanel(new FlowLayout(LEFT));
        keepProportionsCB = new JCheckBox("Keep Proportions");
        keepProportionsCB.setSelected(true);
        keepProportionsCB.addItemListener(this);
        optionsPanel.add(keepProportionsCB);

        JPanel dpiPanel = new JPanel(new FlowLayout(LEFT));
        dpiPanel.add(new JLabel("DPI:"));
        dpiChooser = new JComboBox<>(DPI_VALUES);
        dpiChooser.setSelectedItem(DEFAULT_DPI);
        dpiChooser.addItemListener(this);
        dpiChooser.setEnabled(false);
        dpiPanel.add(dpiChooser);

        Box verticalBox = Box.createVerticalBox();
        verticalBox.add(inputPanel);
        verticalBox.add(optionsPanel);
        verticalBox.add(dpiPanel);
        add(verticalBox);

        updateWidthText(ResizeUnit.PIXELS);
        updateHeightText(ResizeUnit.PIXELS);
    }

    private JTextField createTextField(String labelText, String name,
                                       DimensionValidator validator,
                                       GridBagHelper gbh) {
        JTextField textField = new JTextField(INPUT_FIELD_COLUMNS);
        textField.setName(name);
        textField.addKeyListener(this);
        var unitChooser = new JComboBox<>(unitSelectorModel);
        var jLayer = wrapWithValidation(textField, validator);
        gbh.addLabelAndTwoControls(labelText, jLayer, unitChooser);
        unitChooser.addItemListener(this);
        return textField;
    }

    private ResizeUnit getUnit() {
        return (ResizeUnit) unitSelectorModel.getSelectedItem();
    }

    private int getDpi() {
        return (int) dpiChooser.getSelectedItem();
    }

    private boolean keepProportions() {
        return keepProportionsCB.isSelected();
    }

    // a combo box or a checkbox was used
    @Override
    public void itemStateChanged(ItemEvent e) {
        Object source = e.getSource();
        if (e.getStateChange() != ItemEvent.SELECTED) {
            return;
        }

        if (source == keepProportionsCB) {
            if (keepProportions()) {
                adjustHeightToKeepProportions();
            }
        } else if (source == dpiChooser) {
            dpiChanged();
        } else { // one of the unit combo boxes was selected
            unitChanged();
        }
    }

    private void dpiChanged() {
        int oldDpi = currentDpi;
        int newDpi = getDpi();
        currentDpi = newDpi;

        // When DPI changes, the physical size should be preserved.
        // To avoid precision loss from reading the rounded values in the text fields,
        // we recalculate the target pixel dimensions directly.
        targetWidth = (int) Math.round(((double) targetWidth / oldDpi) * newDpi);
        if (targetWidth == 0) {
            targetWidth = 1;
        }

        if (keepProportions()) {
            adjustHeightToKeepProportions();
        } else {
            // if not keeping proportions, update height independently
            targetHeight = (int) Math.round(((double) targetHeight / oldDpi) * newDpi);
            if (targetHeight == 0) {
                targetHeight = 1;
            }
        }

        updateBorderText();
    }

    private void unitChanged() {
        ResizeUnit newUnit = getUnit();
        widthValidator.setUnit(newUnit);
        heightValidator.setUnit(newUnit);

        dpiChooser.setEnabled(newUnit.isPhysical());

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
        int dpi = getDpi();

        if (isWidthField) {
            targetWidth = unit.toPixels(value, origWidth, dpi);
        } else { // height field
            targetHeight = unit.toPixels(value, origHeight, dpi);
        }
    }

    /**
     * Recalculates the other dimension to keep proportions.
     */
    private void recalculateOtherDimension(boolean changedWidth) {
        if (changedWidth) {
            adjustHeightToKeepProportions();
        } else { // changed height
            adjustWidthToKeepProportions();
        }
    }

    private void adjustHeightToKeepProportions() {
        targetHeight = (int) Math.round(targetWidth / origAspectRatio);
        if (targetHeight == 0) {
            targetHeight = 1;
        }
        updateHeightText(getUnit());
    }

    private void adjustWidthToKeepProportions() {
        targetWidth = (int) Math.round(targetHeight * origAspectRatio);
        if (targetWidth == 0) {
            targetWidth = 1;
        }
        updateWidthText(getUnit());
    }

    private void updateWidthText(ResizeUnit unit) {
        double valueInUnit = unit.fromPixels(targetWidth, origWidth, getDpi());
        widthTF.setText(unit.format(valueInUnit));
    }

    private void updateHeightText(ResizeUnit unit) {
        double valueInUnit = unit.fromPixels(targetHeight, origHeight, getDpi());
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
        preset.put("Unit", getUnit().toString());
        preset.putInt("DPI", getDpi());
        preset.putBoolean("Constrain", keepProportions());
        preset.put("Width", getWidthText());
        preset.put("Height", getHeightText());
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        unitSelectorModel.setSelectedItem(preset.getEnum("Unit", ResizeUnit.class));
        dpiChooser.setSelectedItem(preset.getInt("DPI"));

        boolean keep = preset.getBoolean("Constrain");
        keepProportionsCB.setSelected(keep);

        // set text fields first, then update the model from them
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
     * Validates text fields for positive numeric values based on the current unit.
     */
    static class DimensionValidator implements TextFieldValidator {
        private boolean pixelMode = true;
        private final String label;

        public DimensionValidator(String label) {
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
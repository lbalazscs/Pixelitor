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

package pixelitor.gui.utils;

import pixelitor.Canvas;
import pixelitor.filters.gui.UserPreset;
import pixelitor.utils.DimensionValidator;
import pixelitor.utils.ResizeUnit;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.ParseException;

import static pixelitor.Composition.DEFAULT_DPI;
import static pixelitor.gui.utils.TFValidationLayerUI.wrapWithValidation;

/**
 * A delegate that handles common dimension input functionality for
 * the "new image" and "resize" panels.
 */
public class DimensionHelper {
    private static final int INPUT_FIELD_COLUMNS = 5;
    public static final Integer[] DPI_VALUES = {150, DEFAULT_DPI, 600};
    private static final String DPI_PRESET_KEY = "DPI";

    private final JTextField widthTF;
    private final JTextField heightTF;
    private final JComboBox<Integer> dpiChooser;
    private final DefaultComboBoxModel<ResizeUnit> unitSelectorModel;

    private final DimensionValidator widthValidator;
    private final DimensionValidator heightValidator;

    private int targetWidth;
    private int targetHeight;
    private int currentDpi;

    private final int originalWidth;
    private final int originalHeight;

    private final DimensionChangeCallback callback;

    /**
     * A callback interface for the client panel to implement.
     */
    public interface DimensionChangeCallback {
        /**
         * Called when a dimension text field (width or height) is modified by the user.
         */
        void dimensionChanged(boolean isWidthChanged);

        /**
         * Called when the DPI value is changed.
         */
        void dpiChanged();
    }

    public DimensionHelper(DimensionChangeCallback callback, ResizeUnit[] units, int initialWidth, int initialHeight, int initialDpi, int originalWidth, int originalHeight) {
        this.callback = callback;
        this.targetWidth = initialWidth;
        this.targetHeight = initialHeight;
        this.currentDpi = initialDpi;
        this.originalWidth = originalWidth;
        this.originalHeight = originalHeight;

        widthValidator = new DimensionValidator("Width", Canvas.MAX_WIDTH, originalWidth);
        heightValidator = new DimensionValidator("Height", Canvas.MAX_HEIGHT, originalHeight);

        unitSelectorModel = new DefaultComboBoxModel<>(units);
        widthTF = new JTextField(INPUT_FIELD_COLUMNS);
        widthTF.setName("widthTF");

        heightTF = new JTextField(INPUT_FIELD_COLUMNS);
        heightTF.setName("heightTF");

        dpiChooser = new JComboBox<>(DPI_VALUES);
        dpiChooser.setSelectedItem(initialDpi);
    }

    public JLayer<JTextField> createWidthTextField(KeyListener keyListener) {
        widthTF.addKeyListener(keyListener);
        return wrapWithValidation(widthTF, widthValidator);
    }

    public JLayer<JTextField> createHeightTextField(KeyListener keyListener) {
        heightTF.addKeyListener(keyListener);
        return wrapWithValidation(heightTF, heightValidator);
    }

    public JComboBox<ResizeUnit> createUnitChooser(ItemListener itemListener) {
        var unitChooser = new JComboBox<>(unitSelectorModel);
        unitChooser.addItemListener(itemListener);
        return unitChooser;
    }

    public JComboBox<Integer> getDpiChooser() {
        return dpiChooser;
    }

    public ValidationResult checkWidthAndHeight() {
        return widthValidator.check(widthTF).and(heightValidator.check(heightTF));
    }

    public DefaultComboBoxModel<ResizeUnit> getUnitSelectorModel() {
        return unitSelectorModel;
    }

    public int getTargetWidth() {
        return targetWidth;
    }

    public int getTargetHeight() {
        return targetHeight;
    }

    public ResizeUnit getUnit() {
        return (ResizeUnit) unitSelectorModel.getSelectedItem();
    }

    public int getDpi() {
        return (int) dpiChooser.getSelectedItem();
    }

    public void setTargetWidth(int width) {
        this.targetWidth = width;
    }

    public void setTargetHeight(int height) {
        this.targetHeight = height;
    }

    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == dpiChooser) {
            dpiChanged();
        } else { // one of the unit combo boxes was selected
            unitChanged();
        }
    }

    private void dpiChanged() {
        int prevDpi = currentDpi;
        int newDpi = getDpi();
        currentDpi = newDpi;

        // preserve the physical size by recalculating pixel dimensions
        // directly to avoid precision loss from parsing rounded text field values
        targetWidth = scaleForDpiChange(targetWidth, prevDpi, newDpi);
        targetHeight = scaleForDpiChange(targetHeight, prevDpi, newDpi);

        widthValidator.updateContext(getUnit(), newDpi);
        heightValidator.updateContext(getUnit(), newDpi);

        callback.dpiChanged();
    }

    /**
     * Scales a dimension value for DPI change.
     */
    private static int scaleForDpiChange(int value, int prevDpi, int newDpi) {
        int scaledValue = (int) Math.round(((double) value / prevDpi) * newDpi);
        return Math.max(scaledValue, 1);
    }

    private void unitChanged() {
        ResizeUnit newUnit = getUnit();
        int dpi = getDpi();

        widthValidator.updateContext(newUnit, dpi);
        heightValidator.updateContext(newUnit, dpi);

        dpiChooser.setEnabled(newUnit.isPhysical());

        updateWidthText();
        updateHeightText();
    }

    public void keyReleased(KeyEvent e) {
        Object source = e.getSource();
        if (source == widthTF || source == heightTF) {
            textFieldChanged((JTextField) source);
        }
    }

    private void textFieldChanged(JTextField source) {
        boolean isWidth = source == widthTF;
        try {
            updateModelFromText(source.getText().trim(), isWidth);
        } catch (NumberFormatException | ParseException e) {
            // mark the value as invalid
            if (isWidth) {
                targetWidth = -1;
            } else {
                targetHeight = -1;
            }
        }
        // notify client to update UI (e.g., border text) and handle proportions
        callback.dimensionChanged(isWidth);
    }

    private void updateModelFromText(String text, boolean isWidthField) throws ParseException, NumberFormatException {
        ResizeUnit unit = getUnit();
        double value = unit.parse(text);
        int dpi = getDpi();

        if (isWidthField) {
            targetWidth = unit.toPixels(value, originalWidth, dpi);
        } else { // height field
            targetHeight = unit.toPixels(value, originalHeight, dpi);
        }
    }

    public void updateWidthText() {
        ResizeUnit unit = getUnit();
        double valueInUnit = unit.fromPixels(targetWidth, originalWidth, getDpi());
        widthTF.setText(unit.format(valueInUnit));
    }

    public void updateHeightText() {
        ResizeUnit unit = getUnit();
        double valueInUnit = unit.fromPixels(targetHeight, originalHeight, getDpi());
        heightTF.setText(unit.format(valueInUnit));
    }

    public void setInitialValues() {
        ResizeUnit unit = getUnit();
        widthValidator.updateContext(unit, getDpi());
        heightValidator.updateContext(unit, getDpi());
        dpiChooser.setEnabled(unit.isPhysical());
        updateWidthText();
        updateHeightText();
    }

    public void saveStateTo(UserPreset preset) {
        preset.put("Width", widthTF.getText().trim());
        preset.put("Height", heightTF.getText().trim());

        preset.put(ResizeUnit.PRESET_KEY, getUnit().name());
        preset.putInt(DPI_PRESET_KEY, getDpi());
    }

    public void loadUserPreset(UserPreset preset, boolean keepProportions) {
        unitSelectorModel.setSelectedItem(preset.getEnum(ResizeUnit.PRESET_KEY, ResizeUnit.class));
        dpiChooser.setSelectedItem(preset.getInt(DPI_PRESET_KEY, DEFAULT_DPI));

        // set text fields first, then update the model from them
        widthTF.setText(preset.get("Width"));
        heightTF.setText(preset.get("Height"));

        // update model from text fields
        textFieldChanged(widthTF);
        if (!keepProportions) {
            textFieldChanged(heightTF);
        }
    }
}

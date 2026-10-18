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

package pixelitor.autopaint;

import pixelitor.colors.FgBgColors;
import pixelitor.filters.gui.*;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.SliderSpinner.LabelPosition;
import pixelitor.gui.utils.Validated;
import pixelitor.gui.utils.ValidationResult;
import pixelitor.tools.AbstractBrushTool;

import javax.swing.*;
import java.awt.GridBagLayout;

import static java.lang.Integer.parseInt;
import static pixelitor.gui.utils.TextFieldValidator.createIntRangeLayer;

/**
 * Configuration panel for the "Auto Paint" dialog.
 */
public class AutoPaintPanel extends JPanel implements Validated, DialogMenuOwner {
    public static final int MIN_STROKE_COUNT = 1;
    public static final int MAX_STROKE_COUNT = 10_000;
    public static final int MIN_STROKE_LENGTH = 1;
    public static final int MAX_STROKE_LENGTH = 1_000;

    private static final String STROKE_LENGTH_TEXT = "Average Stroke Length";
    private static final String STROKE_COUNT_TEXT = "Number of Strokes";

    private final ChoiceParam<AbstractBrushTool> toolsParam = new ChoiceParam<>(
        "Tool", AutoPaint.SUPPORTED_TOOLS);
    private final JTextField strokeCountTF;
    private final JTextField strokeLengthTF;

    private final EnumParam<ColorMode> colorMode = new EnumParam<>(
        "Random Colors", ColorMode.class);

    private final RangeParam lengthVariation = new RangeParam(
        "Stroke Length Variation (%)", 0, 50, 100, true, LabelPosition.NONE);
    private final RangeParam curvature = new RangeParam(
        "Stroke Curvature (%)", 0, 100, 300, true, LabelPosition.NONE);

    private final EnumParam<StrokeDirection> strokeDirection = new EnumParam<>(
        "Direction", StrokeDirection.class);

    AutoPaintPanel() {
        super(new GridBagLayout());
        var gbh = new GridBagHelper(this);

        gbh.addParam(toolsParam, "toolSelector");
        gbh.addParam(strokeDirection);

        strokeCountTF = new JTextField("100");
        strokeCountTF.setName("strokeCountTF");
        gbh.addLabelAndControl(STROKE_COUNT_TEXT + ":",
            createIntRangeLayer(strokeCountTF, STROKE_COUNT_TEXT,
                MIN_STROKE_COUNT, MAX_STROKE_COUNT));

        strokeLengthTF = new JTextField("100");
        gbh.addLabelAndControl(STROKE_LENGTH_TEXT + ":",
            createIntRangeLayer(strokeLengthTF, STROKE_LENGTH_TEXT,
                MIN_STROKE_LENGTH, MAX_STROKE_LENGTH));

        gbh.addParam(lengthVariation);
        gbh.addParam(curvature);
        gbh.addParam(colorMode, "colorsCB");

        // enable the color selector UI only if the tool supports it
        toolsParam.enableOtherWhen(colorMode, AutoPaint::toolUsesColors);
    }

    /**
     * Creates an {@link AutoPaintSettings} object from the current UI state.
     */
    public AutoPaintSettings getSettings() {
        return AutoPaintSettings.of(getSelectedTool(),
            getStrokeCount(), getStrokeLength(), strokeDirection.getSelected(),
            colorMode.getSelected(),
            lengthVariation.getPercentage(), curvature.getPercentage());
    }

    private AbstractBrushTool getSelectedTool() {
        return toolsParam.getSelected();
    }

    private int getStrokeCount() {
        return parseInt(strokeCountTF.getText().trim());
    }

    private int getStrokeLength() {
        return parseInt(strokeLengthTF.getText().trim());
    }

    @Override
    public ValidationResult validateSettings() {
        return ValidationResult.valid()
            .requireIntRange(strokeCountTF.getText(), STROKE_COUNT_TEXT,
                MIN_STROKE_COUNT, MAX_STROKE_COUNT)
            .requireIntRange(strokeLengthTF.getText(), STROKE_LENGTH_TEXT,
                MIN_STROKE_LENGTH, MAX_STROKE_LENGTH);
    }

    @Override
    public boolean supportsUserPresets() {
        return true;
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        toolsParam.saveStateTo(preset);
        strokeDirection.saveStateTo(preset);

        preset.putInt(STROKE_COUNT_TEXT, getStrokeCount());
        preset.putInt(STROKE_LENGTH_TEXT, getStrokeLength());

        lengthVariation.saveStateTo(preset);
        curvature.saveStateTo(preset);
        colorMode.saveStateTo(preset);

        if (AutoPaint.toolUsesColors(getSelectedTool())) {
            FgBgColors.saveStateTo(preset);
        }
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        toolsParam.loadStateFrom(preset);
        strokeDirection.loadStateFrom(preset);

        strokeCountTF.setText(preset.get(STROKE_COUNT_TEXT));
        strokeLengthTF.setText(preset.get(STROKE_LENGTH_TEXT));

        lengthVariation.loadStateFrom(preset);
        curvature.loadStateFrom(preset);
        colorMode.loadStateFrom(preset);

        if (AutoPaint.toolUsesColors(getSelectedTool())) {
            FgBgColors.loadStateFrom(preset, true);
        }
    }

    @Override
    public UserPreset createUserPreset(String presetName) {
        ValidationResult validation = validateSettings();
        if (!validation.isValid()) {
            validation.showErrorDialog(this);

            // abort the save process
            return null;
        }

        // settings are valid, proceed with default preset creation
        return DialogMenuOwner.super.createUserPreset(presetName);
    }

    @Override
    public String getPresetDirName() {
        return "Auto Paint";
    }
}

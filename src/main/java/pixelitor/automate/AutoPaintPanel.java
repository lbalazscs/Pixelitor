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

package pixelitor.automate;

import pixelitor.colors.FgBgColors;
import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.SliderSpinner.LabelPosition;
import pixelitor.gui.utils.ValidatedPanel;
import pixelitor.gui.utils.ValidationResult;
import pixelitor.tools.AbstractBrushTool;

import javax.swing.*;
import java.awt.GridBagLayout;

import static java.lang.Integer.parseInt;
import static pixelitor.gui.utils.TextFieldValidator.createPositiveIntLayer;

/**
 * Configuration panel for the "Auto Paint".
 */
public class AutoPaintPanel extends ValidatedPanel implements DialogMenuOwner {
    private static final String COLOR_MODE_FOREGROUND = "Foreground";
    private static final String COLOR_MODE_INTERPOLATED = "Foreground-Background Mix";
    private static final String COLOR_MODE_RANDOM = "Random";
    public static final String[] COLOR_MODES =
        {COLOR_MODE_INTERPOLATED, COLOR_MODE_FOREGROUND, COLOR_MODE_RANDOM};

    private static final String STROKE_LENGTH_TEXT = "Average Stroke Length";
    private static final String STROKE_COUNT_TEXT = "Number of Strokes";

    private final ChoiceParam<AbstractBrushTool> toolsParam = new ChoiceParam<>(
        "Tool", AutoPaint.SUPPORTED_TOOLS);
    private final JTextField strokeCountTF;
    private final JTextField strokeLengthTF;
    private final ChoiceParam<String> colorMode = new ChoiceParam<>(
        "Random Colors", COLOR_MODES);

    private final RangeParam lengthVariation = new RangeParam(
        "Stroke Length Variation (%)", 0, 50, 100, true, LabelPosition.NONE);
    private final RangeParam curvature = new RangeParam(
        "Stroke Curvature (%)", 0, 100, 300, true, LabelPosition.NONE);

    private final IntChoiceParam strokeDirection = new IntChoiceParam("Direction", new Item[]{
        new Item("Random", AutoPaintSettings.DIRECTION_RANDOM),
        new Item("Radial", AutoPaintSettings.DIRECTION_RADIAL),
        new Item("Circular", AutoPaintSettings.DIRECTION_CIRCULAR),
        new Item("Noise", AutoPaintSettings.DIRECTION_NOISE),
    });

    AutoPaintPanel() {
        super(new GridBagLayout());
        var gbh = new GridBagHelper(this);

        gbh.addParam(toolsParam, "toolSelector");
        gbh.addParam(strokeDirection);

        strokeCountTF = new JTextField("100");
        strokeCountTF.setName("strokeCountTF");
        gbh.addLabelAndControl(STROKE_COUNT_TEXT + ":",
            createPositiveIntLayer(
                STROKE_COUNT_TEXT, strokeCountTF));

        strokeLengthTF = new JTextField("100");
        gbh.addLabelAndControl(STROKE_LENGTH_TEXT + ":",
            createPositiveIntLayer(STROKE_LENGTH_TEXT, strokeLengthTF));

        gbh.addParam(lengthVariation);
        gbh.addParam(curvature);
        gbh.addParam(colorMode, "colorsCB");

        toolsParam.setupEnableOtherIf(colorMode, AutoPaint::useColors);
    }

    /**
     * Creates an {@link AutoPaintSettings} object from the current UI state.
     */
    public AutoPaintSettings getSettings() {
        boolean randomColors = false;
        boolean interpolatedColors = false;
        if (colorMode.isEnabled()) {
            String selectedMode = colorMode.getSelected();
            randomColors = selectedMode.equals(COLOR_MODE_RANDOM);
            interpolatedColors = selectedMode.equals(COLOR_MODE_INTERPOLATED);
        }

        return new AutoPaintSettings(getSelectedTool(),
            getStrokeCount(), getStrokeLength(), strokeDirection.getValue(),
            randomColors, interpolatedColors,
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
            .requirePositiveInt(strokeCountTF.getText(), STROKE_COUNT_TEXT)
            .requirePositiveInt(strokeLengthTF.getText(), STROKE_LENGTH_TEXT);
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

        if (AutoPaint.useColors(getSelectedTool())) {
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

        if (AutoPaint.useColors(getSelectedTool())) {
            FgBgColors.loadStateFrom(preset);
        }
    }

    @Override
    public String getPresetDirName() {
        return "Auto Paint";
    }
}

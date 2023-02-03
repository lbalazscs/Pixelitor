/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.utils.SliderSpinner.TextPosition;
import pixelitor.gui.utils.ValidatedPanel;
import pixelitor.gui.utils.ValidationResult;
import pixelitor.tools.Tool;

import javax.swing.*;
import java.awt.GridBagLayout;

import static java.lang.Integer.parseInt;
import static pixelitor.gui.utils.TextFieldValidator.createPositiveIntLayer;
import static pixelitor.tools.Tools.BRUSH;

/**
 * The GUI of the "Auto Paint" dialog
 */
public class AutoPaintPanel extends ValidatedPanel implements DialogMenuOwner {
    private static final String COLOR_FOREGROUND = "Foreground";
    private static final String COLOR_INTERPOLATED = "Foreground-Background Mix";
    private static final String COLOR_RANDOM = "Random";
    public static final String[] COLOR_CHOICES =
        {COLOR_INTERPOLATED, COLOR_FOREGROUND, COLOR_RANDOM};

    private static final String STROKE_LENGTH_TEXT = "Average Stroke Length";
    private static final String NUM_STROKES_TEXT = "Number of Strokes";

    private final ListParam<Tool> toolsParam = new ListParam<>(
        "Tool", AutoPaint.ALLOWED_TOOLS);
    private final JTextField numStrokesTF;
    private final JTextField lengthTF;
    private final ListParam<String> colorsParam = new ListParam<>(
        "Random Colors", COLOR_CHOICES);

    private final RangeParam lengthVariability = new RangeParam(
        "Stroke Length Variability (%)", 0, 50, 100, true, TextPosition.NONE);
    private final RangeParam maxCurvature = new RangeParam(
        "Maximal Curvature (%)", 0, 100, 300, true, TextPosition.NONE);

    private final IntChoiceParam angleType = new IntChoiceParam("Angle", new Item[]{
        new Item("Random", AutoPaintSettings.ANGLE_TYPE_RANDOM),
        new Item("Radial", AutoPaintSettings.ANGLE_TYPE_RADIAL),
        new Item("Circular", AutoPaintSettings.ANGLE_TYPE_CIRCULAR),
        new Item("Noise", AutoPaintSettings.ANGLE_TYPE_NOISE),
    });

    AutoPaintPanel() {
        super(new GridBagLayout());
        var gbh = new GridBagHelper(this);

        gbh.addParam(toolsParam, "toolSelector");
        gbh.addParam(angleType);

        numStrokesTF = new JTextField("100");
        numStrokesTF.setName("numStrokesTF");
        gbh.addLabelAndControl(NUM_STROKES_TEXT + ":",
            createPositiveIntLayer(
                NUM_STROKES_TEXT, numStrokesTF, false));

        lengthTF = new JTextField("100");
        gbh.addLabelAndControl(STROKE_LENGTH_TEXT + ":",
            createPositiveIntLayer(
                STROKE_LENGTH_TEXT, lengthTF, false));

        gbh.addParam(lengthVariability);
        gbh.addParam(maxCurvature);
        gbh.addParam(colorsParam, "colorsCB");

        toolsParam.setupEnableOtherIf(colorsParam, AutoPaintPanel::useColors);
    }

    private static boolean useColors(Tool selectedTool) {
        return selectedTool == BRUSH;
    }

    public AutoPaintSettings getSettings() {
        int numStrokes = getNumStrokes();
        int strokeLength = getStrokeLength();
        Tool tool = getSelectedTool();

        boolean colorsEnabled = colorsParam.isEnabled();
        String colors = colorsParam.getSelected();
        boolean randomColors = colorsEnabled && colors.equals(COLOR_RANDOM);
        boolean interpolatedColors = colorsEnabled && colors.equals(COLOR_INTERPOLATED);

        double lengthRandomnessPercentage = lengthVariability.getPercentage();
        double maxCurvaturePercentage = maxCurvature.getPercentage();

        return new AutoPaintSettings(tool, numStrokes, strokeLength, randomColors,
            lengthRandomnessPercentage, maxCurvaturePercentage, interpolatedColors,
            angleType.getValue());
    }

    private Tool getSelectedTool() {
        return toolsParam.getSelected();
    }

    private int getNumStrokes() {
        return parseInt(numStrokesTF.getText().trim());
    }

    private int getStrokeLength() {
        return parseInt(lengthTF.getText().trim());
    }

    @Override
    public ValidationResult validateSettings() {
        var retVal = ValidationResult.ok();
        try {
            int ns = getNumStrokes();
            retVal = retVal.addErrorIfZero(ns, NUM_STROKES_TEXT);
            retVal = retVal.addErrorIfNegative(ns, NUM_STROKES_TEXT);
        } catch (NumberFormatException e) {
            retVal = retVal.addError("\"" + NUM_STROKES_TEXT + "\" must be an integer.");
        }
        try {
            int ln = getStrokeLength();
            retVal = retVal.addErrorIfZero(ln, STROKE_LENGTH_TEXT);
            retVal = retVal.addErrorIfNegative(ln, STROKE_LENGTH_TEXT);
        } catch (NumberFormatException e) {
            retVal = retVal.addError("\"" + STROKE_LENGTH_TEXT + "\" must be an integer.");
        }
        return retVal;
    }

    @Override
    public boolean canHaveUserPresets() {
        return true;
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        toolsParam.saveStateTo(preset);
        angleType.saveStateTo(preset);

        preset.putInt(NUM_STROKES_TEXT, getNumStrokes());
        preset.putInt(STROKE_LENGTH_TEXT, getStrokeLength());

        lengthVariability.saveStateTo(preset);
        maxCurvature.saveStateTo(preset);
        colorsParam.saveStateTo(preset);

        if (useColors(getSelectedTool())) {
            FgBgColors.saveStateTo(preset);
        }
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        toolsParam.loadStateFrom(preset);
        angleType.loadStateFrom(preset);

        numStrokesTF.setText(preset.get(NUM_STROKES_TEXT));
        lengthTF.setText(preset.get(STROKE_LENGTH_TEXT));

        lengthVariability.loadStateFrom(preset);
        maxCurvature.loadStateFrom(preset);
        colorsParam.loadStateFrom(preset);

        if (useColors(getSelectedTool())) {
            FgBgColors.loadStateFrom(preset);
        }
    }

    @Override
    public String getPresetDirName() {
        return "Auto Paint";
    }
}

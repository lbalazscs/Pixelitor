/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.SliderSpinner;
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
public class AutoPaintPanel extends ValidatedPanel {
    private static final String COLOR_FOREGROUND = "Foreground";
    private static final String COLOR_INTERPOLATED = "Foreground-Background Mix";
    private static final String COLOR_RANDOM = "Random";
    public static final String[] COLOR_CHOICES =
        {COLOR_INTERPOLATED, COLOR_FOREGROUND, COLOR_RANDOM};

    private final JComboBox<Tool> toolSelector;

    private final JTextField numStrokesTF;

    private final JTextField lengthTF;

    private final JComboBox<String> colorsCB;
    private final JLabel colorsLabel;

    private final RangeParam lengthVariability =
        new RangeParam("", 0, 50, 100);
    private final RangeParam maxCurvature =
        new RangeParam("", 0, 100, 300);

    private final IntChoiceParam angleType = new IntChoiceParam("", new Item[]{
        new Item("Random", AutoPaintSettings.ANGLE_TYPE_RANDOM),
        new Item("Radial", AutoPaintSettings.ANGLE_TYPE_RADIAL),
        new Item("Circular", AutoPaintSettings.ANGLE_TYPE_CIRCULAR),
        new Item("Noise", AutoPaintSettings.ANGLE_TYPE_NOISE),
    });

    AutoPaintPanel() {
        super(new GridBagLayout());
        var gbh = new GridBagHelper(this);

        toolSelector = new JComboBox<>(AutoPaint.ALLOWED_TOOLS);
        toolSelector.setName("toolSelector");
        gbh.addLabelAndControl("Tool:", toolSelector);

        gbh.addLabelAndControl("Angle:", angleType.createGUI());

        numStrokesTF = new JTextField("100");
        numStrokesTF.setName("numStrokesTF");
        gbh.addLabelAndControl("Number of Strokes:",
            createPositiveIntLayer(
                "Number of Strokes", numStrokesTF, false));

        lengthTF = new JTextField("100");
        gbh.addLabelAndControl("Average Stroke Length:",
            createPositiveIntLayer(
                "Average Stroke Length", lengthTF, false));

        gbh.addLabelAndControl("Stroke Length Variability (%):",
            SliderSpinner.from(lengthVariability));

        gbh.addLabelAndControl("Maximal Curvature (%):",
            SliderSpinner.from(maxCurvature));

        colorsLabel = new JLabel("Random Colors:");
        colorsCB = new JComboBox<>(COLOR_CHOICES);
        colorsCB.setName("colorsCB");
        gbh.addTwoComponents(colorsLabel, colorsCB);

        toolSelector.addActionListener(e -> updateRandomColorsEnabledState());
        updateRandomColorsEnabledState();
    }

    private void updateRandomColorsEnabledState() {
        Tool tool = (Tool) toolSelector.getSelectedItem();
        if (tool == BRUSH) {
            colorsLabel.setEnabled(true);
            colorsCB.setEnabled(true);
        } else {
            colorsLabel.setEnabled(false);
            colorsCB.setEnabled(false);
        }
    }

    public AutoPaintSettings getSettings() {
        int numStrokes = getNumStrokes();
        int strokeLength = getStrokeLength();

        Tool tool = (Tool) toolSelector.getSelectedItem();

        boolean colorsEnabled = colorsCB.isEnabled();
        String colors = (String) colorsCB.getSelectedItem();
        boolean randomColors = colorsEnabled && colors.equals(COLOR_RANDOM);
        boolean interpolatedColors = colorsEnabled && colors.equals(COLOR_INTERPOLATED);

        float lengthRandomnessPercentage = lengthVariability.getPercentageValF();
        float maxCurvaturePercentage = maxCurvature.getPercentageValF();

        return new AutoPaintSettings(tool, numStrokes, strokeLength, randomColors,
            lengthRandomnessPercentage, maxCurvaturePercentage, interpolatedColors,
            angleType.getValue());
    }

    private int getNumStrokes() {
        return parseInt(numStrokesTF.getText().trim());
    }

    private int getStrokeLength() {
        return parseInt(lengthTF.getText().trim());
    }

    @Override
    public ValidationResult checkValidity() {
        var retVal = ValidationResult.ok();
        try {
            int ns = getNumStrokes();
            retVal = retVal.addErrorIfZero(ns, "Number of Strokes");
            retVal = retVal.addErrorIfNegative(ns, "Number of Strokes");
        } catch (NumberFormatException e) {
            retVal = retVal.addError("\"Number of Strokes\" must be an integer.");
        }
        try {
            int ln = getStrokeLength();
            retVal = retVal.addErrorIfZero(ln, "Average Stroke Length");
            retVal = retVal.addErrorIfNegative(ln, "Average Stroke Length");
        } catch (NumberFormatException e) {
            retVal = retVal.addError("\"Average Stroke Length\" must be an integer.");
        }
        return retVal;
    }
}

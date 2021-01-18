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

import net.jafama.FastMath;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.*;
import pixelitor.history.History;
import pixelitor.history.ImageEdit;
import pixelitor.layers.Drawable;
import pixelitor.tools.AbstractBrushTool;
import pixelitor.tools.Tool;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Messages;
import pixelitor.utils.ProgressHandler;

import javax.swing.*;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static pixelitor.colors.FgBgColors.*;
import static pixelitor.gui.utils.TextFieldValidator.createPositiveIntLayer;
import static pixelitor.tools.Tools.*;
import static pixelitor.utils.Texts.i18n;
import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.threadInfo;

/**
 * The "Auto Paint" functionality
 */
public class AutoPaint {
    public static final Tool[] ALLOWED_TOOLS = {SMUDGE, BRUSH, CLONE, ERASER};
    private static Color origFg;
    private static Color origBg;

    private AutoPaint() {
    }

    public static void showDialog(Drawable dr) {
        var configPanel = new ConfigPanel();
        new DialogBuilder()
            .validatedContent(configPanel)
            .title(i18n("auto_paint"))
            .okAction(() -> paintStrokes(dr, configPanel.getSettings()))
            .show();
    }

    private static void paintStrokes(Drawable dr, Settings settings) {
        assert calledOnEDT() : threadInfo();

        saveOriginalFgBgColors();
        History.setIgnoreEdits(true);
        BufferedImage backupImage = dr.getSelectedSubImage(true);

        String msg = format("Auto Paint with %s Tool: ", settings.getTool());
        var progressHandler = Messages.startProgress(msg, settings.getNumStrokes());

        try {
            runStrokes(settings, dr, progressHandler);
        } catch (Exception e) {
            Messages.showException(e);
        } finally {
            History.setIgnoreEdits(false);
            History.add(new ImageEdit("Auto Paint", dr.getComp(),
                dr, backupImage, false));

            progressHandler.stopProgress();
            Messages.showPlainInStatusBar(msg + "finished.");

            restoreOriginalFgBgColors(settings);
        }
    }

    private static void runStrokes(Settings settings,
                                   Drawable dr,
                                   ProgressHandler progressHandler) {
        assert calledOnEDT() : threadInfo();

        var random = new Random();
        var comp = dr.getComp();

        int numStrokes = settings.getNumStrokes();
        for (int i = 0; i < numStrokes; i++) {
            progressHandler.updateProgress(i);

            paintSingleStroke(dr, settings, comp, random);
            comp.getView().paintImmediately();
        }
    }

    private static void paintSingleStroke(Drawable dr,
                                          Settings settings,
                                          Composition comp,
                                          Random rand) {
        assert calledOnEDT() : threadInfo();

        setFgBgColors(settings, rand);

        PPoint start = calcRandomStartPoint(comp, rand);
        PPoint end = calcRandomEndPoint(start, comp, settings, rand);

        drawBrushStroke(dr, start, end, settings);
    }

    private static void setFgBgColors(Settings settings, Random rand) {
        if (settings.useRandomColors()) {
            randomizeColors();
        } else if (settings.useInterpolatedColors()) {
            Color interpolated = Colors.rgbInterpolate(
                origFg, origBg, rand.nextFloat());
            setFGColor(interpolated);
        }
    }

    private static PPoint calcRandomStartPoint(Composition comp, Random rand) {
        Canvas canvas = comp.getCanvas();
        return PPoint.lazyFromIm(
            rand.nextInt(canvas.getWidth()),
            rand.nextInt(canvas.getHeight()),
            comp.getView()
        );
    }

    private static PPoint calcRandomEndPoint(PPoint start, Composition comp,
                                             Settings settings, Random rand) {
        int strokeLength = settings.genStrokeLength();
        double angle = rand.nextDouble() * 2 * Math.PI;
        double endX = start.getImX() + strokeLength * FastMath.cos(angle);
        double endY = start.getImY() + strokeLength * FastMath.sin(angle);
        return PPoint.lazyFromIm(endX, endY, comp.getView());
    }

    private static void drawBrushStroke(Drawable dr,
                                        PPoint start, PPoint end,
                                        Settings settings) {
        Tool tool = settings.getTool();
        // tool.randomize();
        if (tool instanceof AbstractBrushTool) {
            AbstractBrushTool abt = (AbstractBrushTool) tool;
            abt.drawBrushStrokeProgrammatically(dr, start, end);
        } else {
            throw new IllegalStateException("tool = " + tool.getClass().getName());
        }
    }

    /**
     * The GUI of the "Auto Paint" dialog
     */
    public static class ConfigPanel extends ValidatedPanel {
        private static final String COL_FOREGROUND = "Foreground";
        private static final String COL_INTERPOLATED = "Foreground-Background Mix";
        private static final String COL_RANDOM = "Random";
        public static final String[] COLOR_SETTINGS =
            {COL_FOREGROUND, COL_INTERPOLATED, COL_RANDOM};

        private final JComboBox<Tool> toolSelector;
        private static Tool defaultTool = SMUDGE;

        private final JTextField numStrokesTF;
        private static int defaultNumStrokes = 100;

        private final JTextField lengthTF;
        private static int defaultLength = 100;

        private final JComboBox<String> colorsCB;
        private final JLabel colorsLabel;

        private static int defaultLengthVariability = 50;
        private final RangeParam lengthVariability =
            new RangeParam("", 0, defaultLengthVariability, 100);

        private static String defaultColors = COL_INTERPOLATED;

        private ConfigPanel() {
            super(new GridBagLayout());
            var gbh = new GridBagHelper(this);

            toolSelector = new JComboBox<>(ALLOWED_TOOLS);
            toolSelector.setSelectedItem(defaultTool);
            toolSelector.setName("toolSelector");
            gbh.addLabelAndControl("Tool:", toolSelector);

            numStrokesTF = new JTextField(String.valueOf(defaultNumStrokes));
            numStrokesTF.setName("numStrokesTF");
            gbh.addLabelAndControl("Number of Strokes:",
                createPositiveIntLayer(
                    "Number of Strokes", numStrokesTF, false));

            lengthTF = new JTextField(String.valueOf(defaultLength));
            gbh.addLabelAndControl("Average Stroke Length:",
                createPositiveIntLayer(
                    "Average Stroke Length", lengthTF, false));

            lengthVariability.setValueNoTrigger(defaultLengthVariability);
            gbh.addLabelAndControl("Stroke Length Variability (%):",
                SliderSpinner.from(lengthVariability));


            colorsLabel = new JLabel("Random Colors:");
            colorsCB = new JComboBox<>(COLOR_SETTINGS);
            colorsCB.setName("colorsCB");
            colorsCB.setSelectedItem(defaultColors);
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

        public Settings getSettings() {
            int numStrokes = getNumStrokes();
            defaultNumStrokes = numStrokes;

            int strokeLength = getStrokeLength();
            defaultLength = strokeLength;

            Tool tool = (Tool) toolSelector.getSelectedItem();
            defaultTool = tool;

            boolean colorsEnabled = colorsCB.isEnabled();
            String colors = (String) colorsCB.getSelectedItem();
            boolean randomColors = colorsEnabled && colors.equals(COL_RANDOM);
            boolean interpolatedColors = colorsEnabled && colors.equals(COL_INTERPOLATED);
            defaultColors = colors;

            float lengthRandomnessPercentage = lengthVariability.getPercentageValF();
            defaultLengthVariability = lengthVariability.getValue();

            return new Settings(tool, numStrokes, strokeLength,
                randomColors, lengthRandomnessPercentage, interpolatedColors);
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

    private static void saveOriginalFgBgColors() {
        origFg = getFGColor();
        origBg = getBGColor();
    }

    private static void restoreOriginalFgBgColors(Settings settings) {
        // if colors were changed, restore the original
        if (settings.changeColors()) {
            setFGColor(origFg);
            setBGColor(origBg);
        }
    }

    /**
     * The settings of Auto Paint
     */
    private static class Settings {
        private final Tool tool;
        private final int numStrokes;
        private final int minStrokeLength;
        private final int maxStrokeLength;
        private final boolean randomColors;
        private final boolean interpolatedColors;

        private Settings(Tool tool, int numStrokes, int strokeLength,
                         boolean randomColors, float lengthVariability,
                         boolean interpolatedColors) {
            this.tool = tool;
            this.numStrokes = numStrokes;

            if (lengthVariability == 0.0f) {
                minStrokeLength = strokeLength;
                maxStrokeLength = strokeLength;
            } else {
                minStrokeLength = (int) (strokeLength - lengthVariability * strokeLength);
                maxStrokeLength = (int) (strokeLength + lengthVariability * strokeLength);
            }

            this.randomColors = randomColors;
            this.interpolatedColors = interpolatedColors;
        }

        public Tool getTool() {
            return tool;
        }

        public int getNumStrokes() {
            return numStrokes;
        }

        public int genStrokeLength() {
            if (minStrokeLength == maxStrokeLength) {
                return minStrokeLength;
            } else {
                return ThreadLocalRandom.current()
                    .nextInt(minStrokeLength, maxStrokeLength + 1);
            }
        }

        public boolean useRandomColors() {
            return randomColors;
        }

        public boolean useInterpolatedColors() {
            return interpolatedColors;
        }

        public boolean changeColors() {
            return randomColors || interpolatedColors;
        }
    }
}

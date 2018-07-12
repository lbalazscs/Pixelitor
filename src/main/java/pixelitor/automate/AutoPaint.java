/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.MessageHandler;
import pixelitor.colors.ColorUtils;
import pixelitor.colors.FgBgColors;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.IntTextField;
import pixelitor.gui.utils.OKCancelDialog;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.history.History;
import pixelitor.history.ImageEdit;
import pixelitor.layers.Drawable;
import pixelitor.tools.AbstractBrushTool;
import pixelitor.tools.Tool;
import pixelitor.tools.shapes.ShapesTool;
import pixelitor.tools.util.ImDrag;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static pixelitor.tools.Tools.BRUSH;
import static pixelitor.tools.Tools.CLONE;
import static pixelitor.tools.Tools.ERASER;
import static pixelitor.tools.Tools.SMUDGE;

/**
 * The "Auto Paint" functionality
 */
public class AutoPaint {
    public static final Tool[] ALLOWED_TOOLS = {
            SMUDGE, BRUSH, CLONE, ERASER};
    private static Color origFg;
    private static Color origBg;

    private AutoPaint() {
    }

    public static void showDialog() {
        showDialog(ImageComponents.getActiveDrawableOrThrow());
    }

    public static void showDialog(Drawable dr) {
        ConfigPanel configPanel = new ConfigPanel();
        JDialog d = new OKCancelDialog(configPanel, "Auto Paint") {
            @Override
            protected void okAction() {
                close();
                Settings settings = configPanel.getSettings();
                paintStrokes(dr, settings);
            }
        };
        d.setVisible(true);
    }

    private static void paintStrokes(Drawable dr, Settings settings) {
        assert SwingUtilities.isEventDispatchThread() : "not EDT thread";

        origFg = FgBgColors.getFG();
        origBg = FgBgColors.getBG();

        String msg = String.format("Auto Paint with %s Tool: ", settings.getTool());

        MessageHandler msgHandler = Messages.getMessageHandler();
        msgHandler.startProgress(msg, settings.getNumStrokes());

        BufferedImage backupImage = dr.getImageOrSubImageIfSelected(true, true);
        History.setIgnoreEdits(true);

        try {
            runStrokes(settings, dr, msgHandler);
        } finally {
            History.setIgnoreEdits(false);
            ImageEdit edit = new ImageEdit("Auto Paint", dr.getComp(),
                    dr, backupImage, false, false);
            History.addEdit(edit);
            msgHandler.stopProgress();
            msgHandler.showInStatusBar(msg + "finished.");

            // if colors were changed, restore the original
            if (settings.changeColors()) {
                FgBgColors.setFG(origFg);
                FgBgColors.setBG(origBg);
            }
        }
    }

    private static void runStrokes(Settings settings,
                                   Drawable dr,
                                   MessageHandler msgHandler) {
        Random random = new Random();

        Composition comp = dr.getComp();

        ImageComponent ic = comp.getIC();

        int numStrokes = settings.getNumStrokes();
        for (int i = 0; i < numStrokes; i++) {
            msgHandler.updateProgress(i);

            paintSingleStroke(dr, settings, comp, random);
            ic.paintImmediately(ic.getBounds());
        }
    }

    private static void paintSingleStroke(Drawable dr,
                                          Settings settings,
                                          Composition comp,
                                          Random rand) {
        if (settings.useRandomColors()) {
            FgBgColors.randomize();
        } else if (settings.useInterpolatedColors()) {
            float interpolationRatio = rand.nextFloat();
            Color interpolated = ColorUtils.interpolateInRGB(origFg, origBg, interpolationRatio);
            FgBgColors.setFG(interpolated);
        }

        int strokeLength = settings.genStrokeLength();

        ImageComponent ic = comp.getIC();
        Canvas canvas = comp.getCanvas();
        int canvasWidth = canvas.getImWidth();
        int canvasHeight = canvas.getImHeight();
        PPoint start = new PPoint.Image(ic,
                rand.nextInt(canvasWidth),
                rand.nextInt(canvasHeight));

        double angle = rand.nextDouble() * 2 * Math.PI;
        double endX = start.getImX() + strokeLength * FastMath.cos(angle);
        double endY = start.getImY() + strokeLength * FastMath.sin(angle);
        PPoint end = new PPoint.Image(ic, endX, endY);

        try {
            Tool tool = settings.getTool();
            // tool.randomize();
            if (tool instanceof AbstractBrushTool) {
                AbstractBrushTool abt = (AbstractBrushTool) tool;
                abt.drawBrushStrokeProgrammatically(dr, start, end);
            } else if (tool instanceof ShapesTool) {
                ShapesTool st = (ShapesTool) tool;
                st.paintDrag(dr, new ImDrag(start, end));
            } else {
                throw new IllegalStateException("tool = "
                        + tool.getClass().getName());
            }
        } catch (Exception e) {
            Messages.showException(e);
        }
    }

    /**
     * The GUI of the "Auto Paint" dialog
     */
    public static class ConfigPanel extends JPanel {

        private static final String COL_FOREGROUND = "Foreground";
        private static final String COL_INTERPOLATED = "Foreground-Background Mix";
        private static final String COL_RANDOM = "Random";
        public static final String[] COLOR_SETTINGS = {
                COL_FOREGROUND, COL_INTERPOLATED, COL_RANDOM};

        private final JComboBox<Tool> toolSelector;
        private static Tool defaultTool = SMUDGE;

        private final IntTextField numStrokesTF;
        private static int defaultNumStrokes = 100;

        private final IntTextField lengthTF;
        private static int defaultLength = 100;

        private final JComboBox<String> colorsCB;
        private final JLabel colorsLabel;

        private static int defaultLengthVariability = 50;
        private final RangeParam lengthVariability =
                new RangeParam("", 0, defaultLengthVariability, 100);

        private static String defaultColors = COL_INTERPOLATED;

        private ConfigPanel() {
            super(new GridBagLayout());
            GridBagHelper gbh = new GridBagHelper(this);

            toolSelector = new JComboBox<>(ALLOWED_TOOLS);
            toolSelector.setSelectedItem(defaultTool);
            toolSelector.setName("toolSelector");
            gbh.addLabelWithControl("Tool:", toolSelector);

            numStrokesTF = new IntTextField(String.valueOf(defaultNumStrokes));
            numStrokesTF.setName("numStrokesTF");
            gbh.addLabelWithControl("Number of Strokes:", numStrokesTF);

            lengthTF = new IntTextField(String.valueOf(defaultLength));
            gbh.addLabelWithControl("Stroke Length Average:", lengthTF);

            lengthVariability.setValueNoTrigger(defaultLengthVariability);
            gbh.addLabelWithControl("Stroke Length Variability:",
                    new SliderSpinner(lengthVariability,
                            SliderSpinner.TextPosition.NONE, false));


            colorsLabel = new JLabel("Random Colors:");
            colorsCB = new JComboBox(COLOR_SETTINGS);
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
            int numStrokes = numStrokesTF.getIntValue();
            defaultNumStrokes = numStrokes;

            int strokeLength = lengthTF.getIntValue();
            defaultLength = strokeLength;

            Tool tool = (Tool) toolSelector.getSelectedItem();
            defaultTool = tool;

            boolean colorsEnabled = colorsCB.isEnabled();
            String colorsSelected = (String) colorsCB.getSelectedItem();
            boolean randomColors = colorsEnabled && colorsSelected.equals(COL_RANDOM);
            boolean interpolatedColors = colorsEnabled && colorsSelected.equals(COL_INTERPOLATED);
            defaultColors = colorsSelected;

            float lengthRandomnessPercentage = lengthVariability.getValueAsPercentage();
            defaultLengthVariability = lengthVariability.getValue();

            return new Settings(tool, numStrokes, strokeLength,
                    randomColors, lengthRandomnessPercentage, interpolatedColors);
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
                         boolean randomColors, float lengthVariability, boolean interpolatedColors) {
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
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                return rnd.nextInt(minStrokeLength, maxStrokeLength + 1);
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

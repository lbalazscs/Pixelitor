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
import pixelitor.Composition;
import pixelitor.MessageHandler;
import pixelitor.colors.FgBgColors;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.IntTextField;
import pixelitor.gui.utils.OKCancelDialog;
import pixelitor.history.History;
import pixelitor.history.ImageEdit;
import pixelitor.layers.Drawable;
import pixelitor.tools.AbstractBrushTool;
import pixelitor.tools.Tool;
import pixelitor.tools.UserDrag;
import pixelitor.tools.shapestool.ShapesTool;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.Point;
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
    //    public static final Tool[] ALLOWED_TOOLS = {SMUDGE, BRUSH, CLONE, ERASER, SHAPES};
    public static final Tool[] ALLOWED_TOOLS = {SMUDGE, BRUSH, CLONE, ERASER};

    private AutoPaint() {
    }

    public static void showDialog(Drawable dr) {
        ConfigPanel configPanel = new ConfigPanel();
        JDialog d = new OKCancelDialog(configPanel, "Auto Paint") {
            @Override
            protected void dialogAccepted() {
                close();
                Settings settings = configPanel.getSettings();
                paintStrokes(dr, settings);
            }
        };
        d.setVisible(true);
    }

    private static void paintStrokes(Drawable dr, Settings settings) {
        assert SwingUtilities.isEventDispatchThread() : "not EDT thread";

        Color origFg = null;
        Color origBg = null;
        if (settings.useRandomColors()) {
            origFg = FgBgColors.getFG();
            origBg = FgBgColors.getBG();
        }

        String msg = String.format("Auto Paint with %s Tool: ", settings.getTool());

        MessageHandler msgHandler = Messages.getMessageHandler();
        msgHandler.startProgress(msg, settings.getNumStrokes());

        BufferedImage backupImage = dr.getImageOrSubImageIfSelected(true, true);
        History.setIgnoreEdits(true);

        try {
            runStrokes(settings, dr, msgHandler);
        } finally {
            History.setIgnoreEdits(false);
            ImageEdit edit = new ImageEdit(dr.getComp(), "Auto Paint",
                    dr, backupImage, false, false);
            History.addEdit(edit);
            msgHandler.stopProgress();
            msgHandler.showStatusMessage(msg + "finished.");

            if (settings.useRandomColors()) {
                FgBgColors.setFG(origFg);
                FgBgColors.setBG(origBg);
            }
        }
    }

    private static void runStrokes(Settings settings, Drawable dr, MessageHandler msgHandler) {
        Random random = new Random();

        Composition comp = dr.getComp();

        ImageComponent ic = comp.getIC();

        int numStrokes = settings.getNumStrokes();
        for (int i = 0; i < numStrokes; i++) {
            msgHandler.updateProgress(i);

            paintSingleStroke(dr, settings,
                    comp.getCanvasWidth(), comp.getCanvasHeight(), random);
            ic.paintImmediately(ic.getBounds());
        }
    }

    private static void paintSingleStroke(Drawable dr, Settings settings, int canvasWidth, int canvasHeight, Random rand) {
        if (settings.useRandomColors()) {
            FgBgColors.randomize();
        }

        int strokeLength = settings.genStrokeLength();

        Point start = new Point(rand.nextInt(canvasWidth), rand.nextInt(canvasHeight));
        double randomAngle = rand.nextDouble() * 2 * Math.PI;
        int endX = (int) (start.x + strokeLength * FastMath.cos(randomAngle));
        int endY = (int) (start.y + strokeLength * FastMath.sin(randomAngle));

        Point end = new Point(endX, endY);

        try {
            Tool tool = settings.getTool();
            // tool.randomize();
            if (tool instanceof AbstractBrushTool) {
                AbstractBrushTool abt = (AbstractBrushTool) tool;
                abt.drawBrushStrokeProgrammatically(dr, start, end);
            } else if (tool instanceof ShapesTool) {
                ShapesTool st = (ShapesTool) tool;
                st.paintShapeOnIC(dr, new UserDrag(start.x, start.y, end.x, end.y));
            } else {
                throw new IllegalStateException("tool = " + tool.getClass().getName());
            }
        } catch (Exception e) {
            Messages.showException(e);
        }
    }

    /**
     * The GUI of the "Auto Paint" dialog
     */
    private static class ConfigPanel extends JPanel {
        private final JComboBox<Tool> toolSelector;
        private static Tool defaultTool = SMUDGE;

        private final IntTextField numStrokesTF;
        private static int defaultNumStrokes = 100;

        private final IntTextField lengthTF;
        private static int defaultLength = 100;

        private final JCheckBox randomColorsCB;
        private final JLabel randomColorsLabel;

        private static int defaultLengthVariability = 50;
        private final RangeParam lengthVariability = new RangeParam("", 0, defaultLengthVariability, 100);

        private static boolean defaultRandomColors = true;

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

            // TODO stroke length variability
//            gbh.addLabelWithControl("Stroke Length Average:", lengthTF);
            gbh.addLabelWithControl("Stroke Length:", lengthTF);
//
            // TODO stroke length variability

//            lengthVariability.setValueNoTrigger(defaultLengthVariability);
//            gbh.addLabelWithControl("Stroke Length Variability:",
//                    new SliderSpinner(lengthVariability,
//                            SliderSpinner.TextPosition.NONE, false));

            randomColorsLabel = new JLabel("Random Colors:");
            randomColorsCB = new JCheckBox();
            randomColorsCB.setSelected(defaultRandomColors);
            gbh.addTwoControls(randomColorsLabel, randomColorsCB);

            toolSelector.addActionListener(e -> updateRandomColorsEnabledState());
            updateRandomColorsEnabledState();
        }

        private void updateRandomColorsEnabledState() {
            Tool tool = (Tool) toolSelector.getSelectedItem();
            if (tool == BRUSH) {
                randomColorsLabel.setEnabled(true);
                randomColorsCB.setEnabled(true);
            } else {
                randomColorsLabel.setEnabled(false);
                randomColorsCB.setEnabled(false);
            }
        }

        public Settings getSettings() {
            int numStrokes = numStrokesTF.getIntValue();
            defaultNumStrokes = numStrokes;

            int strokeLength = lengthTF.getIntValue();
            defaultLength = strokeLength;

            Tool tool = (Tool) toolSelector.getSelectedItem();
            defaultTool = tool;

            boolean randomColorsEnabled = randomColorsCB.isEnabled();
            boolean randomColorsSelected = randomColorsCB.isSelected();
            boolean randomColors = randomColorsEnabled && randomColorsSelected;
            defaultRandomColors = randomColorsSelected;

            float lengthRandomnessPercentage = lengthVariability.getValueAsPercentage();
            defaultLengthVariability = lengthVariability.getValue();

            // TODO stroke length variability
//            return new Settings(tool, numStrokes, strokeLength, randomColors, lengthRandomnessPercentage);
            return new Settings(tool, numStrokes, strokeLength, randomColors, 0);
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

        private Settings(Tool tool, int numStrokes, int strokeLength, boolean randomColors, float lengthVariability) {
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
                return ThreadLocalRandom.current().nextInt(minStrokeLength, maxStrokeLength + 1);
            }
        }

        public boolean useRandomColors() {
            return randomColors;
        }
    }
}

/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.FgBgColors;
import pixelitor.MessageHandler;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.IntTextField;
import pixelitor.gui.utils.OKCancelDialog;
import pixelitor.history.History;
import pixelitor.history.ImageEdit;
import pixelitor.layers.ImageLayer;
import pixelitor.selection.IgnoreSelection;
import pixelitor.tools.AbstractBrushTool;
import pixelitor.tools.Tool;
import pixelitor.tools.UserDrag;
import pixelitor.tools.shapestool.ShapesTool;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.Random;

import static pixelitor.tools.Tools.BRUSH;
import static pixelitor.tools.Tools.CLONE;
import static pixelitor.tools.Tools.ERASER;
import static pixelitor.tools.Tools.SMUDGE;

public class AutoPaint {
    //    public static final Tool[] ALLOWED_TOOLS = {SMUDGE, BRUSH, CLONE, ERASER, SHAPES};
    public static final Tool[] ALLOWED_TOOLS = {SMUDGE, BRUSH, CLONE, ERASER};

    private AutoPaint() {
    }

    public static void showDialog() {
        ConfigPanel configPanel = new ConfigPanel();
        JDialog d = new OKCancelDialog(configPanel, "Auto Paint") {
            @Override
            protected void dialogAccepted() {
                close();
                Settings settings = configPanel.getSettings();
                paintStrokes(settings);
            }
        };
        d.setVisible(true);
    }

    private static void paintStrokes(Settings settings) {
        assert SwingUtilities.isEventDispatchThread();

        String msg = String.format("Auto Paint with %s Tool: ", settings.getTool());

        MessageHandler messageHandler = Messages.getMessageHandler();
        messageHandler.startProgress(msg, settings.getNumStrokes());

        Composition comp = ImageComponents.getActiveComp().orElseThrow(() -> new RuntimeException("no active composition"));

        ImageLayer imageLayer = comp.getActiveMaskOrImageLayer();
        BufferedImage backupImage = imageLayer.getImageOrSubImageIfSelected(true, true);
        History.setIgnoreEdits(true);

        try {
            runStrokes(settings, comp, messageHandler);
        } finally {
            History.setIgnoreEdits(false);
            ImageEdit edit = new ImageEdit(comp, "Auto Paint",
                    imageLayer, backupImage, IgnoreSelection.NO, false);
            History.addEdit(edit);
            messageHandler.stopProgress();
            messageHandler.showStatusMessage(msg + "finished.");
        }
    }

    private static void runStrokes(Settings settings, Composition comp, MessageHandler messageHandler) {
        Random random = new Random();
        int canvasWidth = comp.getCanvasWidth();
        int canvasHeight = comp.getCanvasHeight();
        ImageComponent ic = comp.getIC();

        int numStrokes = settings.getNumStrokes();
        for (int i = 0; i < numStrokes; i++) {
            messageHandler.updateProgress(i);

            paintSingleStroke(comp, settings, canvasWidth, canvasHeight, random);
            ic.paintImmediately(ic.getBounds());
        }
    }

    private static void paintSingleStroke(Composition comp, Settings settings, int canvasWidth, int canvasHeight, Random rand) {
        if (settings.withRandomColors()) {
            FgBgColors.randomizeColors();
        }

        int strokeLength = settings.getStrokeLength();

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
                abt.drawBrushStrokeProgrammatically(comp, start, end);
            } else if (tool instanceof ShapesTool) {
                ShapesTool st = (ShapesTool) tool;
                st.paintShapeOnIC(comp, new UserDrag(start.x, start.y, end.x, end.y));
            } else {
                throw new IllegalStateException("tool = " + tool.getClass().getName());
            }
        } catch (Exception e) {
            Messages.showException(e);
        }
    }

    private static class ConfigPanel extends JPanel {
        private final JComboBox<Tool> toolSelector;
        private static Tool defaultTool = SMUDGE;

        private final IntTextField numStrokesTF;
        private static int defaultNumStrokes = 100;

        private final IntTextField lengthTF;
        private static int defaultLength = 100;

        private final JCheckBox randomColorsCB;
        private final JLabel randomColorsLabel;
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
            gbh.addLabelWithControl("Stroke Length:", lengthTF);

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

            return new Settings(tool, numStrokes, strokeLength, randomColors);
        }
    }

    private static class Settings {
        private final Tool tool;
        private final int numStrokes;
        private final int strokeLength;
        private final boolean randomColors;

        private Settings(Tool tool, int numStrokes, int strokeLength, boolean randomColors) {
            this.tool = tool;
            this.numStrokes = numStrokes;
            this.strokeLength = strokeLength;
            this.randomColors = randomColors;
        }

        public Tool getTool() {
            return tool;
        }

        public int getNumStrokes() {
            return numStrokes;
        }

        public int getStrokeLength() {
            return strokeLength;
        }

        public boolean withRandomColors() {
            return randomColors;
        }
    }
}

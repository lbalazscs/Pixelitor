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

package pixelitor.io;

import org.jdesktop.swingx.VerticalLayout;
import pd.AnimatedGifEncoder;
import pixelitor.Composition;
import pixelitor.gui.utils.*;
import pixelitor.layers.Layer;
import pixelitor.utils.Messages;
import pixelitor.utils.ProgressTracker;
import pixelitor.utils.StatusBarProgressTracker;

import javax.swing.*;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.awt.FlowLayout.LEFT;
import static java.lang.Integer.parseInt;
import static javax.swing.BorderFactory.createEmptyBorder;
import static pixelitor.io.FileChoosers.gifFilter;

/**
 * An animation based on the layers of a composition.
 */
public class LayerAnimation {
    // delay in milliseconds between the frames of the animation
    private final int frameDurationMs;

    private static final int DEFAULT_DELAY_MS = 200;

    // the list of images representing animation frames
    private final List<BufferedImage> frames = new ArrayList<>();

    private LayerAnimation(Composition comp, int frameDurationMs, boolean isPingPong) {
        this.frameDurationMs = frameDurationMs;

        generateFrames(comp, isPingPong);
    }

    private void generateFrames(Composition comp, boolean isPingPong) {
        int numLayers = comp.getNumLayers();

        // add frames in the forward direction
        for (int i = 0; i < numLayers; i++) {
            addFrameFromLayer(comp, i);
        }

        // add frames in reverse direction if ping-pong is enabled
        if (isPingPong && numLayers > 2) {
            for (int i = numLayers - 2; i > 0; i--) {
                addFrameFromLayer(comp, i);
            }
        }
    }

    private void addFrameFromLayer(Composition comp, int layerIndex) {
        Layer layer = comp.getLayer(layerIndex);
        BufferedImage layerImage = layer.toImage(true, true);
        if (layerImage != null) {
            frames.add(layerImage);
        }
    }

    /**
     * Shows a dialog for exporting the animation as an animated GIF.
     */
    public static void showExportDialog(Composition comp) {
        if (comp.getNumLayers() < 2) {
            Messages.showInfo("Not Enough Layers",
                "<html>Animation frames are based on the layers of the image." +
                    "<br>The image <b>" + comp.getName() + "</b> has only one layer.");
            return;
        }

        var configPanel = new ConfigPanel(comp.getNumLayers());
        new DialogBuilder()
            .title("Export Animated GIF")
            .validatedContent(configPanel)
            .okText("Export")
            .okAction(() -> exportDialogAccepted(comp, configPanel))
            .show();
    }

    private static void exportDialogAccepted(Composition comp, ConfigPanel config) {
        File outputFile = FileChoosers.selectSaveFileForFormat(
            comp.suggestFileName("gif"), gifFilter);

        if (outputFile != null) {
            new LayerAnimation(
                comp, config.getDelayMillis(), config.isPingPong())
                .saveToFile(outputFile);
            Messages.showFileSavedMessage(outputFile);
        }
    }

    private void saveToFile(File outputFile) {
        assert outputFile != null;

        GUIUtils.runWithBusyCursor(() -> writeToFile(outputFile));
    }

    private void writeToFile(File outputFile) {
        ProgressTracker pt = new StatusBarProgressTracker("Writing " + outputFile.getName(), frames.size());

        var encoder = new AnimatedGifEncoder();
        encoder.start(outputFile);
        encoder.setDelay(frameDurationMs);
        encoder.setRepeat(0); // infinite loop

        for (BufferedImage image : frames) {
            encoder.addFrame(image);
            pt.unitDone();
        }

        pt.finished();
        encoder.finish();
    }

    public static class ConfigPanel extends ValidatedPanel {
        private final JTextField delayTF;
        private final JCheckBox pingPongCB;

        public ConfigPanel(int numLayers) {
            super(new VerticalLayout(10));
            setBorder(createEmptyBorder(10, 10, 10, 10));

            add(new JLabel(" Animation frames are based on the layers of the image. "));

            var settingsPanel = new JPanel();
            settingsPanel.setLayout(new FlowLayout(LEFT, 10, 10));
            settingsPanel.add(new JLabel("Delay Between Frames (Milliseconds):"));
            delayTF = new JTextField(String.valueOf(DEFAULT_DELAY_MS), 4);
            settingsPanel.add(TextFieldValidator.createPositiveIntLayer(
                "Delay", delayTF));

            add(settingsPanel);

            if (numLayers > 2) {
                pingPongCB = new JCheckBox("Ping Pong Animation");
            } else {
                pingPongCB = new JCheckBox("Ping Pong Animation (requires at least 3 layers)");
                pingPongCB.setEnabled(false);
            }
            add(pingPongCB);
        }

        public int getDelayMillis() {
            return parseInt(delayTF.getText().trim());
        }

        public boolean isPingPong() {
            return pingPongCB.isSelected();
        }

        @Override
        public ValidationResult validateSettings() {
            return ValidationResult.valid()
                .requirePositiveInt(delayTF.getText(), "Delay");
        }
    }
}

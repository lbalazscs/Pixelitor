/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pd.AnimatedGifEncoder;
import pixelitor.Composition;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Layer;
import pixelitor.utils.ProgressTracker;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A layer animation (an animation based on the layers of a composition).
 */
public class LayerAnimation {
    // The delay between frames in milliseconds.
    private final int delayMillis;

    private final List<BufferedImage> images = new ArrayList<>();
    private final int numFrames;

    public LayerAnimation(Composition comp, int delayMillis, boolean isPingPong) {
        this.delayMillis = delayMillis;
        int numLayers = comp.getNumLayers();

        if (isPingPong && numLayers > 2) {
            numFrames = 2 * numLayers - 1;
        } else {
            numFrames = numLayers;
        }

        addFramesFromLayers(comp, isPingPong, numLayers);
    }

    private void addFramesFromLayers(Composition comp, boolean isPingPong, int numLayers) {
        // Add frames in the forward direction.
        for (int i = 0; i < numLayers; i++) {
            addLayerFrame(comp, i);
        }

        // Add frames in reverse direction if ping-pong is enabled.
        if (isPingPong && numLayers > 2) {
            for (int i = numLayers - 2; i > 0; i--) {
                addLayerFrame(comp, i);
            }
        }
    }

    private void addLayerFrame(Composition comp, int layerIndex) {
        Layer layer = comp.getLayer(layerIndex);
        BufferedImage layerImage = layer.asImage(true, true);
        if (layerImage != null) {
            images.add(layerImage);
        }
    }

    private void export(File f) {
        ProgressTracker pt = new StatusBarProgressTracker("Writing " + f.getName(), numFrames);

        var encoder = new AnimatedGifEncoder();
        encoder.start(f);
        encoder.setDelay(delayMillis);
        encoder.setRepeat(0);

        for (BufferedImage image : images) {
            encoder.addFrame(image);
            pt.unitDone();
        }

        pt.finished();
        encoder.finish();
    }

    public void saveToFile(File selectedFile) {
        assert selectedFile != null;

        GUIUtils.runWithBusyCursor(() -> export(selectedFile));
    }
}

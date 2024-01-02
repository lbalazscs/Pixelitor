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
 * A layer animation (an animation based on the layers of a composition)
 */
public class LayerAnimation {
    private final int delayMillis;
    private final List<BufferedImage> images = new ArrayList<>();
    private int numFrames;

    public LayerAnimation(Composition comp, int delayMillis, boolean pingPong) {
        this.delayMillis = delayMillis;

        int numLayers = comp.getNumLayers();
        numFrames = numLayers;
        if (pingPong && numLayers > 2) {
            numFrames += (numLayers - 1);
        }

        for (int i = 0; i < numLayers; i++) {
            addLayer(comp, i);
        }
        if (pingPong && numLayers > 2) {
            for (int i = numLayers - 2; i > 0; i--) {
                addLayer(comp, i);
            }
        }
    }

    private void addLayer(Composition comp, int layerIndex) {
        Layer layer = comp.getLayer(layerIndex);
        BufferedImage image = layer.asImage(true, true);
        if (image == null) {
            return;
        }
        images.add(image);
    }

    private void export(File f) {
        ProgressTracker pt = new StatusBarProgressTracker("Writing " + f.getName(), numFrames);

        AnimatedGifEncoder e = new AnimatedGifEncoder();
        e.start(f);
        e.setDelay(delayMillis);
        e.setRepeat(0);
        for (BufferedImage image : images) {
            e.addFrame(image);
            pt.unitDone();
        }
        pt.finished();
        e.finish();
    }

    public void saveToFile(File selectedFile) {
        assert selectedFile != null;

        GUIUtils.runWithBusyCursor(() -> export(selectedFile));
    }
}

/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.TextLayer;
import pixelitor.utils.ImageUtils;

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

    public LayerAnimation(Composition comp, int delayMillis, boolean pingPong) {
        this.delayMillis = delayMillis;
        addComposition(comp, pingPong);
    }

    private void addComposition(Composition comp, boolean pingPong) {
        int numLayers = comp.getNumLayers();
        for (int i = 0; i < numLayers; i++) {
            addLayerToAnimation(comp, i);
        }
        if (pingPong && numLayers > 2) {
            for (int i = numLayers - 2; i > 0; i--) {
                addLayerToAnimation(comp, i);
            }
        }
    }

    private void addLayerToAnimation(Composition comp, int layerIndex) {
        Layer layer = comp.getLayer(layerIndex);
        if (layer instanceof ImageLayer) {
            ImageLayer imageLayer = (ImageLayer) layer;
            BufferedImage image = imageLayer.getCanvasSizedSubImage();

            if (layer.hasMask()) {
                // TODO probably problems with translation
                image = ImageUtils.copyImage(image);
                layer.getMask().applyToImage(image);
            }

            images.add(image);
        } else if (layer instanceof TextLayer) {
            // TODO apply mask
            TextLayer textLayer = (TextLayer) layer;
            BufferedImage image = textLayer.createRasterizedImage();
            images.add(image);
        }
    }

    private void export(File f) {
        AnimatedGifEncoder e = new AnimatedGifEncoder();
        e.start(f);
        e.setDelay(delayMillis);
        e.setRepeat(0);
        images.forEach(e::addFrame);
        e.finish();
    }

    public void saveToFile(File selectedFile) {
        assert selectedFile != null;

        Runnable r = () -> export(selectedFile);
        GUIUtils.runWithBusyCursor(r);
    }
}

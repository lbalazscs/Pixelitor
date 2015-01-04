/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.utils.Utils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LayerAnimationFrames {
    private int delayMillis;
    List<BufferedImage> images = new ArrayList<>();

    public LayerAnimationFrames(Composition comp, int delayMillis) {
        this.delayMillis = delayMillis;
        addComposition(comp);
    }

    private void addComposition(Composition comp) {
        int nrLayers = comp.getNrLayers();
        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            if (layer instanceof ImageLayer) {
                ImageLayer imageLayer = (ImageLayer) layer;
                BufferedImage image = imageLayer.getImage();
                images.add(image);
            }
        }
    }

    private void export(File f) {
        AnimatedGifEncoder e = new AnimatedGifEncoder();
        e.start(f);
        e.setDelay(delayMillis);
        e.setRepeat(0);
        for (BufferedImage image : images) {
            e.addFrame(image);
        }
        boolean ok = e.finish();
    }

    public void saveToFile(final File selectedFile) {
        if (selectedFile == null) {
            throw new IllegalArgumentException("selectedFile is null");
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                export(selectedFile);
            }
        };
        Utils.executeWithBusyCursor(r);
    }
}

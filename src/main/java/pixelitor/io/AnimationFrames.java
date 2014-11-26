/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.io;

import pd.AnimatedGifEncoder;
import pixelitor.Composition;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.utils.Dialogs;
import pixelitor.utils.Utils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnimationFrames {
    private int delayMillis;
    List<BufferedImage> images = new ArrayList<>();

    public AnimationFrames(Composition comp, int delayMillis) {
        this.delayMillis = delayMillis;
        addComposition(comp);
    }

    private void addComposition(Composition comp) {
        int nrLayers = comp.getNrLayers();
        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            if (layer instanceof ImageLayer) {
                ImageLayer imageLayer = (ImageLayer) layer;
                BufferedImage image = imageLayer.getBufferedImage();
                images.add(image);
            }
        }
    }

    private void export(File f) throws IOException {
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
                try {
                    export(selectedFile);
                } catch (IOException e) {
                    Dialogs.showExceptionDialog(e);
                }
            }
        };
        Utils.executeWithBusyCursor(r, false);
    }

}

/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
package pixelitor.filters.animation;

import pd.AnimatedGifEncoder;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * An AnimationWriter implementation that writes an animated GIF file
 */
public class AnimGIFWriter implements AnimationWriter {
    private final AnimatedGifEncoder encoder;

    public AnimGIFWriter(File file, int delayMillis) {
        encoder = new AnimatedGifEncoder();
        encoder.start(file);
        encoder.setDelay(delayMillis);
        encoder.setRepeat(0);
    }

    @Override
    public void addFrame(BufferedImage image) {
        encoder.addFrame(image);
    }

    @Override
    public void finish() {
        encoder.finish();
    }

    @Override
    public void cancel() {
        encoder.cancel();
    }
}

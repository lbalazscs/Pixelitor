/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.io.TrackedIO;
import pixelitor.utils.Messages;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static java.lang.String.format;

/**
 * An {@link AnimationWriter} implementation
 * that writes a sequence of PNG files
 */
public class PNGFileSequenceWriter implements AnimationWriter {
    private final File outputDir;
    private int sequenceNumber;
    private int numWrittenImages = 0;

    public PNGFileSequenceWriter(File outputDir) {
        this.outputDir = outputDir;
    }

    @Override
    public void addFrame(BufferedImage image) throws IOException {
        String fileName = format("frame_%05d.png", sequenceNumber);
        sequenceNumber++;
        File outputFile = new File(outputDir, fileName);

        TrackedIO.write(image, "PNG", outputFile, null);
        numWrittenImages++;
    }

    @Override
    public void finish() {
        Messages.showFilesSavedMessage(numWrittenImages, outputDir);
    }

    @Override
    public void cancel() {

    }
}

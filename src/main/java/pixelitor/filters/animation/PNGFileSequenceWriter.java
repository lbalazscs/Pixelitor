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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * An AnimationWriter implementation that writes a sequence of PNG files
 */
public class PNGFileSequenceWriter implements AnimationWriter {
    private final File outputDir;
    private int fileSequenceNumber;

    public PNGFileSequenceWriter(File outputDir) {
        this.outputDir = outputDir;
    }

    @Override
    public void addFrame(BufferedImage image) throws IOException {
        String fileName = String.format("frame_%05d.png", fileSequenceNumber);
        fileSequenceNumber++;
        File outputFile = new File(outputDir, fileName);
        ImageIO.write(image, "PNG", outputFile);
    }

    @Override
    public void finish() {

    }

    @Override
    public void cancel() {

    }
}

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

import pixelitor.utils.Messages;
import pixelitor.utils.ProgressTracker;
import pixelitor.utils.StatusBarProgressTracker;
import pixelitor.utils.SubtaskProgressTracker;
import pixelitor.utils.TrackerWriteProgressListener;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static javax.imageio.ImageWriteParam.MODE_DEFAULT;
import static javax.imageio.ImageWriteParam.MODE_DISABLED;
import static javax.imageio.ImageWriteParam.MODE_EXPLICIT;

/**
 * Utility class with static methods related to writing JPEG images
 */
public final class JpegOutput {
    private JpegOutput() {
    }

    public static void save(BufferedImage image, JpegInfo config, File selectedFile) throws IOException {
        write(image, selectedFile, config);
    }

    private static void write(BufferedImage image, File file, JpegInfo config) throws IOException {
        ImageOutputStream ios = ImageIO.createImageOutputStream(file);
        if (ios == null) {
            TrackedIO.throwNoIOSErrorFor(file);
        }
        ProgressTracker tracker = new StatusBarProgressTracker("Writing " + file.getName(), 100);
        writeJPGtoStream(image, ios, config, tracker);
    }

    public static ImageWithSize writeJPGtoPreviewImage(BufferedImage image, JpegInfo config, ProgressTracker pt) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(32768);
        BufferedImage previewImage = null;
        byte[] bytes = null;
        try {
            // writes the JPEG format with the given settings to memory...
            // approximately 70% of the total time is spent here
            ImageOutputStream ios = ImageIO.createImageOutputStream(bos);
            ProgressTracker pt1 = new SubtaskProgressTracker(0.7, pt);
            writeJPGtoStream(image, ios, config, pt1);

            // ...then reads it back into an image
            // approximately 30% of the total time is spent here
            bytes = bos.toByteArray();
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ProgressTracker pt2 = new SubtaskProgressTracker(0.3, pt);
            try (ImageInputStream iis = ImageIO.createImageInputStream(in)) {
                previewImage = TrackedIO.readFromIIS(iis, pt2);
            }

            pt.finished();
        } catch (IOException e) {
            Messages.showException(e);
        }

        int sizeInBytes = bytes.length;

        return new ImageWithSize(previewImage, sizeInBytes);
    }

    private static void writeJPGtoStream(BufferedImage image,
                                         ImageInputStream ios,
                                         JpegInfo config,
                                         ProgressTracker tracker) throws IOException {
        Iterator<ImageWriter> jpgWriters = ImageIO.getImageWritersByFormatName("jpg");
        if (!jpgWriters.hasNext()) {
            throw new IllegalStateException("No JPG writers found");
        }
        ImageWriter writer = jpgWriters.next();

        ImageWriteParam imageWriteParam = writer.getDefaultWriteParam();

        if (config.isProgressive()) {
            imageWriteParam.setProgressiveMode(MODE_DEFAULT);
        } else {
            imageWriteParam.setProgressiveMode(MODE_DISABLED);
        }

        imageWriteParam.setCompressionMode(MODE_EXPLICIT);
        imageWriteParam.setCompressionQuality(config.getQuality());

        IIOImage iioImage = new IIOImage(image, null, null);

        writer.setOutput(ios);
        if (tracker != null) {
            writer.addIIOWriteProgressListener(new TrackerWriteProgressListener(tracker));
        }
        writer.write(null, iioImage, imageWriteParam);

        ios.flush();
        ios.close();
    }

    static class ImageWithSize {
        final BufferedImage image;
        final int size;

        private ImageWithSize(BufferedImage image, int size) {
            this.image = image;
            this.size = size;
        }

        public BufferedImage getImage() {
            return image;
        }

        public int getSize() {
            return size;
        }
    }
}
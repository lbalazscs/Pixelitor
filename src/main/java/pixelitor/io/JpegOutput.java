/*
 * Copyright 2017 Laszlo Balazs-Csiki
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

/**
 *
 */
public final class JpegOutput {
    /**
     * Utility class with static methods
     */
    private JpegOutput() {
    }

    public static void writeJPG(BufferedImage image, File file, JpegSettings settings) throws IOException {
        ImageOutputStream ios = ImageIO.createImageOutputStream(file);
        if (ios != null) {
            writeJPGtoStream(image, ios, settings);
        }
    }

    public static ImageWithSize writeJPGtoPreviewImage(BufferedImage image, JpegSettings settings) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(32768);
        BufferedImage previewImage = null;
        byte[] bytes = null;
        try {
            ImageOutputStream ios = ImageIO.createImageOutputStream(bos);
            writeJPGtoStream(image, ios, settings);

            bytes = bos.toByteArray();
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            previewImage = ImageIO.read(in);
        } catch (IOException e) {
            Messages.showException(e);
        }

        int sizeInBytes = bytes.length;

        return new ImageWithSize(previewImage, sizeInBytes);
    }

    private static void writeJPGtoStream(BufferedImage image, ImageInputStream ios, JpegSettings jpegSettings) throws IOException {
        Iterator<ImageWriter> jpgWriters = ImageIO.getImageWritersByFormatName("jpg");
        if (!jpgWriters.hasNext()) {
            throw new IllegalStateException("No JPG writers found");
        }
        ImageWriter writer = jpgWriters.next();

        ImageWriteParam imageWriteParam = writer.getDefaultWriteParam();

        if (jpegSettings.isProgressive()) {
            imageWriteParam.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
        } else {
            imageWriteParam.setProgressiveMode(ImageWriteParam.MODE_DISABLED);
        }

        imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        imageWriteParam.setCompressionQuality(jpegSettings.getQuality());

        IIOImage iioImage = new IIOImage(image, null, null);

        writer.setOutput(ios);
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
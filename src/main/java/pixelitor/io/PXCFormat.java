/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Composition;
import pixelitor.utils.*;

import javax.imageio.ImageIO;
import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.zip.GZIPInputStream;

import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;
import static pixelitor.utils.ImageUtils.getPixels;

/**
 * PXC file format support.
 */
public class PXCFormat {
    private static final int CURRENT_PXC_VERSION_NUMBER = 0x04;

    // the first version supporting a thumbnail
    private static final int THUMBNAIL_FORMAT_VERSION = 0x04;

    // tracks the writing of the whole file
    private static ProgressTracker mainPT;

    private static double workRatioForOneImage;

    private PXCFormat() {
    }

    public static Composition read(File file) throws BadPxcFormatException {
        ProgressTracker tracker = new StatusBarProgressTracker(
            "Reading " + file.getName(), (int) file.length());
        return read(file, tracker);
    }

    public static Composition read(File file, ProgressTracker tracker) throws BadPxcFormatException {
        Composition comp = null;
        try (InputStream is = new ProgressTrackingInputStream(new FileInputStream(file), tracker)) {
            int firstByte = is.read();
            int secondByte = is.read();
            if (firstByte == 0xAB && secondByte == 0xC4) {
                // identification bytes OK
            } else {
                throw new BadPxcFormatException(file.getName()
                    + " is not in the pxc format.");
            }
            int versionByte = is.read();
            if (versionByte == 0) {
                throw new BadPxcFormatException(file.getName()
                    + " is in an obsolete pxc format, "
                    + "it can only be opened in the old beta Pixelitor versions 0.9.2-0.9.7");
            }
            if (versionByte == 1) {
                throw new BadPxcFormatException(file.getName()
                    + " is in an obsolete pxc format, "
                    + "it can only be opened in the old beta Pixelitor version 0.9.8");
            }
            if (versionByte == 2) {
                throw new BadPxcFormatException(file.getName()
                    + " is in an obsolete pxc format, "
                    + "it can only be opened in the old Pixelitor versions 0.9.9-1.1.2");
            }
            if (versionByte > CURRENT_PXC_VERSION_NUMBER) {
                throw new BadPxcFormatException(file.getName()
                    + " has unknown version byte " + versionByte);
            }

            // Skip thumbnail data
            if (versionByte >= THUMBNAIL_FORMAT_VERSION) {
                // Read thumbnail length (4 bytes)
                int thumbnailLength = readInt(is);
                // Skip the thumbnail data
                is.skip(thumbnailLength);
            }

            if (versionByte == 3) { // gzipped stream in old pxc files
                try (GZIPInputStream gs = new GZIPInputStream(is)) {
                    try (ObjectInput ois = new ObjectInputStream(gs)) {
                        comp = (Composition) ois.readObject();
                    }
                }
            } else {
                try (ObjectInput ois = new ObjectInputStream(is)) {
                    comp = (Composition) ois.readObject();
                }
            }
            // file is transient in Composition because the pxc file can be renamed
            comp.setFile(file);

            EventQueue.invokeLater(comp::checkFontsAreInstalled);
        } catch (IOException | ClassNotFoundException e) {
            Messages.showException(e);
        }

        return comp;
    }

    public static void write(Composition comp, File file) {
        mainPT = new StatusBarProgressTracker(
            "Writing " + file.getName(), 100);
        int numImages = comp.countImages();
        if (numImages > 0) {
            workRatioForOneImage = 1.0 / numImages;
        } else {
            workRatioForOneImage = -1;
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            // write header bytes and version
            fos.write(new byte[]{(byte) 0xAB, (byte) 0xC4, CURRENT_PXC_VERSION_NUMBER});

            // write thumbnail
            BufferedImage thumbnail = OpenRaster.createORAThumbnail(comp.getCompositeImage());
            // create an extra stream so that we know the length of the thumbnail data
            ByteArrayOutputStream thumbnailBytes = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "PNG", thumbnailBytes);
            byte[] thumbnailData = thumbnailBytes.toByteArray();
            writeInt(fos, thumbnailData.length); // write thumbnail length
            fos.write(thumbnailData); // write thumbnail data

//            try (GZIPOutputStream gz = new GZIPOutputStream(fos)) {
//                try (ObjectOutput oos = new ObjectOutputStream(gz)) {
//                    oos.writeObject(comp);
//                    oos.flush();
//                }
//            }

            // since pxc version 4, the stream isn't gzipped
            try (ObjectOutput oos = new ObjectOutputStream(fos)) {
                oos.writeObject(comp);
                oos.flush();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        mainPT.finished();
        mainPT = null;
    }

    /**
     * Reads only the thumbnail from a PXC file.
     */
    public static BufferedImage readThumbnail(File file) throws BadPxcFormatException {
        try (InputStream is = new FileInputStream(file)) {
            int firstByte = is.read();
            int secondByte = is.read();
            if (firstByte != 0xAB || secondByte != 0xC4) {
                throw new BadPxcFormatException(file.getName() + " is not in the pxc format.");
            }

            int versionByte = is.read();
            if (versionByte != THUMBNAIL_FORMAT_VERSION) {
                return null; // old version, no thumbnail
            }

            int thumbnailLength = readInt(is);
            byte[] thumbnailData = new byte[thumbnailLength];
            is.read(thumbnailData);

            try (ByteArrayInputStream bais = new ByteArrayInputStream(thumbnailData)) {
                return ImageIO.read(bais);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void serializeImage(ObjectOutputStream out,
                                      BufferedImage img) throws IOException {
        assert img != null;
        int imgWidth = img.getWidth();
        int imgHeight = img.getHeight();

        // in PXC version 3, only grayscale images were written
        // as PNG, and for simplicity, we still write this field
//        int imgType = img.getType();
        int imgType = TYPE_BYTE_GRAY;

        out.writeInt(imgWidth);
        out.writeInt(imgHeight);
        out.writeInt(imgType);

        ProgressTracker pt = getImageTracker();

        if (imgType == TYPE_BYTE_GRAY) {
            TrackedIO.writeToStream(img, out, "PNG", pt);
//            ImageIO.write(img, "PNG", out);
        } else {
            // this legacy branch is never executed anymore
            int[] pixels = getPixels(img);
            int length = pixels.length;
            int progress = 0;
            int fiveUnits = length / 20;
            for (int pixel : pixels) {
                out.writeInt(pixel);
                progress++;
                if (progress > fiveUnits) {
                    pt.unitsDone(5);
                    progress = 0;
                }
            }
        }
    }

    // when deserializing, the progress tracking
    // is done at the InputStream level, not here
    public static BufferedImage deserializeImage(ObjectInputStream in) throws IOException {
        int width = in.readInt();
        int height = in.readInt();
        int type = in.readInt();

        if (type == TYPE_BYTE_GRAY) {
            BufferedImage img = ImageIO.read(in);
            int imgType = img.getType();
            if (imgType != TYPE_BYTE_GRAY && imgType != BufferedImage.TYPE_INT_ARGB) {
                img = ImageUtils.toSysCompatibleImage(img);
            }
            return img;
        } else {
            // this branch is executed only for legacy (version 3) pxc files
            BufferedImage img = new BufferedImage(width, height, type);
            int[] pixels = getPixels(img);

            int length = pixels.length;
            for (int i = 0; i < length; i++) {
                pixels[i] = in.readInt();
            }
            return img;
        }
    }

    private static ProgressTracker getImageTracker() {
        if (workRatioForOneImage == -1) {
            // a pxc without images
            return ProgressTracker.NULL_TRACKER;
        } else {
            return new SubtaskProgressTracker(workRatioForOneImage, mainPT);
        }
    }

    // Reads 4 bytes as an int
    private static int readInt(InputStream is) throws IOException {
        return is.read() << 24 | (is.read() & 0xFF) << 16 |
            (is.read() & 0xFF) << 8 | (is.read() & 0xFF);
    }

    // Writes an int as 4 bytes
    private static void writeInt(OutputStream os, int value) throws IOException {
        os.write((value >>> 24) & 0xFF);
        os.write((value >>> 16) & 0xFF);
        os.write((value >>> 8) & 0xFF);
        os.write(value & 0xFF);
    }
}

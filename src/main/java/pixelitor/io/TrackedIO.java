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

package pixelitor.io;

import pd.GifDecoder;
import pixelitor.gui.utils.ThumbInfo;
import pixelitor.utils.*;

import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;
import java.util.function.Consumer;

import static pixelitor.utils.ImageUtils.createThumbnail;
import static pixelitor.utils.Threads.calledOutsideEDT;

/**
 * Utility methods like in ImageIO, but with progress tracking
 */
public class TrackedIO {

    private TrackedIO() {
        // do not instantiate
    }

    public static void write(BufferedImage img,
                             String formatName,
                             File file,
                             Consumer<ImageWriteParam> customizer) throws IOException {
        System.out.printf("TrackedIO::write: writing %dx%d %s image to %s%n",
            img.getWidth(), img.getHeight(), formatName, file.getAbsolutePath());

        var tracker = new StatusBarProgressTracker(
            "Writing " + file.getName(), 100);
        // the creation of FileOutputStream is necessary, because if the
        // ImageOutputStream is created directly from the File, then existing files
        // are not truncated, and small files don't completely overwrite bigger files.
        try (FileOutputStream fos = new FileOutputStream(file)) {
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(fos)) {
                if (ios != null) {
                    writeToIOS(img, ios, formatName, tracker, customizer);
                } else {
                    throwNoIOSErrorFor(file);
                }
            }
        }
    }

    private static void throwNoIOSErrorFor(File file) throws IOException {
        // createImageOutputStream swallows the original IO exception
        // for IO errors like "Access is denied" and returns null,
        // therefore throw a generic exception
        throw new IOException("Could not save to " + file.getPath());
    }

    public static void writeToStream(BufferedImage img,
                                     OutputStream os,
                                     String formatName,
                                     ProgressTracker tracker) throws IOException {
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {
            if (ios != null) {
                writeToIOS(img, ios, formatName, tracker, null);
            } else {
                throw new IOException("could not open ImageOutputStream");
            }
        }
    }

    public static void writeToIOS(BufferedImage img,
                                  ImageOutputStream ios,
                                  String formatName,
                                  ProgressTracker tracker,
                                  Consumer<ImageWriteParam> customizer) throws IOException {
        assert calledOutsideEDT() : "on EDT";
        assert ios != null;

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);
        if (!writers.hasNext()) {
            throw new IOException("No writer found for " + formatName);
        }
        ImageWriter writer = writers.next();

        if (customizer != null) {
            customizer.accept(writer.getDefaultWriteParam());
        }

        try {
            writer.setOutput(ios);
            writer.addIIOWriteProgressListener(new TrackerWriteProgressListener(tracker));
            writer.write(img);
        } finally {
            writer.dispose();
            ios.flush();
        }
    }

    public static void writeDetailedToStream(BufferedImage image, ImageOutputStream ios,
                                             ProgressTracker tracker, String formatName,
                                             boolean progressive, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);
        if (!writers.hasNext()) {
            throw new IllegalStateException("No " + formatName + " writers found");
        }
        ImageWriter writer = writers.next();

        ImageWriteParam imageWriteParam = writer.getDefaultWriteParam();

        IIOImage iioImage = new IIOImage(image, null, null);

        writer.setOutput(ios);
        if (tracker != null) {
            writer.addIIOWriteProgressListener(new TrackerWriteProgressListener(tracker));
        }
        writer.write(null, iioImage, imageWriteParam);

        ios.flush();
        ios.close();
    }

    /**
     * Reads an image from a file, and throws only runtime exceptions
     */
    public static BufferedImage uncheckedRead(File file) {
        try {
            BufferedImage image = read(file);
            // For some decoding problems (ImageIO bugs?) we get an
            // exception here, for others we get a null image.
            // In both cases, throw a runtime exception
            if (image == null) {
                throw DecodingException.normal(file, null);
            }
            return image;
        } catch (IOException e) {
            throw DecodingException.normal(file, e);
        } catch (ArrayIndexOutOfBoundsException e) {
            boolean isGif = FileUtils.hasGIFExtension(file.getName());
            if (isGif) {
                // perhaps this is issue #40, try another decoder
                // also see https://stackoverflow.com/questions/22259714/arrayindexoutofboundsexception-4096-while-reading-gif-file
                return alternativeGifRead(file);
            } else {
                throw DecodingException.normal(file, e);
            }
        }
    }

    // Currently using https://github.com/DhyanB/Open-Imaging
    // Apache Imaging also has a fix, but they have not released yet a version with it
    // see https://issues.apache.org/jira/browse/IMAGING-130
    private static BufferedImage alternativeGifRead(File file) {
        BufferedImage img;
        try {
            var dataStream = new FileInputStream(file);
            GifDecoder.GifImage gif = GifDecoder.read(dataStream);
            img = gif.getFrame(0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return img;
    }

    public static BufferedImage read(File file) throws IOException {
        var tracker = new StatusBarProgressTracker(
            "Reading " + file.getName(), 100);

        BufferedImage image;
        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            image = readFromIIS(iis, tracker);
        } catch (Exception e) {
            // an IIOException exception is thrown for example by
            // Java's JPEG reader when reading a CMYK JPEG.
            throw DecodingException.normal(file, e);
        }

        return image;
    }

    public static BufferedImage readFromStream(InputStream is,
                                               ProgressTracker tracker) throws IOException {
        BufferedImage image;
        try (ImageInputStream iis = ImageIO.createImageInputStream(is)) {
            image = readFromIIS(iis, tracker);
        }
        return image;
    }

    public static BufferedImage readFromIIS(ImageInputStream iis,
                                            ProgressTracker tracker) throws IOException {
        assert calledOutsideEDT() : "on EDT";

        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

        if (!readers.hasNext()) {
            return null;
        }

        ImageReader reader = readers.next();

        BufferedImage image;
        try {
            reader.setInput(iis);

            reader.addIIOReadProgressListener(new TrackerReadProgressListener(tracker));

            ImageReadParam param = reader.getDefaultReadParam();
            image = reader.read(0, param);
        } finally {
            reader.dispose();
        }

        return image;
    }

    /**
     * Reads a subsampled image. It requires far less memory,
     * can be almost twice as fast as reading all pixels,
     * and it does not need to be resized later.
     * <p>
     * Idea from https://stackoverflow.com/questions/3294388/make-a-bufferedimage-use-less-ram
     */
    public static ThumbInfo readSubsampledThumb(File file,
                                                int thumbMaxWidth,
                                                int thumbMaxHeight,
                                                ProgressTracker tracker) throws IOException {
        ThumbInfo thumbInfo;
        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

            if (!readers.hasNext()) {
                return null;
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(iis, true);

                if (tracker != null) {
                    // register the progress tracking
                    reader.addIIOReadProgressListener(
                        new TrackerReadProgressListener(tracker));
                }

                // Once the reader has its input source set,
                // we can use it to obtain information about
                // the image without necessarily causing image data
                // to be read into memory
                int imgWidth = reader.getWidth(0);
                int imgHeight = reader.getHeight(0);

                if (reader.readerSupportsThumbnails()) {
                    // a thumbnail could be in the file already
                    boolean hasThumbs = reader.hasThumbnails(0);
                    if (hasThumbs) {
                        // use the embedded thumbnail instead of subsampling
                        BufferedImage thumb = reader.readThumbnail(0, 0);
                        return ThumbInfo.success(thumb, imgWidth, imgHeight);
                    }
                }

                if (imgWidth < 2 * thumbMaxWidth || imgHeight < 2 * thumbMaxHeight) {
                    // subsampling only makes sense when
                    // the image is shrunk by 2x or greater
                    BufferedImage image = reader.read(0);
                    BufferedImage thumb = createThumbnail(image,
                        Math.min(thumbMaxWidth, thumbMaxHeight), null);
                    return ThumbInfo.success(thumb, imgWidth, imgHeight);
                }

                ImageReadParam imageReaderParams = reader.getDefaultReadParam();
                int subsampling = calcSubsamplingCols(imgWidth, imgHeight,
                    thumbMaxWidth, thumbMaxHeight);

                imageReaderParams.setSourceSubsampling(subsampling, subsampling, 0, 0);
                BufferedImage image;
                try {
                    image = reader.read(0, imageReaderParams);
                    thumbInfo = ThumbInfo.success(image, imgWidth, imgHeight);
                } catch (Exception e) {
                    // at least the image width/height could be read
                    thumbInfo = ThumbInfo.failure(imgWidth, imgHeight, ThumbInfo.PREVIEW_ERROR);
                }
            } finally {
                reader.dispose();
            }
        }
        return thumbInfo;
    }

    /**
     * Calculates the number of columns to advance between pixels while subsampling.
     * In order to preserve the aspect ratio, the same number is used
     * for the horizontal and vertical subsampling.
     */
    @VisibleForTesting
    public static int calcSubsamplingCols(int imgWidth, int imgHeight,
                                          int thumbMaxWidth, int thumbMaxHeight) {
        assert imgWidth >= thumbMaxWidth * 2;
        assert imgHeight >= thumbMaxHeight * 2;

        int colsX = (int) Math.ceil(imgWidth / (double) thumbMaxWidth);
        int colsY = (int) Math.ceil(imgHeight / (double) thumbMaxHeight);

        return Math.max(colsX, colsY);
    }
}

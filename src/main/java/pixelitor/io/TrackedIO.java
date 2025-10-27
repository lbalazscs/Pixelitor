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

import pd.GifDecoder;
import pixelitor.Composition;
import pixelitor.gui.utils.ThumbInfo;
import pixelitor.utils.ProgressTracker;
import pixelitor.utils.StatusBarProgressTracker;
import pixelitor.utils.TrackingReadProgressListener;
import pixelitor.utils.TrackingWriteProgressListener;

import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static pixelitor.utils.Threads.callInfo;
import static pixelitor.utils.Threads.calledOutsideEDT;
import static pixelitor.utils.Threads.onEDT;
import static pixelitor.utils.Threads.onIOThread;
import static pixelitor.utils.Thumbnails.createThumbnail;

/**
 * Utility class for image input/output with progress tracking.
 */
public class TrackedIO {

    private TrackedIO() {
        // prevent instantiation
    }

    /**
     * Writes an image to a file with progress tracking.
     */
    public static void write(BufferedImage img,
                             String formatName,
                             File outputFile,
                             Consumer<ImageWriteParam> customizer) throws IOException {
        var tracker = new StatusBarProgressTracker(
            "Writing " + outputFile.getName(), 100);
        // the creation of FileOutputStream is necessary, because if the
        // ImageOutputStream is created directly from the File, then existing files
        // are not truncated, and small files don't completely overwrite bigger files.
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             ImageOutputStream ios = ImageIO.createImageOutputStream(fos)) {

            if (ios != null) {
                writeToIOS(img, ios, formatName, tracker, customizer);
            } else {
                // createImageOutputStream swallows the original IO exception
                // for IO errors like "Access is denied" and returns null,
                // therefore throw a generic exception
                throw new IOException("Could not save to " + outputFile.getPath());
            }
        }
    }

    /**
     * Writes an image to an output stream with progress tracking.
     */
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

    /**
     * Writes an image to an image output stream with progress tracking.
     */
    public static void writeToIOS(BufferedImage img,
                                  ImageOutputStream ios,
                                  String formatName,
                                  ProgressTracker tracker,
                                  Consumer<ImageWriteParam> customizer) throws IOException {
        assert calledOutsideEDT() : callInfo();
        assert ios != null;

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);
        if (!writers.hasNext()) {
            throw new IOException("No writer found for " + formatName);
        }
        ImageWriter writer = writers.next();

        ImageWriteParam param = null;
        if (customizer != null) {
            param = writer.getDefaultWriteParam();
            customizer.accept(param);
        }

        try {
            writer.setOutput(ios);
            writer.addIIOWriteProgressListener(new TrackingWriteProgressListener(tracker));
            IIOImage iioImage = new IIOImage(img, null, null);
            writer.write(null, iioImage, param);
        } finally {
            writer.dispose();
            ios.flush();
        }
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
                throw DecodingException.forImageIORead(file, null);
            }
            return image;
        } catch (ArrayIndexOutOfBoundsException e) {
            boolean isGif = FileUtils.hasGIFExtension(file.getName());
            if (isGif) {
                // perhaps this is issue #40, try another decoder
                // also see https://stackoverflow.com/questions/22259714/arrayindexoutofboundsexception-4096-while-reading-gif-file
                return alternativeGifRead(file);
            } else {
                throw DecodingException.forImageIORead(file, e);
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

    /**
     * Reads an image from a file with progress tracking.
     */
    public static BufferedImage read(File file) {
        var tracker = new StatusBarProgressTracker(
            "Reading " + file.getName(), 100);

        BufferedImage image;
        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            image = readFromIIS(iis, tracker);
        } catch (Exception e) {
            // an IIOException exception is thrown for example by
            // Java's JPEG reader when reading a CMYK JPEG.
            throw DecodingException.forImageIORead(file, e);
        }

        return image;
    }

    /**
     * Reads an image from an input stream with progress tracking.
     */
    public static BufferedImage readFromStream(InputStream is,
                                               ProgressTracker tracker) throws IOException {
        BufferedImage image;
        try (ImageInputStream iis = ImageIO.createImageInputStream(is)) {
            image = readFromIIS(iis, tracker);
        }
        return image;
    }

    /**
     * Reads an image from an image input stream with progress tracking.
     */
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
            reader.addIIOReadProgressListener(new TrackingReadProgressListener(tracker));
            ImageReadParam param = reader.getDefaultReadParam();
            image = reader.read(0, param);
        } finally {
            reader.dispose();
        }

        return image;
    }

    /**
     * Reads a thumbnail from an image file. If an embedded thumbnail
     * is not found inside the file, then it reads a subsampled image.
     */
    public static ThumbInfo readThumbnail(File file,
                                          int maxThumbWidth,
                                          int maxThumbHeight,
                                          ProgressTracker tracker) throws IOException {

        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

            if (!readers.hasNext()) {
                return ThumbInfo.failure(ThumbInfo.PREVIEW_ERROR);
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true);

                if (tracker != null) {
                    reader.addIIOReadProgressListener(new TrackingReadProgressListener(tracker));
                }

                return readThumbnail(reader, maxThumbWidth, maxThumbHeight);
            } finally {
                reader.dispose();
            }
        }
    }

    // reads a thumbnail from an image reader
    private static ThumbInfo readThumbnail(ImageReader reader, int maxThumbWidth, int maxThumbHeight) throws IOException {
        int imgWidth = reader.getWidth(0);
        int imgHeight = reader.getHeight(0);

        // check if the image contains an embedded thumbnail
        if (reader.readerSupportsThumbnails() && reader.hasThumbnails(0)) {
            BufferedImage thumb = reader.readThumbnail(0, 0);
            return ThumbInfo.success(thumb, imgWidth, imgHeight);
        }

        // check if subsampling is worthwhile
        if (imgWidth < 2 * maxThumbWidth || imgHeight < 2 * maxThumbHeight) {
            // read the image fully: subsampling only makes sense
            // when the image is shrunk by 2x or greater
            BufferedImage image = reader.read(0);
            BufferedImage thumb = createThumbnail(image,
                maxThumbWidth, maxThumbHeight, null);
            return ThumbInfo.success(thumb, imgWidth, imgHeight);
        }

        // subsampled read
        ImageReadParam imageReaderParam = reader.getDefaultReadParam();
        int subsampling = calcSubsampling(imgWidth, imgHeight,
            maxThumbWidth, maxThumbHeight);
        imageReaderParam.setSourceSubsampling(subsampling, subsampling, 0, 0);
        try {
            BufferedImage image = reader.read(0, imageReaderParam);
            return ThumbInfo.success(image, imgWidth, imgHeight);
        } catch (Exception e) {
            // at least the image width/height could be read
            return ThumbInfo.failure(imgWidth, imgHeight, ThumbInfo.PREVIEW_ERROR);
        }
    }

    /**
     * Calculates the number of columns/rows to advance between pixels
     * while subsampling. In order to preserve the aspect ratio, the same
     * number is used for the horizontal and vertical subsampling.
     */
    public static int calcSubsampling(int imgWidth, int imgHeight,
                                      int thumbMaxWidth, int thumbMaxHeight) {
        assert imgWidth >= thumbMaxWidth * 2;
        assert imgHeight >= thumbMaxHeight * 2;

        int cols = (int) Math.ceil(imgWidth / (double) thumbMaxWidth);
        int rows = (int) Math.ceil(imgHeight / (double) thumbMaxHeight);

        return Math.max(cols, rows);
    }

    public static Composition readSingleLayeredSync(File file) {
        BufferedImage img = uncheckedRead(file);
        return Composition.fromImage(img, file, null);
    }

    static CompletableFuture<Composition> readSingleLayeredAsync(File file) {
        return CompletableFuture.supplyAsync(() -> uncheckedRead(file), onIOThread)
            .thenApplyAsync(img -> Composition.fromImage(img, file, null), onEDT);
    }
}

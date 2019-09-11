/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.ProgressTracker;
import pixelitor.utils.StatusBarProgressTracker;
import pixelitor.utils.TrackerReadProgressListener;
import pixelitor.utils.TrackerWriteProgressListener;
import pixelitor.utils.VisibleForTesting;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Iterator;

import static pixelitor.utils.ImageUtils.createThumbnail;

/**
 * Utility methods like in ImageIO, but with progress tracking
 */
public class TrackedIO {
    private TrackedIO() {
        // do not instantiate
    }

    public static void write(BufferedImage img,
                             String formatName,
                             File file) throws IOException {
        ProgressTracker pt = new StatusBarProgressTracker("Writing " + file.getName(), 100);
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(file)) {
            if (ios != null) {
                writeToIOS(img, ios, formatName, pt);
            } else {
                throwNoIOSErrorFor(file);
            }
        }
    }

    public static void throwNoIOSErrorFor(File file) throws IOException {
        // sadly createImageOutputStream swallows the original IO exception
        // for IO errors like "Access is denied" and returns null,
        // so we can only throw a generic exception
        throw new IOException("Could not save to " + file.getPath());
    }

    public static void writeToStream(BufferedImage img,
                                     OutputStream os,
                                     String formatName,
                                     ProgressTracker pt) throws IOException {
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {
            if (ios != null) {
                writeToIOS(img, ios, formatName, pt);
            } else {
                throw new IOException("could not open ImageOutputStream");
            }
        }
    }

    private static void writeToIOS(BufferedImage img,
                                   ImageOutputStream ios,
                                   String formatName,
                                   ProgressTracker pt) throws IOException {
        assert !EventQueue.isDispatchThread();
        assert ios != null;

        ImageTypeSpecifier type =
            ImageTypeSpecifier.createFromRenderedImage(img);
        Iterator<ImageWriter> writers = ImageIO.getImageWriters(type, formatName);

        if (!writers.hasNext()) {
            throw new IOException("No writer found for " + formatName);
        }
        ImageWriter writer = writers.next();
        try {
            writer.setOutput(ios);
            writer.addIIOWriteProgressListener(new TrackerWriteProgressListener(pt));
            writer.write(img);
        } finally {
            writer.dispose();
        }
    }

    public static BufferedImage uncheckedRead(File file) {
        try {
            return read(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ArrayIndexOutOfBoundsException e) {
            boolean isGif = FileUtils.findExtension(file.getName())
                .map(String::toLowerCase)
                .filter(s -> s.equals("gif"))
                .isPresent();
            if (isGif) {
                // perhaps this is issue #40, try another decoder
                // also see https://stackoverflow.com/questions/22259714/arrayindexoutofboundsexception-4096-while-reading-gif-file
                return alternativeGifRead(file);
            } else {
                throw e;
            }
        }
    }

    // Currently using https://github.com/DhyanB/Open-Imaging
    // Apache Imaging also has a fix, but they have not released yet a version with it
    // see https://issues.apache.org/jira/browse/IMAGING-130
    private static BufferedImage alternativeGifRead(File file) {
        BufferedImage img = null;
        try {
            FileInputStream data = new FileInputStream(file);
            GifDecoder.GifImage gif = GifDecoder.read(data);
            img = gif.getFrame(0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return img;
    }

    public static BufferedImage read(File file) throws IOException {
        ProgressTracker pt = new StatusBarProgressTracker(
            "Reading " + file.getName(), 100);

        BufferedImage image;
        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            image = readFromIIS(iis, pt);
        }
        return image;
    }

    public static BufferedImage readFromStream(InputStream is,
                                               ProgressTracker pt) throws IOException {
        BufferedImage image;
        try (ImageInputStream iis = ImageIO.createImageInputStream(is)) {
            image = readFromIIS(iis, pt);
        }
        return image;
    }

    public static BufferedImage readFromIIS(ImageInputStream iis,
                                            ProgressTracker pt) throws IOException {
        assert !EventQueue.isDispatchThread();

        BufferedImage image;
        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

        if (!readers.hasNext()) {
            return null;
        }

        ImageReader reader = readers.next();

        try {
            reader.setInput(iis);

            reader.addIIOReadProgressListener(new TrackerReadProgressListener(pt));

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
                                                ProgressTracker pt) throws IOException {
        ThumbInfo thumbInfo;
        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

            if (!readers.hasNext()) {
                return null;
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(iis, true);

                if (pt != null) {
                    // register the progress tracking
                    reader.addIIOReadProgressListener(new TrackerReadProgressListener(pt));
                }

                // Once the reader has its input source set,
                // we can use it to obtain information about
                // the image without necessarily causing image data
                // to be read into memory
                int imgWidth = reader.getWidth(0);
                int imgHeight = reader.getHeight(0);

                if (imgWidth < 2 * thumbMaxWidth || imgHeight < 2 * thumbMaxHeight) {
                    // subsampling only makes sense when
                    // the image is shrunk by 2x or greater
                    BufferedImage image = reader.read(0);
                    BufferedImage thumb = createThumbnail(image,
                        Math.min(thumbMaxWidth, thumbMaxHeight), null);
                    return new ThumbInfo(thumb, imgWidth, imgHeight);
                }

// TODO in principle a thumbnail could be in the file already
// see https://docs.oracle.com/javase/7/docs/technotes/guides/imageio/spec/apps.fm3.html
//                boolean hasThumbs = reader.hasThumbnails(0);
//                if(hasThumbs) {
//                    BufferedImage bi = reader.readThumbnail(0, 0);
//                }

                ImageReadParam imageReaderParams = reader.getDefaultReadParam();
                int subsampling = calcSubsamplingCols(imgWidth, imgHeight,
                    thumbMaxWidth, thumbMaxHeight);

                imageReaderParams.setSourceSubsampling(subsampling, subsampling, 0, 0);
                BufferedImage image = reader.read(0, imageReaderParams);
                thumbInfo = new ThumbInfo(image, imgWidth, imgHeight);
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

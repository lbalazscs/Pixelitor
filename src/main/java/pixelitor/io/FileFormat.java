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

import pixelitor.Composition;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Utils;

import javax.swing.filechooser.FileFilter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static pixelitor.utils.Threads.onEDT;
import static pixelitor.utils.Threads.onIOThread;

/**
 * The input and output file formats
 */
public enum FileFormat {
    BMP(false, ImageUtils::convertToRGB, FileChoosers.bmpFilter) {
    }, GIF(false, ImageUtils::convertToIndexed, FileChoosers.gifFilter) {
    }, JPG(false, ImageUtils::convertToRGB, FileChoosers.jpegFilter) {
    }, ORA(true, null, FileChoosers.oraFilter) {
        @Override
        public Runnable getSaveTask(Composition comp, SaveSettings settings) {
            return () -> OpenRaster.uncheckedWrite(comp, settings.getFile());
        }

        @Override
        public CompletableFuture<Composition> readFrom(File file) {
            return CompletableFuture.supplyAsync(
                Utils.toSupplier(() -> OpenRaster.read(file)), onIOThread);
        }
    }, PAM(false, ImageUtils::convertToInterleavedRGBA, FileChoosers.pamFilter) {
    }, PNG(false, null, FileChoosers.pngFilter) {
    }, PPM(false, ImageUtils::convertToInterleavedRGB, FileChoosers.ppmFilter) {
    }, PXC(true, null, FileChoosers.pxcFilter) {
        @Override
        public Runnable getSaveTask(Composition comp, SaveSettings settings) {
            return () -> PXCFormat.write(comp, settings.getFile());
        }

        @Override
        public CompletableFuture<Composition> readFrom(File file) {
            return CompletableFuture.supplyAsync(
                Utils.toSupplier(() -> PXCFormat.read(file)), onIOThread);
        }
    }, TGA(false, null, FileChoosers.tgaFilter) {
    }, TIFF(false, null, FileChoosers.tiffFilter) {
    };

    private final boolean hasLayers;
    private final Function<BufferedImage, BufferedImage> converter;
    private final FileFilter fileFilter;

    FileFormat(boolean hasLayers, Function<BufferedImage, BufferedImage> converter, FileFilter fileFilter) {
        this.hasLayers = hasLayers;
        this.converter = converter;
        this.fileFilter = fileFilter;
    }

    public Runnable getSaveTask(Composition comp, SaveSettings settings) {
        assert !hasLayers; // overwritten for multi-layered formats

        return () -> saveSingleLayered(comp, settings);
    }

    public CompletableFuture<Composition> readFrom(File file) {
        // overwritten for multi-layered formats
        return readSimpleFrom(file);
    }

    /**
     * Loads a composition from a file with a single-layer image format
     */
    private static CompletableFuture<Composition> readSimpleFrom(File file) {
        return CompletableFuture.supplyAsync(() -> TrackedIO.uncheckedRead(file), onIOThread)
            .thenApplyAsync(img -> Composition.fromImage(img, file, null), onEDT);
    }

    private void saveSingleLayered(Composition comp, SaveSettings settings) {
        BufferedImage img = comp.getCompositeImage();
        if (converter != null) {
            // do the final conversion, which might be
            // necessary before writing the image
            img = converter.apply(img);
        }

        IO.saveImageToFile(img, settings);
    }

    public FileFilter getFileFilter() {
        return fileFilter;
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

    public static Optional<FileFormat> fromFile(File file) {
        String fileName = file.getName();
        String extension = FileUtils.findExtension(fileName).orElse("");
        return fromExtension(extension);
    }

    public static Optional<FileFormat> fromExtension(String extension) {
        String extLC = extension.toLowerCase();
        return switch (extLC) {
            case "bmp" -> Optional.of(BMP);
            case "gif" -> Optional.of(GIF);
            case "jpg", "jpeg" -> Optional.of(JPG);
            case "ora" -> Optional.of(ORA);
            case "pam" -> Optional.of(PAM);
            case "png" -> Optional.of(PNG);
            case "ppm" -> Optional.of(PPM);
            case "pxc" -> Optional.of(PXC);
            case "tga" -> Optional.of(TGA);
            case "tif", "tiff" -> Optional.of(TIFF);
            default -> Optional.empty();
        };
    }

    private static volatile FileFormat lastOutput = JPG;

    public static FileFormat getLastOutput() {
        return lastOutput;
    }

    public static void setLastOutput(FileFormat format) {
        lastOutput = format;
    }
}

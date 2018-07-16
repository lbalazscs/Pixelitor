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

import pixelitor.Composition;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * The output file format
 */
public enum OutputFormat {
    JPG(false, false) {
    }, PNG(false, true) {
    }, TIFF(false, true) {
    }, GIF(false, false) { // the format supports alpha, but the default encoder has bugs
    }, BMP(false, false) {
    }, PXC(true, true) {
        @Override
        public Runnable getSaveTask(Composition comp, File file) {
            return () -> PXCFormat.write(comp, file);
        }
    }, ORA(true, true) {
        @Override
        public Runnable getSaveTask(Composition comp, File file) {
            return () -> OpenRaster.uncheckedWrite(comp, file, false);
        }
    };

    private final boolean layered;
    private final boolean hasAlpha;

    OutputFormat(boolean layered, boolean hasAlpha) {
        this.layered = layered;
        this.hasAlpha = hasAlpha;
    }

    public Runnable getSaveTask(Composition comp, File file) {
        assert !layered; // overwritten for layered formats

        return () -> {
            BufferedImage img = comp.getCompositeImage();
            if (!hasAlpha) {
                // no alpha support, convert first to RGB
                img = ImageUtils.convertToRGB(img, false);
            }
            OpenSaveManager.saveImageToFile(file, img, this);
        };
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

    public static OutputFormat fromFile(File file) {
        String fileName = file.getName();
        String extension = FileExtensionUtils.getExt(fileName);
        return fromExtension(extension);
    }

    public static OutputFormat fromExtension(String extension) {
        String extLC = extension.toLowerCase();
        switch (extLC) {
            case "jpg":
            case "jpeg":
                return JPG;
            case "png":
                return PNG;
            case "bmp":
                return BMP;
            case "gif":
                return GIF;
            case "pxc":
                return PXC;
            case "ora":
                return ORA;
            case "tif":
            case "tiff":
                return TIFF;
            default:
                throw new IllegalArgumentException("extension = " + extension);
        }
    }

    private static OutputFormat lastOutputFormat = JPG;

    public static OutputFormat getLastUsed() {
        return lastOutputFormat;
    }

    public static void setLastUsed(OutputFormat lastOutputFormat) {
        OutputFormat.lastOutputFormat = lastOutputFormat;
    }
}

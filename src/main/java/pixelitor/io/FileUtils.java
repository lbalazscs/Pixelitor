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

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Utility class with static methods related to files
 */
public class FileUtils {
    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "pxc", "ora", "tif", "tiff"));

    private FileUtils() {
    }

    /**
     * Returns the extension of the given file name
     * or empty Optional if no extension found
     */
    public static Optional<String> getExt(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1) {
            return Optional.empty();
        }
        return Optional.of(fileName
                .substring(lastIndex + 1));
    }

    public static String stripExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1) {
            return fileName;
        }
        return fileName.substring(0, lastIndex);
    }

    public static boolean hasSupportedInputExt(File file) {
        return hasSupportedInputExt(file.getName());
    }

    public static boolean hasSupportedInputExt(String fileName) {
        // currently the same extensions are supported for input and output
        return hasSupportedExt(fileName);
    }

    public static boolean hasSupportedOutputExt(String fileName) {
        // currently the same extensions are supported for input and output
        return hasSupportedExt(fileName);
    }

    private static boolean hasSupportedExt(String fileName) {
        return getExt(fileName)
                .map(String::toLowerCase)
                .filter(SUPPORTED_EXTENSIONS::contains)
                .isPresent();
    }

    public static File[] listSupportedInputFilesIn(File dir) {
        java.io.FileFilter imageFilter = FileUtils::hasSupportedInputExt;
        return dir.listFiles(imageFilter);
    }

    public static String replaceExt(String fileName, String newExt) {
        if (!getExt(fileName).isPresent()) {
            return fileName + '.' + newExt;
        }
        String woExt = stripExtension(fileName);
        return woExt + '.' + newExt;
    }
}
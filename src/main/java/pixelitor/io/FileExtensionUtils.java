/*
 * Copyright 2018 Laszlo Balazs-Csiki
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

/**
 * Utility class with static methods related to file extensions
 */
public class FileExtensionUtils {
    private static final String[] SUPPORTED_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "bmp", "pxc", "ora"};

    private FileExtensionUtils() {
    }

    /**
     * Returns the extension of the given file name in lower case
     * or null if no extension found
     */
    public static String getExt(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1) {
            return null;
        }
        return fileName
                .substring(lastIndex + 1, fileName.length())
                .toLowerCase();
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
        String extension = getExt(fileName);
        if (extension == null) {
            return false;
        }
        extension = extension.toLowerCase();
        for (String supported : SUPPORTED_EXTENSIONS) {
            if (extension.equals(supported)) {
                return true;
            }
        }
        return false;
    }

    public static File[] getAllSupportedInputFilesInDir(File dir) {
        java.io.FileFilter imageFilter = FileExtensionUtils::hasSupportedInputExt;
        return dir.listFiles(imageFilter);
    }

    public static String replaceExt(String fileName, String newExt) {
        String inputExt = getExt(fileName);
        if (inputExt == null) {
            return fileName + '.' + newExt;
        }
        String woExt = stripExtension(fileName);
        return woExt + '.' + newExt;
    }
}
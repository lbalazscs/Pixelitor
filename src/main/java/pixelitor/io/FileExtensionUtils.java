/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
 *
 */
public class FileExtensionUtils {
    public static final String[] SUPPORTED_INPUT_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "bmp", "pxc", "ora"};
    public static final String[] SUPPORTED_OUTPUT_EXTENSIONS = SUPPORTED_INPUT_EXTENSIONS;

    /**
     * Utility class with static methods
     */
    private FileExtensionUtils() {
    }


    public static String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1) {
            return null;
        }
        return fileName.substring(lastIndex + 1, fileName.length());
    }

    public static String getFileNameWOExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1) {
            return fileName;
        }
        return fileName.substring(0, lastIndex);
    }

    public static boolean isSupportedExtension(String fileName, String[] supportedExtensions) {
        String extension = getFileExtension(fileName);
        if (extension == null) {
            return false;
        }
        extension = extension.toLowerCase();
        for (String supportedExtension : supportedExtensions) {
            if (extension.equals(supportedExtension)) {
                return true;
            }
        }
        return false;
    }

    public static File[] getAllSupportedFilesInDir(File dir) {
        java.io.FileFilter imageFilter = f -> isSupportedExtension(f.getName(), SUPPORTED_INPUT_EXTENSIONS);
        File[] files = dir.listFiles(imageFilter);
        return files;
    }

    public static String replaceExtension(String inputFileName, String newExtension) {
        String inputExtension = getFileExtension(inputFileName);
        if (inputExtension == null) {
            return inputFileName + '.' + newExtension;
        }
        String woExtension = getFileNameWOExtension(inputFileName);
        return woExtension + '.' + newExtension;
    }
}
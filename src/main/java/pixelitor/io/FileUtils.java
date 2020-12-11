/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import java.io.FileFilter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Utility class with static methods related to files
 */
public class FileUtils {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "gif", "bmp", "pxc", "ora", "tif", "tiff", "tga");

    private FileUtils() {
    }

    public static Optional<String> findExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1) {
            return Optional.empty();
        }
        return Optional.of(fileName.substring(lastIndex + 1));
    }

    public static boolean hasExtension(String fileName) {
        return findExtension(fileName).isPresent();
    }

    public static boolean hasPNGExtension(String fileName) {
        return hasTheExtension(fileName, "png");
    }

    public static boolean hasGIFExtension(String fileName) {
        return hasTheExtension(fileName, "gif");
    }

    public static boolean hasTGAExtension(String fileName) {
        return hasTheExtension(fileName, "tga");
    }

    private static boolean hasTheExtension(String fileName, String ext) {
        return findExtension(fileName)
            .filter(s -> s.equalsIgnoreCase(ext))
            .isPresent();
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
        return findExtension(fileName)
                .map(String::toLowerCase)
                .filter(SUPPORTED_EXTENSIONS::contains)
                .isPresent();
    }

    public static String replaceExt(String fileName, String newExt) {
        if (findExtension(fileName).isEmpty()) {
            return fileName + '.' + newExt;
        }
        String woExt = stripExtension(fileName);
        return woExt + '.' + newExt;
    }

    public static List<File> listSupportedInputFilesIn(File dir) {
        FileFilter imageFilter = FileUtils::hasSupportedInputExt;
        File[] files = dir.listFiles(imageFilter);
        if(files == null) {
            return Collections.emptyList();
        }

        // filter out crazily named directories with image file extensions
        return Stream.of(files)
                .filter(File::isFile)
                .collect(toList());
    }
}
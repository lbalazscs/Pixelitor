/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileFilter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Utility class with static methods related to files
 */
public class FileUtils {
    // the extensions supported by the file open and save dialogs
    private static final Set<String> SUPPORTED_OPEN_EXTENSIONS =
        extensionSet(FileChoosers.OPEN_FILTERS);
    private static final Set<String> SUPPORTED_SAVE_EXTENSIONS =
        extensionSet(FileChoosers.SAVE_FILTERS);

    // export-only formats that aren't offered when using save
    private static final Set<String> SUPPORTED_EXPORT_EXTENSIONS =
        Set.of("svg");
    private static Pattern fileNamePattern = null;

    private FileUtils() {
    }

    public static String calcExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1) {
            return null;
        }
        return fileName.substring(lastIndex + 1);
    }

    public static Optional<String> findExtension(String fileName) {
        return Optional.ofNullable(calcExtension(fileName));
    }

    public static boolean hasExtension(String fileName) {
        return findExtension(fileName).isPresent();
    }

    private static boolean hasTheExtension(String fileName, String ext) {
        return findExtension(fileName)
            .filter(s -> s.equalsIgnoreCase(ext))
            .isPresent();
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

    public static boolean hasMultiLayerExtension(File file) {
        Optional<String> extOpt = findExtension(file.getName());
        if (extOpt.isEmpty()) {
            return false;
        }
        String ext = extOpt.get().toLowerCase(Locale.ROOT);
        return ext.equals("pxc") || ext.equals("ora");
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
        return findExtension(fileName)
            .map(s -> s.toLowerCase(Locale.ROOT))
            .filter(SUPPORTED_OPEN_EXTENSIONS::contains)
            .isPresent();
    }

    public static boolean hasSupportedOutputExt(String fileName) {
        return findExtension(fileName)
            .map(s -> s.toLowerCase(Locale.ROOT))
            .filter(FileUtils::isSupportedOutputExt)
            .isPresent();
    }

    public static boolean isSupportedOutputExt(String extension) {
        return SUPPORTED_SAVE_EXTENSIONS.contains(extension)
            || SUPPORTED_EXPORT_EXTENSIONS.contains(extension);
    }

    public static String replaceExt(String fileName, String newExt) {
        if (findExtension(fileName).isEmpty()) {
            return fileName + '.' + newExt;
        }
        return stripExtension(fileName) + '.' + newExt;
    }

    public static List<File> listSupportedInputFilesIn(File dir) {
        FileFilter imageFilter = FileUtils::hasSupportedInputExt;
        File[] files = dir.listFiles(imageFilter);
        if (files == null) {
            return Collections.emptyList();
        }

        // filter out crazily named directories with image file extensions
        return Stream.of(files)
            .filter(File::isFile)
            .collect(toList());
    }

    private static Set<String> extensionSet(FileNameExtensionFilter[] filters) {
        Set<String> set = new HashSet<>();
        for (FileNameExtensionFilter filter : filters) {
            Collections.addAll(set, filter.getExtensions());
        }
        return set;
    }

    /**
     * Replaces all the special characters in s string with an underscore
     */
    public static String toFileName(String s) {
        if (fileNamePattern == null) {
            //noinspection NonThreadSafeLazyInitialization
            fileNamePattern = Pattern.compile("[/\\\\?%*:|\"<>.,;=]");
        }
        return fileNamePattern.matcher(s.trim()).replaceAll("_");
    }
}
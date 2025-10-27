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

import com.bric.util.JVM;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility class with static methods related to files.
 */
public class FileUtils {
    // supported file extensions for the open file dialog
    private static final Set<String> SUPPORTED_OPEN_EXTENSIONS =
        extensionSet(FileChoosers.OPEN_FILTERS);

    // supported file extensions for the save file dialog
    private static final Set<String> SUPPORTED_SAVE_EXTENSIONS =
        extensionSet(FileChoosers.SAVE_FILTERS);

    // export-only formats that aren't offered when using save
    private static final Set<String> SUPPORTED_EXPORT_EXTENSIONS =
        Set.of("svg");

    private static Pattern specialCharsPattern = null;

    // private constructor to prevent instantiation
    private FileUtils() {
    }

    /**
     * Returns the file extension in lower case or null if none is found.
     */
    public static String getExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1) {
            return null;
        }
        return fileName.substring(lastIndex + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the file extension as an Optional.
     */
    public static Optional<String> findExtension(String fileName) {
        return Optional.ofNullable(getExtension(fileName));
    }

    /**
     * Checks if the given file name has an extension.
     */
    public static boolean hasExtension(String fileName) {
        return fileName.indexOf('.') != -1;
    }

    /**
     * Checks if the given file name has the given extension.
     */
    private static boolean hasSpecificExtension(String fileName, String ext) {
        String foundExt = getExtension(fileName);
        return foundExt != null && foundExt.equals(ext);
    }

    public static boolean hasPNGExtension(String fileName) {
        return hasSpecificExtension(fileName, "png");
    }

    public static boolean hasGIFExtension(String fileName) {
        return hasSpecificExtension(fileName, "gif");
    }

    public static boolean hasMultiLayerExtension(File file) {
        String ext = getExtension(file.getName());
        if (ext == null) {
            return false;
        }
        return ext.equals("pxc") || ext.equals("ora");
    }

    public static String removeExtension(String fileName) {
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
        String extension = getExtension(fileName);
        return extension != null && SUPPORTED_OPEN_EXTENSIONS.contains(extension);
    }

    public static boolean hasSupportedOutputExt(String fileName) {
        String extension = getExtension(fileName);
        return extension != null && isSupportedOutputExt(extension);
    }

    public static boolean isSupportedOutputExt(String extension) {
        return SUPPORTED_SAVE_EXTENSIONS.contains(extension)
            || SUPPORTED_EXPORT_EXTENSIONS.contains(extension);
    }

    public static String replaceExtension(String fileName, String newExtension) {
        return removeExtension(fileName) + '.' + newExtension;
    }

    /**
     * Returns a list of all files in the given directory
     * that have supported input extensions.
     */
    public static List<File> listSupportedInputFiles(File dir) {
        File[] files = dir.listFiles(file -> file.isFile() && hasSupportedInputExt(file));
        if (files == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(files);
    }

    private static Set<String> extensionSet(FileNameExtensionFilter[] filters) {
        Set<String> set = new HashSet<>();
        for (FileNameExtensionFilter filter : filters) {
            Collections.addAll(set, filter.getExtensions());
        }
        return set;
    }

    /**
     * Trims the given string and replaces all special characters with an underscore.
     */
    public static String sanitizeToFileName(String s) {
        if (specialCharsPattern == null) {
            //noinspection NonThreadSafeLazyInitialization
            specialCharsPattern = Pattern.compile("[/\\\\?%*:|\"<>.,;=]");
        }
        return specialCharsPattern.matcher(s.trim()).replaceAll("_");
    }

    /**
     * Locates an external executable by first checking a configured
     * directory, and if not found, searching the system's PATH.
     */
    public static File locateExecutable(String configuredDir, String executableName) {
        // first, check the directory configured in Preferences
        File executable = findExecutable(configuredDir, executableName);
        if (executable != null) {
            return executable;
        }

        // if not found, try to find it in the system's PATH
        String searchCommand = JVM.isWindows ? "where" : "which";
        ProcessBuilder pb = new ProcessBuilder(searchCommand, executableName);
        try {
            Process process = pb.start();
            // read the first line of the standard output to get the path
            String fullPath;
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8))) {
                fullPath = reader.readLine();
            }

            int exitValue = process.waitFor();
            if (exitValue != 0 || fullPath == null || fullPath.isBlank()) {
                return null; // not found in PATH
            }

            return new File(fullPath.trim());
        } catch (InterruptedException | IOException e) {
            // command failed (e.g., 'which' not found) or was interrupted
            return null;
        }
    }

    /**
     * Checks if an executable file with the given name exists and
     * can be executed in the given directory.
     */
    public static File findExecutable(String dirPath, String executableName) {
        if (dirPath.isEmpty()) {
            return null;
        }
        if (JVM.isWindows) {
            executableName += ".exe";
        }
        File executableFile = new File(dirPath, executableName);
        if (executableFile.exists() && executableFile.canExecute()) {
            return executableFile;
        }
        return null;
    }
}

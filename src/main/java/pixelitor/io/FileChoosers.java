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

import pixelitor.Composition;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Messages;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class FileChoosers {
    private static boolean useNativeDialogs = AppPreferences.loadNativeChoosers();

    static {
        setUseNativeDialogs(useNativeDialogs);
    }

    private static FilePicker picker;

    public static final FileNameExtensionFilter bmpFilter = new FileNameExtensionFilter(
        "BMP files", "bmp");
    public static final FileNameExtensionFilter gifFilter = new FileNameExtensionFilter(
        "GIF files", "gif");
    public static final FileNameExtensionFilter jpegFilter = new FileNameExtensionFilter(
        "JPEG files", "jpg", "jpeg");
    public static final FileNameExtensionFilter oraFilter = new FileNameExtensionFilter(
        "OpenRaster files", "ora");
    public static final FileNameExtensionFilter pamFilter = new FileNameExtensionFilter(
        "PAM files", "pam");
    public static final FileNameExtensionFilter pngFilter = new FileNameExtensionFilter(
        "PNG files", "png");
    private static final FileNameExtensionFilter netPBMFilters = new FileNameExtensionFilter(
        "NetPBM files", "pam", "pbm", "pgm", "ppm", "pfm");
    public static final FileNameExtensionFilter ppmFilter = new FileNameExtensionFilter(
        "PPM files", "ppm");
    public static final FileNameExtensionFilter pxcFilter = new FileNameExtensionFilter(
        "PXC files", "pxc");
    public static final FileNameExtensionFilter tgaFilter = new FileNameExtensionFilter(
        "TGA files", "tga");
    public static final FileNameExtensionFilter tiffFilter = new FileNameExtensionFilter(
        "TIFF files", "tiff", "tif");
    public static final FileNameExtensionFilter svgFilter = new FileNameExtensionFilter(
        "SVG files", "svg");
    public static final FileNameExtensionFilter webpFilter = new FileNameExtensionFilter(
        "WebP files", "webp");

    // the difference is that all NetPBM files can be opened,
    // but only PAM and PPM can be saved
    public static final FileNameExtensionFilter[] OPEN_FILTERS = {
        bmpFilter, gifFilter, jpegFilter, netPBMFilters, oraFilter,
        pngFilter, pxcFilter, tiffFilter, tgaFilter, webpFilter};
    public static final FileNameExtensionFilter[] SAVE_FILTERS = {
        bmpFilter, gifFilter, jpegFilter, oraFilter, pamFilter,
        pngFilter, ppmFilter, pxcFilter, tiffFilter, tgaFilter};

    private FileChoosers() {
    }

    public static File getAnyOpenFile() {
        return picker.getAnyOpenFile();
    }

    public static File getSupportedOpenFile() {
        return picker.getSupportedOpenFile();
    }

    public static void openAsync() {
        File selectedFile = picker.getSupportedOpenFile();
        if (selectedFile != null) {
            String fileName = selectedFile.getName();
            if (FileUtils.hasSupportedInputExt(fileName)) {
                IO.openFileAsync(selectedFile, true);
            } else { // unsupported extension
                handleUnsupportedExtensionWhileOpening(fileName);
            }
        }
    }

    private static void handleUnsupportedExtensionWhileOpening(String fileName) {
        String extension = FileUtils.findExtension(fileName).orElse("");
        String msg = "<html>Could not open <b>" + fileName + "</b>, because ";
        if (extension.isEmpty()) {
            msg += "it has no extension.";
        } else {
            msg += "files of type <b>" + extension + "</b> are not supported.";
        }
        Messages.showError("Error", msg);
    }

    public static boolean saveWithSingleAllowedExtension(Composition comp,
                                                         String suggestedFileName,
                                                         Object extraInfo,
                                                         FileNameExtensionFilter extensionFilter) {
        File selectedFile = selectSaveFileForSpecificFormat(
            suggestedFileName, extensionFilter);
        return saveSelectedFile(comp, selectedFile, extraInfo);
    }

    public static File selectSaveFileForSpecificFormat(String suggestedFileName, FileFilter fileFilter) {
        FileChooserInfo chooserInfo = FileChooserInfo.forSingleFormat(suggestedFileName, fileFilter);
        return picker.showSaveDialog(chooserInfo);
    }

    private static boolean saveSelectedFile(Composition comp, File file, Object extraInfo) {
        if (file == null) {
            return false;
        }
        String extension = picker.getSelectedSaveExtension(file);
        if (extension == null) {
            extension = FileFormat.getLastSaved().toString();
            file = new File(file.getAbsolutePath() + "." + extension);
        }
        if (!FileUtils.isSupportedOutputExt(extension)) {
            Messages.showError("Unsupported Extension",
                "<html> The extension <b>" + extension + "</b> isn't supported.");
            return false;
        }
        IO.saveToChosenFile(comp, file, extraInfo, extension);
        return true;
    }

    /**
     * Returns true if the file was saved, false if the user cancels the saving
     */
    public static boolean saveWithChooser(Composition comp) {
        File selectedFile = picker.showSaveDialog(FileChooserInfo.forSavingComp(comp));
        return saveSelectedFile(comp, selectedFile, null);
    }

    public static File showSaveDialog(FileChooserInfo chooserInfo) {
        return picker.showSaveDialog(chooserInfo);
    }

    public static boolean useNativeDialogs() {
        return useNativeDialogs;
    }

    public static void setUseNativeDialogs(boolean useNativeDialogs) {
        if (FileChoosers.useNativeDialogs != useNativeDialogs || picker == null) {
            FileChoosers.useNativeDialogs = useNativeDialogs;
            if (useNativeDialogs) {
                picker = new AWTFilePicker();
            } else {
                picker = new SwingFilePicker();
            }
        }
    }
}

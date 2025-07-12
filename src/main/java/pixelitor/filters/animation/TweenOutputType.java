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

package pixelitor.filters.animation;

import pixelitor.gui.utils.ValidationResult;
import pixelitor.io.FileChoosers;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.nio.file.Files;

/**
 * The output formats for the tween animation.
 */
public enum TweenOutputType {
    PNG_FILE_SEQUENCE("PNG File Sequence", true) {
        @Override
        AnimationWriter createWriter(File outputDir, int delayMillis) {
            return new PNGFileSequenceWriter(outputDir);
        }

        @Override
        public ValidationResult validate(File outputDir) {
            return checkDir(outputDir, this);
        }

        @Override
        public FileNameExtensionFilter getFileFilter() {
            return null; // directory selection doesn't use file filters
        }
    }, ANIM_GIF("Animated GIF File", false) {
        @Override
        AnimationWriter createWriter(File file, int delayMillis) {
            return new AnimGIFWriter(file, delayMillis);
        }

        @Override
        public ValidationResult validate(File output) {
            return checkFile(output, this, "GIF");
        }

        @Override
        public FileNameExtensionFilter getFileFilter() {
            return FileChoosers.gifFilter;
        }
    };

    private final String displayName;
    private final boolean requiresDirectory;

    TweenOutputType(String displayName, boolean requiresDirectory) {
        this.displayName = displayName;
        this.requiresDirectory = requiresDirectory;
    }

    public boolean needsDirectory() {
        return requiresDirectory;
    }

    /**
     * Creates an appropriate {@link AnimationWriter} for this output type.
     */
    abstract AnimationWriter createWriter(File file, int delayMillis);

    /**
     * Validates that the given output location (file or directory)
     * is appropriate for this output type.
     */
    public abstract ValidationResult validate(File outputLocation);

    public abstract FileNameExtensionFilter getFileFilter();

    private static ValidationResult checkFile(File outputFile,
                                              TweenOutputType type,
                                              String fileType) {
        if (outputFile.exists() && outputFile.isDirectory()) {
            return ValidationResult.invalid(String.format(
                "<b>%s</b> is a folder." +
                    "<br>A %s file is required for the <b>%s</b> output type.",
                outputFile.getAbsolutePath(), fileType, type));
        }

        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            return ValidationResult.invalid(String.format(
                "The parent folder does not exist:<br><b>%s</b>", parentDir.getAbsolutePath()));
        }

        return ValidationResult.valid();
    }

    private static ValidationResult checkDir(File outputDir, TweenOutputType type) {
        if (!outputDir.exists()) {
            return ValidationResult.invalid(String.format(
                "Folder <b>%s</b> does not exist.", outputDir.getAbsolutePath()));
        }
        if (!outputDir.isDirectory()) {
            return ValidationResult.invalid(String.format(
                "<b>%s</b> is not a folder.<br>Please select an existing folder for the <b>%s</b> output type.",
                outputDir.getAbsolutePath(), type));
        }
        if (!Files.isWritable(outputDir.toPath())) {
            return ValidationResult.invalid(String.format(
                "Folder <b>%s</b> is not writable.", outputDir.getAbsolutePath()));
        }
        return ValidationResult.valid();
    }

    @Override
    public String toString() {
        return displayName;
    }
}

/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
 * The output formats of the tween animation.
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
            return null; // Directory selection doesn't use file filters
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
        if (outputFile.exists()) {
            if (outputFile.isDirectory()) {
                String msg = String.format("%s is a folder." +
                        "<br>For the \"%s\" output type, " +
                        "select a (new or existing) %s file in an existing folder.",
                    outputFile.getAbsolutePath(), type, fileType);
                return ValidationResult.invalid(msg);
            }
        } else { // if it doesn't exist, we still expect the parent directory to exist
            File parentDir = outputFile.getParentFile();
            if (parentDir == null) {
                return ValidationResult.invalid(
                    String.format("Folder %s not found", parentDir.getAbsolutePath()));
            }
            if (!parentDir.exists()) {
                String msg = String.format("The folder %s of the %s file does not exist." +
                        "<br>For the \"%s\" output type, " +
                        "select a (new or existing) %s file in an existing folder.",
                    parentDir.getName(), outputFile.getAbsolutePath(),
                    type, fileType);
                return ValidationResult.invalid(msg);
            }
        }
        return ValidationResult.valid();
    }

    private static ValidationResult checkDir(File outputDir, TweenOutputType type) {
        if (!outputDir.exists()) {
            return ValidationResult.invalid(outputDir.getAbsolutePath() + " doesn't exist.");
        }

        if (!outputDir.isDirectory()) {
            String msg = String.format("\"<b>%s</b>\" isn't a folder." +
                    "<br>For the \"%s\" output type, select an existing folder.",
                outputDir.getAbsolutePath(), type);
            return ValidationResult.invalid(msg);
        }

        if (!Files.isWritable(outputDir.toPath())) {
            return ValidationResult.invalid(
                String.format("Folder '%s' is not writable.", outputDir));
        }

        return ValidationResult.valid();
    }

    @Override
    public String toString() {
        return displayName;
    }
}

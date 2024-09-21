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

import static java.lang.String.format;

/**
 * The output type of the tweening animation.
 */
public enum TweenOutputType {
    PNG_FILE_SEQUENCE("PNG File Sequence", true) {
        @Override
        AnimationWriter createWriter(File file, int delayMillis) {
            return new PNGFileSequenceWriter(file);
        }

        @Override
        public ValidationResult validate(File output) {
            return checkDir(output, this);
        }

        @Override
        public FileNameExtensionFilter getFileFilter() {
            return null;
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

    private final String guiName;
    private final boolean needsDirectory;

    TweenOutputType(String guiName, boolean needsDirectory) {
        this.guiName = guiName;
        this.needsDirectory = needsDirectory;
    }

    public boolean needsDirectory() {
        return needsDirectory;
    }

    abstract AnimationWriter createWriter(File file, int delayMillis);

    public abstract ValidationResult validate(File output);

    private static ValidationResult checkFile(File output,
                                              TweenOutputType type,
                                              String fileType) {
        if (output.exists()) {
            if (output.isDirectory()) {
                String msg = format("%s is a folder." +
                        "<br>For the \"%s\" output type, " +
                        "select a (new or existing) %s file in an existing folder.",
                    output.getAbsolutePath(), type, fileType);
                return ValidationResult.invalid(msg);
            }
        } else { // if it doesn't exist, we still expect the parent directory to exist
            File parentDir = output.getParentFile();
            if (parentDir == null) {
                return ValidationResult.invalid("Folder not found");
            }
            if (!parentDir.exists()) {
                String msg = format("The folder %s of the %s file does not exist." +
                        "<br>For the \"%s\" output type, " +
                        "select a (new or existing) %s file in an existing folder.",
                    parentDir.getName(), output.getAbsolutePath(),
                    type, fileType);
                return ValidationResult.invalid(msg);
            }
        }
        return ValidationResult.valid();
    }

    private static ValidationResult checkDir(File output, TweenOutputType type) {
        // we expect it to be an existing directory
        if (!output.isDirectory()) {
            String msg = format("\"<b>%s</b>\" isn't a folder." +
                    "<br>For the \"%s\" output type, select an existing folder.",
                output.getAbsolutePath(), type);
            return ValidationResult.invalid(msg);
        }
        if (!output.exists()) {
            return ValidationResult.invalid(output.getAbsolutePath() + " doesn't exist.");
        }
        return ValidationResult.valid();
    }

    public abstract FileNameExtensionFilter getFileFilter();

    @Override
    public String toString() {
        return guiName;
    }
}

/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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

import pixelitor.io.FileChoosers;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

/**
 * The output type of the tweening animation
 */
public enum TweenOutputType {
    PNG_FILE_SEQUENCE("PNG File Sequence") {
        @Override
        AnimationWriter createAnimationWriter(File file, int delayMillis) {
            return new PNGFileSequenceWriter(file);
        }

        @Override
        public String checkFile(File output) {
            return expectDir(output, this);
        }

        @Override
        public boolean needsDirectory() {
            return true;
        }

        @Override
        public FileNameExtensionFilter getFileFilter() {
            return null;
        }
    }, ANIM_GIF("Animated GIF File") {
        @Override
        AnimationWriter createAnimationWriter(File file, int delayMillis) {
            return new AnimGIFWriter(file, delayMillis);
        }

        @Override
        public String checkFile(File output) {
            return expectFileInExistingDir(output, this, "GIF");
        }

        @Override
        public boolean needsDirectory() {
            return false;
        }

        @Override
        public FileNameExtensionFilter getFileFilter() {
            return FileChoosers.gifFilter;
        }
    };

    abstract AnimationWriter createAnimationWriter(File file, int delayMillis);

    /**
     * Returns the error message or null if the argument is OK as output
     */
    public abstract String checkFile(File output);

    public abstract boolean needsDirectory();

    private static String expectFileInExistingDir(File output, TweenOutputType type, String fileType) {
        if (output.exists()) {
            if (output.isDirectory()) {
                return String.format("<html>%s is a folder." +
                                "<br>For the \"%s\" output type, select a (new or existing) %s file in an existing folder.",
                        output.getAbsolutePath(), type.toString(), fileType);
            }
        } else { // if it does not exist, we still expect the parent directory to exist
            File parentDir = output.getParentFile();
            if (!parentDir.exists()) {
                return String.format("<html>The folder %s of the %s file does not exist." +
                                "<br>For the \"%s\" output type, select a (new or existing) %s file in an existing folder.",
                        parentDir.getName(), output.getAbsolutePath(), type.toString(), fileType);
            }
        }
        return null;
    }

    private static String expectDir(File output, TweenOutputType type) {
        // we expect it to be an existing directory
        if (!output.isDirectory()) {
            return String.format("<html>%s is not a folder." +
                            "<br>For the \"%s\" output type, select an existing folder.",
                    output.getAbsolutePath(), type.toString());
        }
        if (!output.exists()) {
            return output.getAbsolutePath() + " does not exist.";
        }
        return null;
    }

    private final String guiName;

    TweenOutputType(String guiName) {
        this.guiName = guiName;
    }

    public abstract FileNameExtensionFilter getFileFilter();

    @Override
    public String toString() {
        return guiName;
    }
}

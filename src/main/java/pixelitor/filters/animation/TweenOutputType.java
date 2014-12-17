/*
 * Copyright 2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters.animation;

import java.io.File;

public enum TweenOutputType {
    ANIM_GIF {
        @Override
        AnimationWriter createAnimationWriter(File file, int delayMillis) {
            return new AnimGIFWriter(file, delayMillis);
        }

        @Override
        public String toString() {
            return "Animated GIF";
        }

        @Override
        public String checkFile(File output) {
            return expectFile(output, this, "GIF");
        }

        @Override
        public boolean needsDirectory() {
            return false;
        }
    }, PNG_FILE_SEQUENCE {
        @Override
        AnimationWriter createAnimationWriter(File file, int delayMillis) {
            return new PNGFileSequenceWriter(file);
        }

        @Override
        public String toString() {
            return "PNG File Sequence";
        }

        @Override
        public String checkFile(File output) {
            return expectDir(output, this);
        }

        @Override
        public boolean needsDirectory() {
            return true;
        }
    };

    abstract AnimationWriter createAnimationWriter(File file, int delayMillis);

    /**
     * Returns the error message or null if the argument is OK as output
     */
    public abstract String checkFile(File output);

    public abstract boolean needsDirectory();

    private static String expectFile(File output, TweenOutputType type, String fileType) {
        // we expect it to be a file
        if (output.exists()) {
            if (output.isDirectory()) {
                return String.format("<html>%s is a folder." +
                                "<br>For the \"%s\" output type, select a (new or existing) %s file in an existing folder.",
                        output.getAbsolutePath(), type.toString(), fileType);
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
}

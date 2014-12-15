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
        AnimationWriter getAnimationWriter(File file, int delayMillis) {
            return new AnimGIFWriter(file, delayMillis);
        }

        @Override
        public String toString() {
            return "Animated GIF";
        }

        @Override
        public void checkFile(File output) {
            // we expect it to be a file
            if(output.exists()) {
                if (output.isDirectory()) {
                    throw new IllegalStateException(output.getAbsolutePath() + " is a directory");
                }
            }
        }

        @Override
        public boolean needsDirectory() {
            return false;
        }
    }, PNG_FILE_SEQUENCE {
        @Override
        AnimationWriter getAnimationWriter(File file, int delayMillis) {
            return new PNGFileSequenceWriter(file);
        }

        @Override
        public String toString() {
            return "PNG File Sequence";
        }

        @Override
        public void checkFile(File output) {
            // we expect it to be an existing directory
            if(!output.exists()) {
                throw new IllegalStateException(output.getAbsolutePath() + " does not exist.");
            }
            if(!output.isDirectory()) {
                throw new IllegalStateException(output.getAbsolutePath() + " is not a directory.");
            }
        }

        @Override
        public boolean needsDirectory() {
            return true;
        }
    };

    abstract AnimationWriter getAnimationWriter(File file, int delayMillis);

    public abstract void checkFile(File output);

    public abstract boolean needsDirectory();
}

/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

public class DecodingException extends RuntimeException {
    private final File file;
    private final boolean magick;

    private DecodingException(File file, boolean magick, Exception cause) {
        super(createUserMsg(file, magick, cause), cause);
        this.file = file;
        this.magick = magick;
    }

    public static DecodingException normal(File file, Exception cause) {
        return new DecodingException(file, false, cause);
    }

    public static DecodingException magick(File file, Exception cause) {
        return new DecodingException(file, true, cause);
    }

    // create the message shown to the user
    private static String createUserMsg(File file, boolean magick, Exception cause) {
        return String.format("<html>Could not %s <b>%s</b> as an image file.%s",
            magick ? "import" : "read",
            file.getName(),
            cause == null ? "" : "<br> (" + cause.getMessage() + ")");
    }

    public File getFile() {
        return file;
    }

    public boolean wasMagick() {
        return magick;
    }
}
